package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.ActionSlot;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.FoodSlot;
import it.polimi.ingsw.model.board.LowerRowSlot;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.board.TurnOrderTile;
import it.polimi.ingsw.model.board.UpperRowSlot;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge cases for TotemPlacementState using a minimal real Game/Round/Board.
 * Still no network/server/client classes are used here.
 */
public class GameControllerTotemPlacementEdgeTest {

    @Test
    void unknownPlayerCannotPlaceTotem() {
        GameController controller = createTotemPlacementController();

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived("Ghost", new PlaceTotemAction('A'))
        );
    }

    @Test
    void playerCannotPlaceTotemOnUnknownTile() {
        GameController controller = createTotemPlacementController();

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived("Alice", new PlaceTotemAction('Z'))
        );
    }

    @Test
    void playerCannotPlaceTotemOnTileUnavailableForPlayerCount() {
        GameController controller = createTotemPlacementController();

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived("Alice", new PlaceTotemAction('E'))
        );
    }

    @Test
    void samePlayerCannotPlaceTotemTwiceInSameRound() {
        GameController controller = createTotemPlacementController();

        controller.onMessageReceived("Alice", new PlaceTotemAction('A'));

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived("Alice", new PlaceTotemAction('B'))
        );
    }

    @Test
    void twoPlayersCannotPlaceOnSameOfferTile() {
        GameController controller = createTotemPlacementController();

        controller.onMessageReceived("Alice", new PlaceTotemAction('A'));

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived("Bob", new PlaceTotemAction('A'))
        );
    }

    @Test
    void disconnectedPlayerCannotPlaceTotem() {
        GameController controller = createTotemPlacementController();

        controller.markPlayerDisconnected("Alice");

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived("Alice", new PlaceTotemAction('A'))
        );
    }

    @Test
    void placingTotemNotifiesListeners() {
        GameController controller = createTotemPlacementController();
        RecordingListener listener = new RecordingListener();
        controller.addListener(listener);

        controller.onMessageReceived("Alice", new PlaceTotemAction('A'));

        assertTrue(listener.events.contains("placed:Alice:A"));
    }

    @Test
    void ifOnlyConnectedPlayersHavePlacedAfterDisconnectionControllerMovesForward() {
        GameController controller = createTotemPlacementController();

        controller.onMessageReceived("Alice", new PlaceTotemAction('A'));
        controller.onPlayerDisconnection("Bob");

        assertTrue(controller.isPlayerDisconnected("Bob"));
        assertFalse(controller.getCurrentState() instanceof TotemPlacementState);
    }

    /**
     * Tiles granting only automatic Food effects (e.g. tile A) leave the
     * player no choice to make, so ActionResolutionState must advance the
     * round immediately instead of waiting for a pick that will never come.
     */
    @Test
    void automaticFoodOnlyTilesDoNotLeaveControllerStuckInActionResolution() {
        GameController controller = createFoodOnlyPlacementController();

        controller.onMessageReceived("Alice", new PlaceTotemAction('A'));
        controller.onMessageReceived("Bob", new PlaceTotemAction('B'));

        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
        assertEquals(2, controller.getGame().getCurrentRound().getNumber());
    }

    /**
     * Verifies that when a placed tile requires a pick from a row that is
     * empty, the controller auto-skips the pick instead of leaving the
     * player stuck waiting. The round must advance past ActionResolutionState
     * without an action from the player.
     */
    @Test
    void emptyRequiredRowShouldAutoSkipInsteadOfWaitingForImpossiblePick() {
        GameController controller = createEmptyRowsButPickRequiredController();

        controller.onMessageReceived("Alice", new PlaceTotemAction('A'));
        controller.onMessageReceived("Bob", new PlaceTotemAction('B'));

        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
    }

    private GameController createTotemPlacementController() {
        GameController controller = new GameController();
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        List<Player> players = new ArrayList<>(List.of(alice, bob));

        Board board = new Board(
                new ArrayList<>(List.of(
                        new OfferTile('A', 1, List.of(new ActionSlot(new LowerRowSlot(), 1))),
                        new OfferTile('B', 1, List.of(new ActionSlot(new UpperRowSlot(), 1))),
                        new OfferTile('E', 5, List.of(new ActionSlot(new FoodSlot(), 3)))
                )),
                TurnOrderTile.createForPlayers(2)
        );

        Game game = new Game(players, 1, null, Era.I, null, board,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        Round round = new Round(1, game, Era.I,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>());
        game.setCurrentRound(round);
        alice.setGame(game);
        bob.setGame(game);

        controller.addLobbyPlayer(alice);
        controller.addLobbyPlayer(bob);
        controller.setGame(game);
        controller.changeState(new TotemPlacementState());
        return controller;
    }

    private GameController createFoodOnlyPlacementController() {
        GameController controller = new GameController();
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        List<Player> players = new ArrayList<>(List.of(alice, bob));

        Board board = new Board(
                new ArrayList<>(List.of(
                        new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 3))),
                        new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 0)))
                )),
                TurnOrderTile.createForPlayers(2)
        );

        Game game = new Game(players, 1, null, Era.I, null, board,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        Round round = new Round(1, game, Era.I,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>());
        game.setCurrentRound(round);
        alice.setGame(game);
        bob.setGame(game);

        controller.addLobbyPlayer(alice);
        controller.addLobbyPlayer(bob);
        controller.setGame(game);
        controller.changeState(new TotemPlacementState());
        return controller;
    }

    private GameController createEmptyRowsButPickRequiredController() {
        GameController controller = new GameController();
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        List<Player> players = new ArrayList<>(List.of(alice, bob));

        Board board = new Board(
                new ArrayList<>(List.of(
                        new OfferTile('A', 1, List.of(new ActionSlot(new LowerRowSlot(), 1))),
                        new OfferTile('B', 1, List.of(new ActionSlot(new UpperRowSlot(), 1)))
                )),
                TurnOrderTile.createForPlayers(2)
        );

        Game game = new Game(players, 1, null, Era.I, null, board,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        Round round = new Round(1, game, Era.I,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>());
        game.setCurrentRound(round);
        alice.setGame(game);
        bob.setGame(game);

        controller.addLobbyPlayer(alice);
        controller.addLobbyPlayer(bob);
        controller.setGame(game);
        controller.changeState(new TotemPlacementState());
        return controller;
    }

    private static Totem createTotem(TotemColor color) {
        return new Totem(color, "OFFER_TRACK");
    }

    private static class RecordingListener implements GameListener {
        private final List<String> events = new ArrayList<>();

        @Override public void onPlayerJoined(String nickname) { events.add("joined:" + nickname); }
        @Override public void onPlayersCountSet(int count) { events.add("count:" + count); }
        @Override public void onPlayerDisconnected(String nickname) { events.add("disconnected:" + nickname); }
        @Override public void onPlayerReconnected(String nickname) { events.add("reconnected:" + nickname); }
        @Override public void onGameSuspended() { events.add("suspended"); }
        @Override public void onGameResumed() { events.add("resumed"); }
        @Override public void onGameStarted() { events.add("started"); }
        @Override public void onStateChanged(GameState newState) { events.add("state:" + newState.getClass().getSimpleName()); }
        @Override public void onTotemPlaced(String nickname, char tileId) { events.add("placed:" + nickname + ":" + tileId); }
        @Override public void onFoodChanged(String nickname, int newAmount, int delta) { events.add("food:" + nickname + ":" + newAmount + ":" + delta); }
        @Override public void onPointsChanged(String nickname, int newAmount, int delta) { events.add("points:" + nickname + ":" + newAmount + ":" + delta); }
        @Override public void onEraChanged(Era newEra) { events.add("era:" + newEra); }
        @Override public void onCharacterCardTaken(String nickname, String cardId, boolean fromUpperRow) { events.add("character:" + nickname + ":" + cardId + ":" + fromUpperRow); }
        @Override public void onBuildingCardTaken(String nickname, String cardId, boolean fromUpperRow) { events.add("building:" + nickname + ":" + cardId + ":" + fromUpperRow); }
        @Override public void onRoundEnded(int roundNumber) { events.add("round:" + roundNumber); }
        @Override public void onGameEnded(Player winner, Map<Player, Integer> finalScores) { events.add("ended:" + (winner == null ? "null" : winner.getNickname())); }
        @Override public void onExtraCardPickAvailable(String nickname, int picks) { events.add("extra:" + nickname + ":" + picks); }
    }
}
