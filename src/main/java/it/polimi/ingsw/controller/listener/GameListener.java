package it.polimi.ingsw.controller.listener;

import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.network.protocol.clientbound.EventResolvedMessage;

import java.util.List;
import java.util.Map;

/**
 * Observer of {@link it.polimi.ingsw.controller.GameController} and model events.
 * Concrete listeners (virtual views, persistence hook, GUI/TUI adapters)
 * implement only the callbacks they care about; methods declared {@code default}
 * are optional and no-op by default. The controller broadcasts events to every
 * registered listener and swallows individual listener exceptions so that one
 * misbehaving client cannot break the broadcast.
 */
public interface GameListener {
    // Lobby
    void onPlayerJoined(String nickname);
    void onPlayersCountSet(int count);
    // Disconnections and Recconetctions
    void onPlayerDisconnected(String nickname);
    void onPlayerReconnected(String nickname);
    void onGameSuspended();
    void onGameResumed();
    // Start
    void onGameStarted();
    void onStateChanged(GameState newState);
    // Totem Placing-
    void onTotemPlaced(String nickname, char tileId);
    // Resources change
    void onFoodChanged(String nickname, int newAmount, int delta);
    void onPointsChanged(String nickname, int newAmount, int delta);
    // Era change
    void onEraChanged(Era newEra);
    // Actions
    default void onCurrentPlayerChanged(String nickname) {
    }
    void onCharacterCardTaken(String nickname, String cardId, boolean fromUpperRow);
    void onBuildingCardTaken(String nickname, String cardId, boolean fromUpperRow);
    default void onGameSnapshotChanged() {}
    // End Round/Game
    default void onEventResolved(int cardId, String title, String description,
                                 List<EventResolvedMessage.PlayerEffect> effects) {}
    void onRoundEnded(int roundNumber);
    void onGameEnded(Player winner, Map<Player, Integer> finalScores);
    void onExtraCardPickAvailable(String nickname, int picks);

    default void onLeaderboardRank(String nickname, int rank, int playersCount) {}
}
