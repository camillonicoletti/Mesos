package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.state.ActionResolutionState;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.card.EventCard;
import it.polimi.ingsw.model.card.HuntEventCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rulebook section 8 enforcement: event cards cannot be picked by players.
 *
 * Verifies that a PickCardAction targeting an event id is rejected, unknown
 * ids are rejected, and that a regular character pick still succeeds when an
 * event shares the upper row. The character pick row exposed by
 * Round.getUpperRowTribute() only contains CharacterCards, so any event id
 * lookup must throw.
 */
class EventCardNotPickableTest {

    @SuppressWarnings("unchecked")
    private <T> List<T> roundList(Round round, String field) {
        try {
            Field f = Round.class.getDeclaredField(field);
            f.setAccessible(true);
            return (List<T>) f.get(round);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Pre-populates upper rows with the given cards before placement, so the
     * ActionResolutionState doesn't auto-complete due to empty rows.
     */
    private GameController bootstrap2pToActionResolution(
            List<EventCard> upperEvents, List<it.polimi.ingsw.model.card.CharacterCard> upperCharacters) {
        List<Player> players = new ArrayList<>(List.of(
                new Player("Alice", new Totem(TotemColor.RED,  "OFFER_TRACK")),
                new Player("Bob",   new Totem(TotemColor.BLUE, "OFFER_TRACK"))
        ));

        Game game = new Game(
                players, 1, null, Era.I, null,
                BoardFactory.createBoard(2),
                new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
        );
        // Round ctor order: lowerEvents, upperEvents, lowerTribute, upperTribute, lowerBuild, upperBuild
        Round round = new Round(1, game, Era.I,
                new ArrayList<>(), new ArrayList<>(upperEvents),
                new ArrayList<>(), new ArrayList<>(upperCharacters),
                new ArrayList<>(), new ArrayList<>());
        game.setCurrentRound(round);
        for (Player p : players) p.setGame(game);

        List<Totem> totems = new ArrayList<>();
        for (Player p : players) totems.add(p.getTotem());
        game.getBoard().getTurnOrderTile().assignRandomTurnOrder(totems);

        GameController ctrl = new GameController();
        ctrl.setGame(game);
        ctrl.changeState(new TotemPlacementState());

        // Both players place on upper-pick tiles. C and F are upper-only.
        String first = ((TotemPlacementState) ctrl.getCurrentState()).getCurrentPlayerNickname(ctrl);
        ctrl.onMessageReceived(first, new PlaceTotemAction('C'));
        String second = first.equals("Alice") ? "Bob" : "Alice";
        ctrl.onMessageReceived(second, new PlaceTotemAction('F'));

        assertTrue(ctrl.getCurrentState() instanceof ActionResolutionState,
                "Expected ActionResolutionState, got " + ctrl.getCurrentState().getClass().getSimpleName());
        return ctrl;
    }

    @Test
    void pickCardActionWithEventIdFromUpperRowIsRejected() {
        EventCard event = new HuntEventCard(777, Era.I, false, 1, 1);
        // Add an unrelated character so the upper row isn't fully drained — otherwise
        // the action phase short-circuits past the player whose turn we want to test.
        GameController ctrl = bootstrap2pToActionResolution(
                List.of(event),
                List.of(new HunterCard(556, Era.I, 0, false),
                        new HunterCard(557, Era.I, 0, false)));
        Round round = ctrl.getGame().getCurrentRound();

        String currentNick = ((ActionResolutionState) ctrl.getCurrentState()).getCurrentPlayerNickname();

        // PickCardAction looks up among CharacterCards in upper tribute — event id is absent there.
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ctrl.onMessageReceived(currentNick, new PickCardAction("777", true))
        );
        assertTrue(ex.getMessage() == null || ex.getMessage().toLowerCase().contains("not found"),
                "Expected a 'not found' style error, got: " + ex.getMessage());

        // Event remains in the row — the failed action must not have any side effect.
        assertEquals(1, round.getUpperRowEvents().size());
    }

    @Test
    void pickCardActionWithUnknownIdIsRejected() {
        GameController ctrl = bootstrap2pToActionResolution(
                List.of(),
                List.of(new HunterCard(556, Era.I, 0, false),
                        new HunterCard(557, Era.I, 0, false)));

        String currentNick = ((ActionResolutionState) ctrl.getCurrentState()).getCurrentPlayerNickname();
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.onMessageReceived(currentNick, new PickCardAction("12345", true)));
    }

    @Test
    void characterPickStillWorksWhenAnEventSharesTheUpperRow() {
        GameController ctrl = bootstrap2pToActionResolution(
                List.of(new HuntEventCard(777, Era.I, false, 1, 1)),
                List.of(new HunterCard(555, Era.I, 0, false),
                        new HunterCard(558, Era.I, 0, false)));
        Round round = ctrl.getGame().getCurrentRound();

        String currentNick = ((ActionResolutionState) ctrl.getCurrentState()).getCurrentPlayerNickname();
        ctrl.onMessageReceived(currentNick, new PickCardAction("555", true));

        // Character consumed; event still in place.
        assertEquals(1, round.getUpperRowTribute().size());
        assertEquals(1, round.getUpperRowEvents().size());
    }
}
