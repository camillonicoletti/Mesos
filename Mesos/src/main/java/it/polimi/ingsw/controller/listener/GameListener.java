package it.polimi.ingsw.controller.listener;

import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.enums.Era;

import java.util.Map;

public interface GameListener {
    // Lobby
    void onPlayerJoined(String nickname);
    void onPlayersCountSet(int count);
    // Start
    void onGameStarted();
    void onStateChanged(GameState newState);
    // Totem Placing-
    void onTotemPlaced(String nickname, char tileId);
    // Resources change
    void onFoodChanged(String nickname, int newAmount, int delta);
    void onPointsChanged(String nickname, int newPoints, int delta);
    // Era change
    void onEraChanged(Era newEra);
    // Actions
    void onCharacterCardTaken(String nickname, String cardId, boolean fromUpperRow);
    void onBuildingCardTaken(String nickname, String cardId, boolean fromUpperRow);
    // TODO : EventResolved
    // End Round/Game
    void onRoundEnded(int roundNumber);
    void onGameEnded(Player winner, Map<Player, Integer> finalScores);

}
