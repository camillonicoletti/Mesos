package it.polimi.ingsw.network.server;

import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.leaderboard.LeaderboardEntry;
import it.polimi.ingsw.controller.leaderboard.LeaderboardService;
import it.polimi.ingsw.controller.listener.PersistenceListener;
import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.network.protocol.clientbound.*;
import it.polimi.ingsw.network.protocol.serverbound.GetLeaderboardMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReconnectMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.network.transport.ServerSideConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import it.polimi.ingsw.controller.leaderboard.NoOpLeaderboardService;
import it.polimi.ingsw.network.protocol.clientbound.LeaderboardMessage;

/**
 * Network-side coordinator that owns the single {@link GameController} of a
 * server instance and routes inbound client messages to it.
 *
 * <h2>Thread-safety / concurrency model</h2>
 * The server is multi-threaded: every Socket client has its own reader and
 * heartbeat threads, RMI calls run on the RMI runtime's thread pool, and a
 * dedicated scheduler ticks the FA4 suspension timeout every second. All of
 * these paths eventually invoke a {@code handle*} method on this class.
 *
 * <p>Every public {@code handle*} entry point, plus {@link #checkSuspensionTimeout()}
 * and the {@code registerPendingConnection}/{@code unregisterPendingConnection}
 * pair, is declared {@code synchronized}. The intrinsic lock of this instance
 * is therefore the single serialization point for the entire server: at most
 * one thread at a time mutates the {@link GameController}, the connection
 * maps, or fires listener notifications. Because {@code GameController}
 * listeners run inside the handler that triggered them, listener fan-out is
 * also covered by the same lock without reentrancy across different monitors.
 *
 * <p>The internal collections ({@code connections}, {@code views},
 * {@code pendingConnections}) are plain non-concurrent collections on purpose:
 * they are only ever touched under this lock. Per-connection outbound writes
 * ({@code connection.send(...)}) are additionally synchronized inside the
 * connection object, so two threads writing to the same client cannot
 * interleave bytes.
 *
 * <p><strong>Known trade-off:</strong> outbound writes happen while the lock
 * is held. A slow client therefore slows down every other client until its
 * write completes. This is acceptable for the expected match size (2-5
 * players on a LAN) and avoids the complexity of per-connection outbound
 * queues.
 */
public class LobbyManager {

    private static final int SUSPENSION_TIMEOUT_CHECK_INTERVAL_SECONDS = 1;

    private final GameController gameController;
    private final Map<String, ServerSideConnection> connections = new HashMap<>();
    private final Map<String, VirtualView> views = new HashMap<>();
    private final Set<ServerSideConnection> pendingConnections = new LinkedHashSet<>();
    private final ScheduledExecutorService suspensionTimeoutExecutor;
    private final LeaderboardService leaderboardService;
    /**
     * Creates a fresh lobby manager with a brand-new controller.
     * Used on a clean server start with no save file on disk.
     */
    public LobbyManager() {
        this(new GameController(), new NoOpLeaderboardService());
    }

    public LobbyManager(LeaderboardService leaderboardService) {
        this(new GameController(), leaderboardService);
    }

    public LobbyManager(GameController preloaded) {
        this(preloaded, new NoOpLeaderboardService());
    }

    /**
     * Creates a lobby manager around an existing controller, typically one
     * restored from disk after a server crash.
     *
     * <p>If the restored controller has a game in progress, every player is
     * marked as disconnected so the controller can suspend the match until the
     * clients reconnect. A {@link PersistenceListener} is attached to keep the
     * save file aligned with future events.
     *
     * @param preloaded         the controller to use as backing state; must not be null
     * @param leaderboardService the leaderboard backend; must not be null
     *                          (pass {@link NoOpLeaderboardService} to disable)
     */
    public LobbyManager(GameController preloaded, LeaderboardService leaderboardService) {
        if (preloaded == null) {
            throw new IllegalArgumentException("Controller cannot be null");
        }
        if (leaderboardService == null) {
            throw new IllegalArgumentException("LeaderboardService cannot be null");
        }
        this.gameController = preloaded;
        this.leaderboardService = leaderboardService;
        this.gameController.setLeaderboardService(leaderboardService);
        if (this.gameController.getGame() != null) {
            this.gameController.markAllPlayersDisconnected();
        }
        this.gameController.addListener(new PersistenceListener(this.gameController));
        // The scheduler is only responsible for the suspended-game timeout.
        this.suspensionTimeoutExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "suspension-timeout-monitor");
            thread.setDaemon(true);
            return thread;
        });
        this.suspensionTimeoutExecutor.scheduleAtFixedRate(
                this::checkSuspensionTimeout,
                SUSPENSION_TIMEOUT_CHECK_INTERVAL_SECONDS,
                SUSPENSION_TIMEOUT_CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    public LeaderboardService getLeaderboardService() {
        return leaderboardService;
    }

    /** Command available only in debug mode. */
    public synchronized void handleForceEndGame() {
        gameController.changeState(new it.polimi.ingsw.controller.state.EndGameState());
    }

    // Synchronization serializes game actions with connection and disconnection events.
    public synchronized void handleGameAction(String nickname, GameAction action) {
        try {
            // Once authenticated, player actions go directly to the GameController state machine.
            gameController.onMessageReceived(nickname, action);
        } catch (Exception exception) {
            ServerSideConnection connection = connections.get(nickname);
            if (connection != null) {
                connection.send(new ErrorMessage(exception.getMessage()));
            }
        }
    }

    public synchronized void handleLogin(ServerSideConnection connection, LoginMessage message) {
        String nickname = message.getNickname();

        // Login is allowed only before the game starts and only for brand-new nicknames.
        if (nickname == null || nickname.isBlank()) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "Invalid nickname"));
            return;
        }
        if (connections.containsKey(nickname)) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "Nickname already in use"));
            return;
        }
        if (gameController.containsLobbyPlayer(nickname) || gameController.containsGamePlayer(nickname)) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED,
                    "Nickname already registered (use reconnect)"));
            return;
        }
        if (gameController.getGame() != null) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "The game has already started"));
            return;
        }

        Totem totem = new Totem(message.getTotemColor(), null);
        try {
            gameController.onMessageReceived(nickname, new JoinGameAction(totem));
        } catch (Exception exception) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, exception.getMessage()));
            return;
        }

        unregisterPendingConnection(connection);
        // From this point the connection belongs to an authenticated player.
        registerConnection(nickname, connection);
        connection.send(new LoginAckMessage(LoginAckMessage.Outcome.LOGIN_OK, null));
        broadcastLobbyUpdate();
        connection.send(new StateChangedMessage(gameController.getCurrentState().getClass().getSimpleName()));
    }

    public synchronized void handleReconnect(ServerSideConnection connection, ReconnectMessage message) {
        String nickname = message.getNickname();

        // Reconnect is allowed only for known game players currently marked disconnected.
        if (nickname == null || nickname.isBlank()) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "Invalid nickname"));
            return;
        }
        if (!gameController.containsGamePlayer(nickname)) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "Unknown player"));
            return;
        }
        if (connections.containsKey(nickname)) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "Player already connected"));
            return;
        }
        if (!gameController.isPlayerDisconnected(nickname)) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED,
                    "Player does not appear to be disconnected"));
            return;
        }
        // Reject reconnects once the match has already ended (e.g. forced-winner timeout in FA4):
        // resuming would resurrect a concluded game and bypass the recorded result.
        if (gameController.getGame() != null && gameController.getGame().isGameOver()) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "Match has already ended"));
            return;
        }

        // 1. Send the ack and the baseline snapshot BEFORE wiring any listener,
        //    so the client always renders state in this order:
        //       ack -> snapshot -> live events.
        connection.send(new LoginAckMessage(LoginAckMessage.Outcome.RECONNECT_OK, null));
        connection.send(new GameSyncMessage(SnapshotBuilder.build(gameController)));
        connection.send(new StateChangedMessage(gameController.getCurrentState().getClass().getSimpleName()));
        String currentTurnPlayer = gameController.getCurrentTurnPlayerNickname();
        if (currentTurnPlayer != null) {
            connection.send(new CurrentPlayerChangedMessage(currentTurnPlayer));
        }

        // 2. Now register the connection + VirtualView so future events flow.
        registerConnection(nickname, connection);

        // 3. Trigger the reconnection in the controller. Any events fired here
        //    (onPlayerReconnected to others, onGameResumed, onStateChanged)
        //    arrive at the client AFTER the snapshot above.
        try {
            gameController.onPlayerReconnection(nickname);
            // 4. Send the suspension state ONLY now, after onPlayerReconnection has
            //    updated the connected count. On a restore the count goes 0 → 1 here,
            //    so updateSuspensionStatus just freshly anchored the forced-win
            //    countdown: sending earlier (while still at 0 connected) would ship a
            //    stale remaining of -1 and leave the client stuck on "waiting for the
            //    server". If the count climbed back to ≥2 the match resumed and
            //    onPlayerReconnection already emitted onGameResumed through the
            //    VirtualView registered above, so there is nothing to send here.
            if (gameController.isSuspended()) {
                connection.send(new GameSuspendedMessage(gameController.getSuspensionRemainingMillis()));
            }
        } catch (Exception exception) {
            unregisterConnection(nickname);
            connection.send(new ErrorMessage(exception.getMessage()));
        }
    }

    public synchronized void handleSetPlayersCount(String nickname, SetPlayersCountMessage message) {
        try {
            boolean gameWasNotStarted = gameController.getGame() == null;
            gameController.onMessageReceived(nickname, new SetPlayerCountAction(message.getCount()));
            // Setting the count can start the match when the lobby becomes complete.
            if (gameWasNotStarted && gameController.getGame() != null) {
                broadcastGameStarted();
            } else {
                broadcastLobbyUpdate();
            }
        } catch (Exception exception) {
            ServerSideConnection connection = connections.get(nickname);
            if (connection != null) {
                connection.send(new ErrorMessage(exception.getMessage()));
            }
        }
    }

    public synchronized void handleReady(String nickname, boolean ready) {
        try {
            boolean gameWasNotStarted = gameController.getGame() == null;
            gameController.onMessageReceived(nickname, new ReadyAction(ready));
            // Ready can also trigger the transition from lobby to game.
            if (gameWasNotStarted && gameController.getGame() != null) {
                broadcastGameStarted();
            } else {
                broadcastLobbyUpdate();
            }
        } catch (Exception exception) {
            ServerSideConnection connection = connections.get(nickname);
            if (connection != null) {
                connection.send(new ErrorMessage(exception.getMessage()));
            }
        }
    }

    public synchronized void handleDisconnect(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return;
        }

        unregisterConnection(nickname);

        if (!gameController.containsLobbyPlayer(nickname) && !gameController.containsGamePlayer(nickname)) {
            return;
        }

        try {
            // The controller decides whether this removes a lobby player or suspends the match.
            gameController.onPlayerDisconnection(nickname);
        } catch (Exception ignored) {
            // The controller is the source of truth; if the player was already processed,
            // we do not want a network-side failure to cascade.
        }

        if (gameController.getGame() == null) {
            broadcastLobbyUpdate();
        }
    }

    public synchronized void handleConnectionClosed(ServerSideConnection connection) {
        if (connection == null) {
            return;
        }

        String nickname = connection.getNickname();
        if (nickname == null) {
            // A client can disconnect before completing login.
            unregisterPendingConnection(connection);
            return;
        }

        handleDisconnect(nickname);
    }

    /**
     * Handles a voluntary quit (player explicitly leaving the match). Different from
     * {@link #handleDisconnect}: a quit is irreversible (no reconnect path) and, if the
     * match is in progress, terminates the game for everyone rather than entering / staying
     * in the FA4 suspended state. Pre-game quits behave like a regular disconnect.
     */
    public synchronized void handleQuit(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return;
        }

        unregisterConnection(nickname);

        if (!gameController.containsLobbyPlayer(nickname) && !gameController.containsGamePlayer(nickname)) {
            return;
        }

        boolean gameInProgress = gameController.getGame() != null && !gameController.getGame().isGameOver();

        if (!gameInProgress) {
            // Pre-game quit or already game-over: same cleanup as a disconnect (remove from lobby).
            try {
                gameController.onPlayerDisconnection(nickname);
            } catch (Exception ignored) {
            }
            if (gameController.getGame() == null) {
                broadcastLobbyUpdate();
            }
            return;
        }

        // Mid-game quit: the match must end for everyone. Mark the quitter disconnected
        // first so any state-specific cleanup (e.g. clearing their turn slot) runs, then
        // transition to EndGameState which freezes scores and notifies all listeners.
        try {
            if (!gameController.isPlayerDisconnected(nickname)) {
                gameController.onPlayerDisconnection(nickname);
            }
        } catch (Exception ignored) {
        }
        try {
            gameController.changeState(new it.polimi.ingsw.controller.state.EndGameState());
        } catch (Exception ignored) {
            // If the controller cannot transition (already game-over), there is nothing more to do.
        }
    }
    public synchronized void handleGetLeaderboard(ServerSideConnection connection,
                                                  GetLeaderboardMessage message) {
        // Limit is capped server-side even if a client asks for more.
        int requestedLimit = Math.min(message.limit(), 10);
        List<LeaderboardEntry> entries =
                leaderboardService.getTop(message.playersCount(), requestedLimit);
        connection.send(new LeaderboardMessage(message.playersCount(), entries));
    }
    private void broadcastLobbyUpdate() {
        // Lobby updates are small and are also useful before login completes.
        LobbyUpdateMessage message = buildLobbyUpdateMessage();

        for (ServerSideConnection connection : connections.values()) {
            connection.send(message);
        }
        // Pending connections (clients still on the login screen) must also see lobby changes
        // so taken totem colors and nicknames disappear from their selection in real time.
        for (ServerSideConnection connection : pendingConnections) {
            connection.send(message);
        }
    }


    private void broadcastGameStarted() {
        // Game start needs a full snapshot, not just a state name.
        GameStartedMessage message = new GameStartedMessage(SnapshotBuilder.build(gameController));
        StateChangedMessage stateMessage = new StateChangedMessage(
                gameController.getCurrentState().getClass().getSimpleName()
        );
        for (ServerSideConnection connection : connections.values()) {
            connection.send(message);
            connection.send(stateMessage);
        }
    }

    private void registerConnection(String nickname, ServerSideConnection connection) {
        unregisterConnection(nickname);
        connections.put(nickname, connection);

        // VirtualView is the per-client listener that receives future game events.
        VirtualView view = new VirtualView(connection, nickname, gameController);
        views.put(nickname, view);
        gameController.addListener(view);
        connection.setNickname(nickname);
    }

    private void unregisterConnection(String nickname) {
        ServerSideConnection removedConnection = connections.remove(nickname);
        VirtualView removedView = views.remove(nickname);
        if (removedView != null) {
            // Removing the listener prevents messages from being sent to a stale connection.
            gameController.removeListener(removedView);
        }
        if (removedConnection != null && removedConnection.isAlive()) {
            removedConnection.close();
        }
    }

    public GameController getGameController() {
        return gameController;
    }

    public void shutdown() {
        suspensionTimeoutExecutor.shutdownNow();
    }

    private synchronized void checkSuspensionTimeout() {
        try {
            gameController.checkSuspensionTimeout();
        } catch (RuntimeException exception) {
            System.err.println("Error while checking the suspension timeout: "
                    + exception.getMessage());
        }
    }

    public synchronized void registerPendingConnection(ServerSideConnection connection) {
        pendingConnections.add(connection);
        // Send the current lobby snapshot so the client knows which totem colors are already taken
        connection.send(buildLobbyUpdateMessage());
    }

    public synchronized void unregisterPendingConnection(ServerSideConnection connection) {
        pendingConnections.remove(connection);
    }

    private LobbyUpdateMessage buildLobbyUpdateMessage() {
        // The lobby DTO intentionally contains only lightweight player status.
        List<LobbyPlayerSnapshot> players = new ArrayList<>();

        for (Player player : gameController.getLobbyPlayers()) {
            String nickname = player.getNickname();
            players.add(new LobbyPlayerSnapshot(
                    nickname,
                    player.getTotem() != null ? player.getTotem().getColor() : null,
                    gameController.isPlayerReady(nickname),
                    !gameController.isPlayerDisconnected(nickname)
            ));
        }

        return new LobbyUpdateMessage(players, gameController.getChosenPlayersCount());
    }

}
