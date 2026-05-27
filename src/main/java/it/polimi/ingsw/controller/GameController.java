package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.leaderboard.LeaderboardService;
import it.polimi.ingsw.controller.leaderboard.NoOpLeaderboardService;
import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.controller.state.ActionResolutionState;
import it.polimi.ingsw.controller.state.EndGameState;
import it.polimi.ingsw.controller.state.EndRoundState;
import it.polimi.ingsw.controller.state.LobbyState;
import it.polimi.ingsw.controller.state.SuspendedState;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Totem;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

import java.io.IOException;
import java.io.ObjectInputStream;
/**
 * Main controller context of the application.
 * It stores the current controller state and the data needed
 * before and during the game.
 *
 * <h2>Thread-safety</h2>
 * This class is <strong>not</strong> thread-safe and must not be invoked
 * concurrently from multiple threads. All external access (network handlers,
 * heartbeat-triggered disconnects, suspension-timeout ticks, RMI calls) is
 * serialized through the intrinsic lock of {@link it.polimi.ingsw.network.server.LobbyManager},
 * which owns this controller. Listener notifications fired during a state
 * transition run on the same thread that triggered the transition, so they
 * inherit the same serialization without any reentrancy hazard.
 *
 * <p>Callers that bypass {@code LobbyManager} (for example tests that drive
 * the controller directly) are responsible for providing their own
 * synchronization.
 */
public class GameController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final long DEFAULT_SUSPENSION_TIMEOUT_MILLIS = 60 * 1000;
    private transient LeaderboardService leaderboardService;
    private transient GamePersistenceService persistenceService;
    private transient DisconnectionManager disconnectionManager;
    private transient TurnRecoveryService turnRecoveryService;
    private SuspensionManager suspensionManager;
    private final UUID matchId = UUID.randomUUID();
    private Game game;
    private GameState currentState;

    private final List<Player> lobbyPlayers;
    private int chosenPlayersCount;

    private final Set<String> readyPlayers;
    private final Set<String> disconnectedPlayers;

    // LISTENER
    private transient List<GameListener> listeners = new ArrayList<>();

    public GameController() {
        this.game = null;
        this.currentState = new LobbyState();
        this.lobbyPlayers = new ArrayList<>();
        this.chosenPlayersCount = -1;
        this.readyPlayers = new HashSet<>();
        this.disconnectedPlayers = new HashSet<>();
        this.suspensionManager = new SuspensionManager(DEFAULT_SUSPENSION_TIMEOUT_MILLIS);
        this.persistenceService = new GamePersistenceService();
        this.disconnectionManager = new DisconnectionManager();
        this.turnRecoveryService = new TurnRecoveryService();

        currentState.onEnter(this);
    }

    /**
     * Receives an action from a player and delegates it to the current state.
     *
     * @param playerNickname the nickname of the player who sent the action
     * @param action         the received action
     */
    public void onMessageReceived(String playerNickname, GameAction action) {
        String normalizedNickname = normalizePlayerNickname(playerNickname);
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null");
        }

        if (Boolean.getBoolean("game.debug") && game != null) {
            if (action instanceof it.polimi.ingsw.controller.action.DebugSkipToEndAction) {
                changeState(new EndGameState());
                return;
            }
            if (action instanceof it.polimi.ingsw.controller.action.DebugSkipRoundAction) {
                debugSkipRound(normalizedNickname);
                return;
            }
        }

        currentState.handleAction(this, normalizedNickname, action);
        getPersistenceService().saveAfterAction(this, currentState);
    }

    /**
     * Records a player disconnection, notifies listeners, recomputes suspension
     * status and enters {@link SuspendedState} if the connected count drops
     * below the FA4 threshold. When no game player remains connected the match
     * is terminated to avoid a zombie game.
     */
    public void onPlayerDisconnection(String playerNickname) {
        String normalizedNickname = normalizePlayerNickname(playerNickname);
        if (isPlayerDisconnected(normalizedNickname)) {
            return;
        }
        boolean knownPlayer = containsLobbyPlayer(normalizedNickname) || containsGamePlayer(normalizedNickname);
        boolean wasSuspended = isSuspended();

        currentState.onPlayerDisconnected(this, normalizedNickname);
        if (knownPlayer) {
            notifyListeners(l -> l.onPlayerDisconnected(normalizedNickname));
        }
        // Recompute after marking the player disconnected so isSuspended() reflects the new connected count.
        updateSuspensionStatus();
        if (!wasSuspended && isSuspended()) {
            enterSuspendedState();
            notifyListeners(GameListener::onGameSuspended);
        }
        // If everyone has disconnected mid-game there is no candidate for the FA4 forced-winner
        // timeout and no one to keep the match alive: terminate cleanly to avoid a zombie game.
        if (game != null && !game.isGameOver() && getConnectedGamePlayersCount() == 0) {
            changeState(new it.polimi.ingsw.controller.state.EndGameState());
        }
    }

    /**
     * Records a player reconnection, forwards it to the current state for
     * recovery, notifies listeners and resumes from {@link SuspendedState}
     * when the connected count climbs back above the suspension threshold.
     *
     * @throws IllegalArgumentException if the player is unknown to this match
     * @throws IllegalStateException if the player was already connected
     */
    public void onPlayerReconnection(String playerNickname) {
        String normalizedNickname = normalizePlayerNickname(playerNickname);
        if (!containsGamePlayer(normalizedNickname)) {
            throw new IllegalArgumentException("Unknown player");
        }
        if (!isPlayerDisconnected(normalizedNickname)) {
            throw new IllegalStateException("Player is already connected");
        }
        boolean wasSuspended = isSuspended();

        markPlayerConnected(normalizedNickname);
        currentState.onPlayerReconnected(this, normalizedNickname);
        notifyListeners(l -> l.onPlayerReconnected(normalizedNickname));
        updateSuspensionStatus();
        // If reconnection brings the connected count back above threshold, resume.
        if (wasSuspended && !isSuspended()) {
            resumeSuspendedState();
            notifyListeners(GameListener::onGameResumed);
        }
    }

    /**
     * Changes the current controller state.
     *
     * @param newState the new state to enter
     */
    public void changeState(GameState newState) {
        if (newState == null) {
            throw new IllegalArgumentException("New state cannot be null");
        }

        GameState previousState = currentState;
        previousState.onExit(this);
        currentState = newState;

        // Notify listeners of the new state BEFORE running its onEnter, so clients
        // render every intermediate transition (e.g. EventResolution -> EndRound ->
        // TotemPlacement) instead of only the final one.
        notifyListeners(l -> l.onStateChanged(newState));

        try {
            currentState.onEnter(this);
        } catch (RuntimeException | Error exception) {
            // onEnter failed; roll back to the previous state to keep the game consistent.
            currentState = previousState;
            try {
                previousState.onEnter(this);
                notifyListeners(l -> l.onStateChanged(previousState));
            } catch (RuntimeException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        }
    }

    public Game getGame() {
        return game;
    }

    /**
     * Installs the game instance for this match, resets suspension tracking
     * and wires the model's listener notifier so that domain events surface
     * through the controller's listener list.
     */
    public void setGame(Game game) {
        this.game = game;
        getSuspensionManager().resetForGame(game);
        if (game != null) {
            game.setNotifier(this::notifyListeners);
        }
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public String getCurrentActionPlayerNickname() {
        return getTurnRecoveryService().currentActionPlayer(currentState);
    }

    public String getCurrentTurnPlayerNickname() {
        return getTurnRecoveryService().currentTurnPlayer(this, currentState);
    }

    public List<Player> getLobbyPlayers() {
        return Collections.unmodifiableList(lobbyPlayers);
    }

    /** Adds a player to the pre-game lobby and clears any previous disconnect record for them. */
    public void addLobbyPlayer(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        lobbyPlayers.add(player);
        getDisconnectionManager().forget(player.getNickname(), disconnectedPlayers);
    }

    public boolean isNicknameAvailable(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return false;
        }

        return lobbyPlayers.stream()
                .noneMatch(player -> player.getNickname().equals(nickname));
    }

    public boolean isTotemAlreadyTaken(Totem totem) {
        if (totem == null || totem.getColor() == null) {
            return true;
        }

        return lobbyPlayers.stream()
                .map(Player::getTotem)
                .anyMatch(existingTotem ->
                        existingTotem != null
                                && existingTotem.getColor() == totem.getColor());
    }

    public int getChosenPlayersCount() {
        return chosenPlayersCount;
    }

    /**
     * Sets the target player count for this lobby. Must be in [2, 5] and
     * cannot be lower than the players already joined.
     *
     * @throws IllegalArgumentException if out of range or below current lobby size
     */
    public void setChosenPlayersCount(int chosenPlayersCount) {
        if (chosenPlayersCount < 2 || chosenPlayersCount > 5) {
            throw new IllegalArgumentException("Players count must be between 2 and 5");
        }
        if (chosenPlayersCount < lobbyPlayers.size()) {
            throw new IllegalArgumentException("Players count cannot be lower than the current lobby size");
        }
        this.chosenPlayersCount = chosenPlayersCount;
    }

    public boolean hasChosenPlayersCount() {
        return chosenPlayersCount != -1;
    }

    public boolean isLobbyFull() {
        return hasChosenPlayersCount() && lobbyPlayers.size() == chosenPlayersCount;
    }

    public boolean isFirstPlayer(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return false;
        }

        return !lobbyPlayers.isEmpty() &&
                lobbyPlayers.get(0).getNickname().equals(nickname);
    }

    public void markPlayerReady(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("Nickname cannot be null or blank");
        }

        if (!containsLobbyPlayer(nickname)) {
            throw new IllegalArgumentException("Player is not in the lobby");
        }

        readyPlayers.add(nickname);
    }

    public void markPlayerNotReady(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("Nickname cannot be null or blank");
        }

        readyPlayers.remove(nickname);
    }

    public boolean isPlayerReady(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return false;
        }

        return readyPlayers.contains(nickname);
    }

    public void resetReadyPlayers() {
        readyPlayers.clear();
    }

    public boolean areAllPlayersReady() {
        return isLobbyFull() && readyPlayers.size() == lobbyPlayers.size();
    }

    public Set<String> getReadyPlayers() {
        return Collections.unmodifiableSet(readyPlayers);
    }

    public boolean containsLobbyPlayer(String nickname) {
        return lobbyPlayers.stream()
                .anyMatch(player -> player.getNickname().equals(nickname));
    }

    public boolean containsGamePlayer(String nickname) {
        if (game == null || nickname == null || nickname.isBlank()) {
            return false;
        }

        return game.getPlayers().stream()
                .anyMatch(player -> player.getNickname().equals(nickname));
    }

    /**
     * Removes a player from the lobby, clears their ready and disconnect
     * records and, when the lobby becomes empty, resets the chosen player
     * count so the next joiner can pick again.
     *
     * @throws IllegalArgumentException if the player is not in the lobby
     */
    public void removePlayerFromLobby(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("Nickname cannot be null or blank");
        }

        boolean removed = lobbyPlayers.removeIf(player -> player.getNickname().equals(nickname));
        if (!removed) {
            throw new IllegalArgumentException("Player is not in the lobby");
        }

        readyPlayers.remove(nickname);
        getDisconnectionManager().forget(nickname, disconnectedPlayers);

        if (lobbyPlayers.isEmpty()) {
            chosenPlayersCount = -1;
            readyPlayers.clear();
        }
    }

    public void markPlayerDisconnected(String nickname) {
        getDisconnectionManager().markDisconnected(
                nickname,
                disconnectedPlayers,
                containsLobbyPlayer(nickname) || containsGamePlayer(nickname)
        );
    }

    public void markPlayerConnected(String nickname) {
        getDisconnectionManager().markConnected(nickname, disconnectedPlayers);
    }

    public boolean isPlayerDisconnected(String nickname) {
        return getDisconnectionManager().isDisconnected(nickname, disconnectedPlayers);
    }

    /**
     * Marks every known player (both in lobby and in game) as disconnected.
     * Intended to be called after restoring a controller from persistence:
     * the on-disk state reflects the situation before the crash, but all
     * actual network connections have been lost and need to be re-established
     * via the standard reconnect flow.
     *
     * <p>After this call, suspension status is recomputed: if a game is in
     * progress, the controller will enter SuspendedState until at least one
     * player reconnects.
     */
    public void markAllPlayersDisconnected() {
        getDisconnectionManager().markAllDisconnected(lobbyPlayers, game, disconnectedPlayers);
        updateSuspensionStatus();
        if (isSuspended() && !(currentState instanceof SuspendedState)) {
            enterSuspendedState();
            notifyListeners(GameListener::onGameSuspended);
        }
    }

    public int getConnectedGamePlayersCount() {
        return getDisconnectionManager().countConnectedGamePlayers(game, disconnectedPlayers);
    }

    public boolean isSuspended() {
        return getSuspensionManager().isSuspended();
    }

    public GameState getSuspendedStateReturn() {
        return getSuspensionManager().getSuspendedStateReturn();
    }

    /** True when the match is still live and at least two players are connected (FA4 threshold). */
    public boolean canContinueAfterDisconnection() {
        return game != null && !game.isGameOver() && getConnectedGamePlayersCount() >= 2;
    }

    public void updateSuspensionStatus() {
        getSuspensionManager().updateStatus(game, getConnectedGamePlayersCount(), System.currentTimeMillis());
    }

    public long getSuspensionTimeoutMillis() {
        return getSuspensionManager().getSuspensionTimeoutMillis();
    }

    public void setSuspensionTimeoutMillis(long suspensionTimeoutMillis) {
        getSuspensionManager().setSuspensionTimeoutMillis(suspensionTimeoutMillis);
    }

    public long getSuspensionStartedAtMillis() {
        return getSuspensionManager().getSuspensionStartedAtMillis();
    }

    public long getSuspensionRemainingMillis() {
        return getSuspensionRemainingMillis(System.currentTimeMillis());
    }

    public long getSuspensionRemainingMillis(long nowMillis) {
        return getSuspensionManager().getSuspensionRemainingMillis(nowMillis);
    }

    public boolean checkSuspensionTimeout() {
        return checkSuspensionTimeout(System.currentTimeMillis());
    }

    /**
     * Checks whether the suspension timeout elapsed and, if so, ends the
     * match in favour of the only connected player by transitioning to
     * {@link EndGameState}. Returns true when the timeout triggered.
     */
    public boolean checkSuspensionTimeout(long nowMillis) {
        boolean timedOut = getSuspensionManager().checkTimeout(
                game,
                getConnectedGamePlayersCount(),
                getOnlyConnectedGamePlayer(),
                nowMillis
        );
        if (!timedOut) {
            return false;
        }

        changeState(new EndGameState());
        return true;
    }

    public Player consumeForcedWinner() {
        return getSuspensionManager().consumeForcedWinner(game);
    }

    private Player getOnlyConnectedGamePlayer() {
        return getDisconnectionManager().onlyConnectedGamePlayer(game, disconnectedPlayers);
    }

    private void enterSuspendedState() {
        SuspendedState suspendedState = getSuspensionManager().enter(game, currentState);
        if (suspendedState == null) {
            return;
        }

        // Bypass changeState() to preserve the interrupted state's progress — onExit() must not be called.
        currentState = suspendedState;
        currentState.onEnter(this);
        notifyListeners(l -> l.onStateChanged(currentState));
    }

    private void resumeSuspendedState() {
        GameState stateToResume = getSuspensionManager().resume(currentState);
        if (stateToResume == null) {
            return;
        }

        currentState.onExit(this);
        // Restore the exact pre-suspension state without re-entering it from scratch.
        currentState = stateToResume;
        GameState resumedState = currentState;
        currentState.onResume(this);
        if (currentState == resumedState) {
            notifyListeners(l -> l.onStateChanged(currentState));
        }
    }

    // LISTENER
    /** Registers a listener for controller and game events. Listener cannot be null. */
    public void addListener(GameListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        listeners.add(listener);
    }

    public void removeListener(GameListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        listeners.remove(listener);
    }

    /**
     * Dispatches an event to every registered listener. Exceptions thrown by
     * individual listeners are logged and swallowed so that one misbehaving
     * client cannot break the broadcast for the others.
     */
    public void notifyListeners(Consumer<GameListener> event) {
        for (GameListener listener : listeners) {
            try {
                event.accept(listener);
            } catch (Exception e) {
                System.err.println("Listener error: " + e.getMessage());
            }
        }
    }

    private String normalizePlayerNickname(String playerNickname) {
        if (playerNickname == null) {
            throw new IllegalArgumentException("Player nickname cannot be null or blank");
        }

        String normalizedNickname = playerNickname.trim();
        if (normalizedNickname.isBlank()) {
            throw new IllegalArgumentException("Player nickname cannot be null or blank");
        }

        for (int i = 0; i < normalizedNickname.length(); i++) {
            if (Character.isISOControl(normalizedNickname.charAt(i))) {
                throw new IllegalArgumentException("Player nickname contains invalid characters");
            }
        }

        return normalizedNickname;
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        // Transient fields are not serialized; rebuild them from scratch on deserialization.
        in.defaultReadObject();
        listeners = new ArrayList<>();
        persistenceService = new GamePersistenceService();
        disconnectionManager = new DisconnectionManager();
        turnRecoveryService = new TurnRecoveryService();
        getSuspensionManager().ensureValidTimeout(DEFAULT_SUSPENSION_TIMEOUT_MILLIS);
    }

    // ── DEBUG ────────────────────────────────────────────────────────────────

    private void debugSkipRound(String senderNickname) {
        GameState state = getTurnRecoveryService().unwrapSuspendedState(currentState);

        if (state instanceof TotemPlacementState) {
            java.util.List<it.polimi.ingsw.model.board.OfferTile> tiles =
                    game.getBoard().getOfferTiles();
            int maxAttempts = game.getPlayers().size() * tiles.size() + 1;
            int attempts = 0;
            while (currentState instanceof TotemPlacementState && attempts++ < maxAttempts) {
                TotemPlacementState cur = (TotemPlacementState) currentState;
                String who = cur.getCurrentPlayerNickname(this);
                if (who == null) break;
                for (it.polimi.ingsw.model.board.OfferTile tile : tiles) {
                    try {
                        currentState.handleAction(this, who,
                                new it.polimi.ingsw.controller.action.PlaceTotemAction(tile.getLetter()));
                        break;
                    } catch (Exception ignored) { }
                }
            }
        } else if (state instanceof ActionResolutionState actionState) {
            actionState.debugAutoComplete(this);
        }
    }
    public void setLeaderboardService(LeaderboardService service) {
        this.leaderboardService = (service != null) ? service : new NoOpLeaderboardService();
    }

    public LeaderboardService getLeaderboardService() {
        if (leaderboardService == null) {
            leaderboardService = new NoOpLeaderboardService();
        }
        return leaderboardService;
    }

    public UUID getMatchId() {
        return matchId;
    }

    private GamePersistenceService getPersistenceService() {
        if (persistenceService == null) {
            persistenceService = new GamePersistenceService();
        }
        return persistenceService;
    }

    private SuspensionManager getSuspensionManager() {
        if (suspensionManager == null) {
            suspensionManager = new SuspensionManager(DEFAULT_SUSPENSION_TIMEOUT_MILLIS);
        }
        return suspensionManager;
    }

    private DisconnectionManager getDisconnectionManager() {
        if (disconnectionManager == null) {
            disconnectionManager = new DisconnectionManager();
        }
        return disconnectionManager;
    }

    private TurnRecoveryService getTurnRecoveryService() {
        if (turnRecoveryService == null) {
            turnRecoveryService = new TurnRecoveryService();
        }
        return turnRecoveryService;
    }
}
