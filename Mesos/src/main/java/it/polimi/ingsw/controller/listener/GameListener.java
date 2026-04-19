package it.polimi.ingsw.controller.listener;

import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.model.Player;

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
    // Actions
    void onCharacterCardTaken(String nickname, String cardId, boolean fromUpperRow);
    void onBuildingCardTaken(String nickname, String cardId, boolean fromUpperRow);
    // End Round/Game
    void onRoundEnded(int roundNumber);
    void onGameEnded(Player winner, Map<Player, Integer> finalScores);

}
