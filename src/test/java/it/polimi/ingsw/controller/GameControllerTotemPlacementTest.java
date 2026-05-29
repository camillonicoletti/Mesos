package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.controller.state.ActionResolutionState;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameController totem placement state.
 *
 * <p>Verifies: valid and invalid totem placements on offer tiles and turn-order tiles.
 */
class GameControllerTotemPlacementTest {

    /**
     * Verifies that a player who is not the current placement player cannot place a totem.
     * The failed action must not occupy any offer tile and must not register any player choice.
     */
    @Test
    void nonCurrentPlayerCannotPlaceTotemAndDoesNotMutateModel() {
        GameController controller = createStartedTwoPlayerGame();
        String currentPlayer = controller.getCurrentTurnPlayerNickname();
        String wrongPlayer = otherPlayer(currentPlayer);

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived(wrongPlayer, new PlaceTotemAction('B'))
        );

        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
        assertEquals(0, controller.getGame().getCurrentRound().getPlayerChoices().size());
        assertFalse(anyOfferTileOccupied(controller));
        assertEquals(currentPlayer, controller.getCurrentTurnPlayerNickname());
    }

    /**
     * Verifies that card-picking actions are rejected during totem placement.
     * This protects the model from state-skipping actions sent too early by a client.
     */
    @Test
    void pickCardActionDuringTotemPlacementDoesNotMutateModel() {
        GameController controller = createStartedTwoPlayerGame();
        String currentPlayer = controller.getCurrentTurnPlayerNickname();

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived(currentPlayer, new PickCardAction("1", true))
        );

        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
        assertEquals(0, controller.getGame().getCurrentRound().getPlayerChoices().size());
        assertFalse(anyOfferTileOccupied(controller));
        assertEquals(currentPlayer, controller.getCurrentTurnPlayerNickname());
    }

    /**
     * Verifies that an unknown tile id is rejected before changing the round choices.
     * A malformed client command must leave the board exactly as it was.
     */
    @Test
    void unknownOfferTileDoesNotMutateModel() {
        GameController controller = createStartedTwoPlayerGame();
        String currentPlayer = controller.getCurrentTurnPlayerNickname();

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived(currentPlayer, new PlaceTotemAction('Z'))
        );

        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
        assertEquals(0, controller.getGame().getCurrentRound().getPlayerChoices().size());
        assertFalse(anyOfferTileOccupied(controller));
        assertEquals(currentPlayer, controller.getCurrentTurnPlayerNickname());
    }

    /**
     * Verifies that a tile known by the game rules but unavailable for the current player count
     * is rejected without creating a player choice.
     */
    @Test
    void unavailableOfferTileForTwoPlayersDoesNotMutateModel() {
        GameController controller = createStartedTwoPlayerGame();
        String currentPlayer = controller.getCurrentTurnPlayerNickname();

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived(currentPlayer, new PlaceTotemAction('A'))
        );

        assertNull(findOfferTile(controller, 'A'),
                "Tile A must not be present on the board in a two-player game.");
        assertEquals(0, controller.getGame().getCurrentRound().getPlayerChoices().size());
        assertFalse(anyOfferTileOccupied(controller));
        assertEquals(currentPlayer, controller.getCurrentTurnPlayerNickname());
    }

    /**
     * Verifies that a valid placement occupies exactly one tile, records exactly one choice,
     * and advances the placement turn to the next player.
     */
    @Test
    void validPlacementRecordsChoiceAndAdvancesCurrentPlayer() {
        GameController controller = createStartedTwoPlayerGame();
        String firstPlayer = controller.getCurrentTurnPlayerNickname();
        String secondPlayer = otherPlayer(firstPlayer);

        controller.onMessageReceived(firstPlayer, new PlaceTotemAction('B'));

        OfferTile tileB = findOfferTile(controller, 'B');
        assertNotNull(tileB);
        assertTrue(tileB.isOccupied());
        assertFalse(isTotemOnTurnOrder(controller, firstPlayer));
        assertEquals(1, controller.getGame().getCurrentRound().getPlayerChoices().size());
        assertEquals(secondPlayer, controller.getCurrentTurnPlayerNickname());
        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
    }

    /**
     * Verifies that after all totems are placed, action resolution starts from the leftmost
     * offer tile, not from the original placement order.
     */
    @Test
    void actionResolutionStartsFromLeftmostPlacedOfferTile() {
        GameController controller = createStartedTwoPlayerGame();
        String firstPlayer = controller.getCurrentTurnPlayerNickname();
        String secondPlayer = otherPlayer(firstPlayer);

        controller.onMessageReceived(firstPlayer, new PlaceTotemAction('F'));
        controller.onMessageReceived(secondPlayer, new PlaceTotemAction('B'));

        assertInstanceOf(ActionResolutionState.class, controller.getCurrentState());
        assertEquals(secondPlayer, controller.getCurrentActionPlayerNickname(),
                "Tile B is left of tile F, so the player on B must resolve first.");
    }

    /**
     * Verifies that an occupied tile cannot be selected by another player and that
     * the original placement remains unchanged after the rejected action.
     */
    @Test
    void occupiedTileCannotBeOverwrittenBySecondPlayer() {
        GameController controller = createStartedTwoPlayerGame();
        String firstPlayer = controller.getCurrentTurnPlayerNickname();
        String secondPlayer = otherPlayer(firstPlayer);

        controller.onMessageReceived(firstPlayer, new PlaceTotemAction('B'));

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived(secondPlayer, new PlaceTotemAction('B'))
        );

        OfferTile tileB = findOfferTile(controller, 'B');
        assertNotNull(tileB);
        assertTrue(tileB.isOccupied());
        assertEquals(firstPlayer,
                controller.getGame().getCurrentRound().getPlayerChoices().keySet().iterator().next().getNickname());
        assertEquals(1, controller.getGame().getCurrentRound().getPlayerChoices().size());
        assertEquals(secondPlayer, controller.getCurrentTurnPlayerNickname());
    }

    private GameController createStartedTwoPlayerGame() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));
        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onMessageReceived("Bob", new ReadyAction(true));

        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
        return controller;
    }

    private Totem createTotem(TotemColor color) {
        return new Totem(color, "OFFER_TRACK");
    }

    private String otherPlayer(String nickname) {
        return "Alice".equals(nickname) ? "Bob" : "Alice";
    }

    private OfferTile findOfferTile(GameController controller, char letter) {
        List<OfferTile> tiles = controller.getGame().getBoard().getOfferTiles();
        return tiles.stream()
                .filter(tile -> tile.getLetter() == letter)
                .findFirst()
                .orElse(null);
    }

    private boolean anyOfferTileOccupied(GameController controller) {
        return controller.getGame().getBoard().getOfferTiles()
                .stream()
                .anyMatch(OfferTile::isOccupied);
    }

    private boolean isTotemOnTurnOrder(GameController controller, String nickname) {
        Totem totem = controller.getGame().getPlayers().stream()
                .filter(player -> player.getNickname().equals(nickname))
                .findFirst()
                .orElseThrow()
                .getTotem();
        return controller.getGame().getBoard().getTurnOrderTile().getSlots().stream()
                .anyMatch(slot -> slot.getOccupiedBy() == totem);
    }
}
