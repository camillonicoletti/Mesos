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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import it.polimi.ingsw.controller.leaderboard.NoOpLeaderboardService;
import it.polimi.ingsw.network.protocol.clientbound.LeaderboardMessage;

public class LobbyManager {

    private static final int SUSPENSION_TIMEOUT_CHECK_INTERVAL_SECONDS = 1;

    private final GameController gameController;
    private final Map<String, ServerSideConnection> connections = new HashMap<>();
    private final Map<String, VirtualView> views = new HashMap<>();
    private final List<ServerSideConnection> pendingConnections = new ArrayList<>();
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

    /** DEBUG ONLY — forces immediate transition to EndGameState. Remove before release. */
    public synchronized void handleForceEndGame() {
        gameController.changeState(new it.polimi.ingsw.controller.state.EndGameState());
    }

    public synchronized void handleGameAction(String nickname, GameAction action) {
        try {
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
        registerConnection(nickname, connection);
        connection.send(new LoginAckMessage(LoginAckMessage.Outcome.LOGIN_OK, null));
        broadcastLobbyUpdate();
        connection.send(new StateChangedMessage(gameController.getCurrentState().getClass().getSimpleName()));
    }

    public synchronized void handleReconnect(ServerSideConnection connection, ReconnectMessage message) {
        String nickname = message.getNickname();

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
        if (gameController.isSuspended()) {
            connection.send(new GameSuspendedMessage(gameController.getSuspensionRemainingMillis()));
        }

        // 2. Now register the connection + VirtualView so future events flow.
        registerConnection(nickname, connection);

        // 3. Trigger the reconnection in the controller. Any events fired here
        //    (onPlayerReconnected to others, onGameResumed, onStateChanged)
        //    arrive at the client AFTER the snapshot above.
        try {
            gameController.onPlayerReconnection(nickname);
        } catch (Exception exception) {
            unregisterConnection(nickname);
            connection.send(new ErrorMessage(exception.getMessage()));
        }
    }

    public synchronized void handleSetPlayersCount(String nickname, SetPlayersCountMessage message) {
        try {
            boolean gameWasNotStarted = gameController.getGame() == null;
            gameController.onMessageReceived(nickname, new SetPlayerCountAction(message.getCount()));
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
            gameController.onPlayerDisconnection(nickname);
        } catch (Exception ignored) {
            // The controller is the source of truth; if the player was already processed,
            // we do not want a network-side failure to cascade.
        }

        if (gameController.getGame() == null) {
            broadcastLobbyUpdate();
        }
    }
    public synchronized void handleGetLeaderboard(ServerSideConnection connection,
                                                  GetLeaderboardMessage message) {
        int requestedLimit = Math.min(message.limit(), 10);
        List<LeaderboardEntry> entries =
                leaderboardService.getTop(message.playersCount(), requestedLimit);
        connection.send(new LeaderboardMessage(message.playersCount(), entries));
    }
    private void broadcastLobbyUpdate() {
        LobbyUpdateMessage message = buildLobbyUpdateMessage();

        for (ServerSideConnection connection : connections.values()) {
            connection.send(message);
        }
    }


    private void broadcastGameStarted() {
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

        VirtualView view = new VirtualView(connection, nickname, gameController);
        views.put(nickname, view);
        gameController.addListener(view);
        connection.setNickname(nickname);
    }

    private void unregisterConnection(String nickname) {
        ServerSideConnection removedConnection = connections.remove(nickname);
        VirtualView removedView = views.remove(nickname);
        if (removedView != null) {
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