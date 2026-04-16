package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.controller.state.LobbyState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Totem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Main controller context of the application.
 * It stores the current controller state and the data needed
 * before and during the game.
 */
public class GameController implements Serializable {

    private static final long serialVersionUID = 1L;

    private Game game;
    private GameState currentState;

    private final List<Player> lobbyPlayers;
    private int chosenPlayersCount;

    public GameController() {
        this.game = null;
        this.currentState = new LobbyState();
        this.lobbyPlayers = new ArrayList<>();
        this.chosenPlayersCount = -1;

        currentState.onEnter(this);
    }

    /**
     * Receives an action from a player and delegates it to the current state.
     *
     * @param playerNickname the nickname of the player who sent the action
     * @param action the received action
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

    /**
     * Changes the current controller state.
     *
     * @param newState the new state to enter
     */
    public void changeState(GameState newState) {
        if (newState == null) {
            throw new IllegalArgumentException("New state cannot be null");
        }

        currentState.onExit(this);
        currentState = newState;
        currentState.onEnter(this);
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
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
                .anyMatch(player -> player.getTotem().equals(totem));
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
}