package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.controller.state.LobbyState;
import it.polimi.ingsw.controller.state.SuspendedState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Totem;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import java.io.IOException;
import java.io.ObjectInputStream;
/**
 * Main controller context of the application.
 * It stores the current controller state and the data needed
 * before and during the game.
 */
public class GameController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final long DEFAULT_SUSPENSION_TIMEOUT_MILLIS = 5 * 60 * 1000;

    private Game game;
    private GameState currentState;

    private final List<Player> lobbyPlayers;
    private int chosenPlayersCount;

    private final Set<String> readyPlayers;
    private final Set<String> disconnectedPlayers;
    private boolean suspended;
    private GameState suspendedStateReturn;
    private long suspensionStartedAtMillis;
    private long suspensionTimeoutMillis;
    private String forcedWinnerNickname;

    // LISTENER
    private transient List<GameListener> listeners = new ArrayList<>();

    public GameController() {
        this.game = null;
        this.currentState = new LobbyState();
        this.lobbyPlayers = new ArrayList<>();
        this.chosenPlayersCount = -1;
        this.readyPlayers = new HashSet<>();
        this.disconnectedPlayers = new HashSet<>();
        this.suspended = false;
        this.suspendedStateReturn = null;
        this.suspensionStartedAtMillis = -1L;
        this.suspensionTimeoutMillis = DEFAULT_SUSPENSION_TIMEOUT_MILLIS;
        this.forcedWinnerNickname = null;

        currentState.onEnter(this);
    }

    /**
     * Receives an action from a player and delegates it to the current state.
     *
     * @param playerNickname the nickname of the player who sent the action
     * @param action         the received action
     */
    public void onMessageReceived(String playerNickname, GameAction action) {
        if (playerNickname == null || playerNickname.isBlank()) {
            throw new IllegalArgumentException("Player nickname cannot be null or blank");
        }
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null");
        }

        currentState.handleAction(this, playerNickname, action);
    }

    public void onPlayerDisconnection(String playerNickname) {
        if (playerNickname == null || playerNickname.isBlank()) {
            throw new IllegalArgumentException("Player nickname cannot be null or blank");
        }
        boolean knownPlayer = containsLobbyPlayer(playerNickname) || containsGamePlayer(playerNickname);
        boolean wasSuspended = suspended;

        currentState.onPlayerDisconnected(this, playerNickname);
        if (knownPlayer) {
            notifyListeners(l -> l.onPlayerDisconnected(playerNickname));
        }
        updateSuspensionStatus();
        if (!wasSuspended && suspended) {
            enterSuspendedState();
            notifyListeners(GameListener::onGameSuspended);
        }
    }

    public void onPlayerReconnection(String playerNickname) {
        if (playerNickname == null || playerNickname.isBlank()) {
            throw new IllegalArgumentException("Player nickname cannot be null or blank");
        }
        if (!containsGamePlayer(playerNickname)) {
            throw new IllegalArgumentException("Unknown player");
        }
        boolean wasSuspended = suspended;

        markPlayerConnected(playerNickname);
        currentState.onPlayerReconnected(this, playerNickname);
        notifyListeners(l -> l.onPlayerReconnected(playerNickname));
        updateSuspensionStatus();
        if (wasSuspended && !suspended) {
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
        try {
            currentState.onEnter(this);
            notifyListeners(l -> l.onStateChanged(newState));
        } catch (RuntimeException | Error exception) {
            currentState = previousState;
            try {
                previousState.onEnter(this);
            } catch (RuntimeException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        }
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
        this.suspended = false;
        this.suspendedStateReturn = null;
        this.suspensionStartedAtMillis = -1L;
        this.forcedWinnerNickname = null;
        if (game != null) {
            game.setNotifier(this::notifyListeners);
        }
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public List<Player> getLobbyPlayers() {
        return Collections.unmodifiableList(lobbyPlayers);
    }

    public void addLobbyPlayer(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        lobbyPlayers.add(player);
        disconnectedPlayers.remove(player.getNickname());
    }

    public boolean isNicknameAvailable(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return false;
        }

        return lobbyPlayers.stream()
                .noneMatch(player -> player.getNickname().equals(nickname));
    }

    public boolean isTotemAlreadyTaken(Totem totem) {
        if (totem == null) {
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

    public void setChosenPlayersCount(int chosenPlayersCount) {
        if (chosenPlayersCount < 2 || chosenPlayersCount > 5) {
            throw new IllegalArgumentException("Players count must be between 2 and 5");
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

    public void removePlayerFromLobby(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("Nickname cannot be null or blank");
        }

        boolean removed = lobbyPlayers.removeIf(player -> player.getNickname().equals(nickname));
        if (!removed) {
            throw new IllegalArgumentException("Player is not in the lobby");
        }

        readyPlayers.remove(nickname);
        disconnectedPlayers.remove(nickname);

        if (lobbyPlayers.isEmpty()) {
            chosenPlayersCount = -1;
            readyPlayers.clear();
        }
    }

    public void markPlayerDisconnected(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("Nickname cannot be null or blank");
        }

        if (!containsLobbyPlayer(nickname) && !containsGamePlayer(nickname)) {
            throw new IllegalArgumentException("Unknown player");
        }

        disconnectedPlayers.add(nickname);
    }

    public void markPlayerConnected(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("Nickname cannot be null or blank");
        }

        disconnectedPlayers.remove(nickname);
    }

    public boolean isPlayerDisconnected(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return false;
        }

        return disconnectedPlayers.contains(nickname);
    }

    public int getConnectedGamePlayersCount() {
        if (game == null) {
            return 0;
        }

        int connectedPlayersCount = 0;
        for (Player player : game.getPlayers()) {
            if (!isPlayerDisconnected(player.getNickname())) {
                connectedPlayersCount++;
            }
        }
        return connectedPlayersCount;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public GameState getSuspendedStateReturn() {
        return suspendedStateReturn;
    }

    public boolean canContinueAfterDisconnection() {
        return game != null && !game.isGameOver() && getConnectedGamePlayersCount() >= 2;
    }

    public void updateSuspensionStatus() {
        if (game == null || game.isGameOver()) {
            suspended = false;
            suspensionStartedAtMillis = -1L;
            return;
        }

        boolean shouldSuspend = getConnectedGamePlayersCount() <= 1;
        if (!suspended && shouldSuspend) {
            suspensionStartedAtMillis = System.currentTimeMillis();
        } else if (suspended && !shouldSuspend) {
            suspensionStartedAtMillis = -1L;
        }

        suspended = shouldSuspend;
    }

    public long getSuspensionTimeoutMillis() {
        return suspensionTimeoutMillis;
    }

    public void setSuspensionTimeoutMillis(long suspensionTimeoutMillis) {
        if (suspensionTimeoutMillis <= 0) {
            throw new IllegalArgumentException("Suspension timeout must be positive");
        }

        this.suspensionTimeoutMillis = suspensionTimeoutMillis;
    }

    public long getSuspensionStartedAtMillis() {
        return suspensionStartedAtMillis;
    }

    public boolean checkSuspensionTimeout() {
        return checkSuspensionTimeout(System.currentTimeMillis());
    }

    public boolean checkSuspensionTimeout(long nowMillis) {
        if (!suspended || game == null || game.isGameOver()) {
            return false;
        }

        if (suspensionStartedAtMillis < 0L) {
            return false;
        }

        if (getConnectedGamePlayersCount() != 1) {
            return false;
        }

        if (nowMillis - suspensionStartedAtMillis < suspensionTimeoutMillis) {
            return false;
        }

        Player forcedWinner = getOnlyConnectedGamePlayer();
        if (forcedWinner == null) {
            return false;
        }

        forcedWinnerNickname = forcedWinner.getNickname();
        suspended = false;
        suspendedStateReturn = null;
        suspensionStartedAtMillis = -1L;
        changeState(new it.polimi.ingsw.controller.state.EndGameState());
        return true;
    }

    public Player consumeForcedWinner() {
        if (forcedWinnerNickname == null || game == null) {
            return null;
        }

        Player forcedWinner = game.getPlayers().stream()
                .filter(player -> player.getNickname().equals(forcedWinnerNickname))
                .findFirst()
                .orElse(null);
        forcedWinnerNickname = null;
        return forcedWinner;
    }

    private Player getOnlyConnectedGamePlayer() {
        if (game == null) {
            return null;
        }

        Player connectedPlayer = null;
        for (Player player : game.getPlayers()) {
            if (isPlayerDisconnected(player.getNickname())) {
                continue;
            }

            if (connectedPlayer != null) {
                return null;
            }

            connectedPlayer = player;
        }

        return connectedPlayer;
    }

    private void enterSuspendedState() {
        if (currentState instanceof SuspendedState || game == null || game.isGameOver()) {
            return;
        }

        // Preserve the live state instance so its internal progress can resume unchanged.
        suspendedStateReturn = currentState;
        currentState = new SuspendedState(suspendedStateReturn);
        currentState.onEnter(this);
        notifyListeners(l -> l.onStateChanged(currentState));
    }

    private void resumeSuspendedState() {
        if (!(currentState instanceof SuspendedState) || suspendedStateReturn == null) {
            return;
        }

        currentState.onExit(this);
        // Restore the exact pre-suspension state without re-entering it from scratch.
        currentState = suspendedStateReturn;
        suspendedStateReturn = null;
        notifyListeners(l -> l.onStateChanged(currentState));
    }

    // LISTENER
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

    public void notifyListeners(Consumer<GameListener> event) {
        for (GameListener listener : listeners) {
            try {
                event.accept(listener);
            } catch (Exception e) {
                System.err.println("Listener error: " + e.getMessage());
            }
        }
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        listeners = new ArrayList<>();
        if (suspensionTimeoutMillis <= 0L) {
            suspensionTimeoutMillis = DEFAULT_SUSPENSION_TIMEOUT_MILLIS;
        }
    }
}
