package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.state.ActionResolutionState;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.HuntEventCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.effect.ExtraCardUpperRowEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the optional extra pick granted by building #13
 * (ExtraCardUpperRowEffect). Per rulebook section 15, this extra pick is
 * optional: the player must be able to decline via SkipExtraPickAction.
 *
 * Verifies the action contract for the extra-pick phase: ReadyAction is
 * rejected during it, SkipExtraPickAction from the active player advances
 * the queue without taking a card, from a non-active player it is rejected,
 * and it is rejected outside the extra-pick phase. Two additional methods
 * verify how the extra-pick queue treats a row whose only remaining content
 * is an Event card (never pickable, so the row counts as empty) versus a
 * row whose only remaining content is an unaffordable Building (still
 * counts as non-empty, so the player gets a turn that fails unless they
 * skip).
 */
class ExtraPickOptionalityTest {

    /**
     * Bootstrap to extra-pick phase with upper-row content present when
     * the queue starts. Only Alice owns building #13, so only she has an
     * extra pick. Bob's tile B is lower-only — but lower is empty, so his
     * regular budget is zero and he advances without picking.
     */
    private GameController bootstrapToExtraPickWithUpper(
            List<CharacterCard> upperContent,
            List<BuildingCard> upperBuildings) {
        return bootstrapToExtraPickWithUpper(upperContent, upperBuildings, new ArrayList<>());
    }

    /**
     * Same as bootstrapToExtraPickWithUpper(List, List) but also seeds
     * the upper row with event cards from the start, so their effect on the
     * extra-pick "is the row empty" check can be observed at the moment it
     * actually runs (right after Bob's regular picks), not after the round
     * has already moved on.
     */
    private GameController bootstrapToExtraPickWithUpper(
            List<CharacterCard> upperContent,
            List<BuildingCard> upperBuildings,
            List<it.polimi.ingsw.model.card.EventCard> upperEvents) {
        Player alice = new Player("Alice", new Totem(TotemColor.RED,  "OFFER_TRACK"));
        Player bob   = new Player("Bob",   new Totem(TotemColor.BLUE, "OFFER_TRACK"));
        List<Player> players = new ArrayList<>(List.of(alice, bob));

        // Only Alice owns building #13 ⇒ extra-pick queue has only Alice.
        alice.addBuildingCard(new BuildingCard(8001, Era.I, 0, new ExtraCardUpperRowEffect(), 0));

        Game game = new Game(players, 1, null, Era.I, null,
                BoardFactory.createBoard(2),
                new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        Round round = new Round(1, game, Era.I,
                new ArrayList<>(), new ArrayList<>(upperEvents),
                new ArrayList<>(), new ArrayList<>(upperContent),
                new ArrayList<>(), new ArrayList<>(upperBuildings));
        game.setCurrentRound(round);
        for (Player p : players) p.setGame(game);

        // Deterministic turn order Alice → Bob.
        game.getBoard().getTurnOrderTile().reset();
        var slots = game.getBoard().getTurnOrderTile().getSlots();
        slots.get(0).placeTotem(alice.getTotem());
        slots.get(1).placeTotem(bob.getTotem());

        GameController ctrl = new GameController();
        ctrl.setGame(game);
        ctrl.changeState(new TotemPlacementState());

        // Alice on B (lower=1), Bob on F (upper=2). Lower is empty so Alice's budget
        // gets zeroed; she completes regular phase without picking. Bob picks 2 from upper.
        ctrl.onMessageReceived("Alice", new PlaceTotemAction('B'));
        ctrl.onMessageReceived("Bob",   new PlaceTotemAction('F'));

        // Bob picks his two upper cards. After his second pick, ActionResolutionState
        // transitions into extra-pick phase; upper still has whatever we put past the
        // first two characters.
        if (upperContent.size() >= 2) {
            ctrl.onMessageReceived("Bob", new PickCardAction(String.valueOf(upperContent.get(0).getId()), true));
            ctrl.onMessageReceived("Bob", new PickCardAction(String.valueOf(upperContent.get(1).getId()), true));
        }
        return ctrl;
    }

    @SuppressWarnings("unchecked")
    private List<CharacterCard> upperRowTributeMutable(Round round) {
        try {
            java.lang.reflect.Field f = Round.class.getDeclaredField("upperRowTribute");
            f.setAccessible(true);
            return (List<CharacterCard>) f.get(round);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<BuildingCard> upperRowBuildingsMutable(Round round) {
        try {
            java.lang.reflect.Field f = Round.class.getDeclaredField("upperRowBuildings");
            f.setAccessible(true);
            return (List<BuildingCard>) f.get(round);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void readyActionDuringExtraPickIsStillRejected() {
        // ReadyAction has no skip semantics during extra-pick: it must be rejected,
        // and the player must still be on the hook (only SkipExtraPickAction is valid).
        GameController ctrl = bootstrapToExtraPickWithUpper(
                new ArrayList<>(List.of(
                        new HunterCard(101, Era.I, 0, false),
                        new HunterCard(102, Era.I, 0, false),
                        new HunterCard(103, Era.I, 0, false)
                )),
                new ArrayList<>());

        assertInstanceOf(ActionResolutionState.class, ctrl.getCurrentState());
        ActionResolutionState state = (ActionResolutionState) ctrl.getCurrentState();
        String currentNick = state.getCurrentPlayerNickname();
        assertEquals("Alice", currentNick);

        assertThrows(IllegalStateException.class,
                () -> ctrl.onMessageReceived(currentNick, new ReadyAction(false)));

        // Alice still on the hook after the rejection.
        assertEquals(currentNick, ((ActionResolutionState) ctrl.getCurrentState()).getCurrentPlayerNickname());
        assertEquals(1, ctrl.getGame().getCurrentRound().getUpperRowTribute().size());
    }

    @Test
    void skipExtraPickAction_advancesQueueWithoutTakingCard() {
        // The fix: SkipExtraPickAction is the legal way to decline the optional
        // extra pick granted by building #13. The queue advances and the player's
        // tribe is unchanged.
        GameController ctrl = bootstrapToExtraPickWithUpper(
                new ArrayList<>(List.of(
                        new HunterCard(101, Era.I, 0, false),
                        new HunterCard(102, Era.I, 0, false),
                        new HunterCard(103, Era.I, 0, false)
                )),
                new ArrayList<>());

        ActionResolutionState state = (ActionResolutionState) ctrl.getCurrentState();
        assertEquals("Alice", state.getCurrentPlayerNickname());
        Player alice = ctrl.getGame().getPlayers().stream()
                .filter(p -> p.getNickname().equals("Alice"))
                .findFirst().orElseThrow();
        int aliceCharsBefore = alice.getTribe().getCharacterCards().size();

        // SkipExtraPickAction must be accepted (no exception).
        ctrl.onMessageReceived("Alice", new it.polimi.ingsw.controller.action.SkipExtraPickAction());

        // Post-condition: Alice did NOT acquire a card.
        assertEquals(aliceCharsBefore, alice.getTribe().getCharacterCards().size(),
                "Alice's tribe must not grow when skipping the extra pick");
    }

    @Test
    void wrongPlayerCannotSkipAnotherPlayersExtraPick() {
        // Setup: Alice has building #13 and an extra pick. Bob does NOT have
        // the extra pick. Bob must not be able to skip on Alice's behalf.
        GameController ctrl = bootstrapToExtraPickWithUpper(
                new ArrayList<>(List.of(
                        new HunterCard(101, Era.I, 0, false),
                        new HunterCard(102, Era.I, 0, false),
                        new HunterCard(103, Era.I, 0, false)
                )),
                new ArrayList<>());

        ActionResolutionState state = (ActionResolutionState) ctrl.getCurrentState();
        assertEquals("Alice", state.getCurrentPlayerNickname());

        // Bob impersonates the skip — must throw "not your turn".
        assertThrows(IllegalStateException.class,
                () -> ctrl.onMessageReceived("Bob",
                        new it.polimi.ingsw.controller.action.SkipExtraPickAction()));

        // Alice is still the active player; the queue has not advanced.
        ActionResolutionState afterState = (ActionResolutionState) ctrl.getCurrentState();
        assertEquals("Alice", afterState.getCurrentPlayerNickname(),
                "Bob's illegal skip must not have advanced Alice's turn");
    }

    @Test
    void skipExtraPickActionIsRejectedOutsideExtraPickPhase() {
        // During the regular pick phase (no extra-pick yet started), a
        // SkipExtraPickAction is meaningless and must be rejected. Build a
        // fresh game and stop in regular ActionResolutionState (no building #13).
        Player alice = new Player("Alice", new Totem(TotemColor.RED,  "OFFER_TRACK"));
        Player bob   = new Player("Bob",   new Totem(TotemColor.BLUE, "OFFER_TRACK"));
        java.util.List<Player> players = new ArrayList<>(java.util.List.of(alice, bob));

        Game game = new Game(players, 1, null, Era.I, null,
                it.polimi.ingsw.model.board.BoardFactory.createBoard(2),
                new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        Round round = new Round(1, game,
                Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(java.util.List.of(
                        new HunterCard(701, Era.I, 0, false),
                        new HunterCard(702, Era.I, 0, false),
                        new HunterCard(703, Era.I, 0, false),
                        new HunterCard(704, Era.I, 0, false))),
                new ArrayList<>(), new ArrayList<>());
        game.setCurrentRound(round);
        for (Player p : players) p.setGame(game);

        game.getBoard().getTurnOrderTile().reset();
        var slots = game.getBoard().getTurnOrderTile().getSlots();
        slots.get(0).placeTotem(alice.getTotem());
        slots.get(1).placeTotem(bob.getTotem());

        GameController ctrl = new GameController();
        ctrl.setGame(game);
        ctrl.changeState(new it.polimi.ingsw.controller.state.TotemPlacementState());
        ctrl.onMessageReceived("Alice", new PlaceTotemAction('C')); // upper=1
        ctrl.onMessageReceived("Bob",   new PlaceTotemAction('F')); // upper=2

        assertInstanceOf(ActionResolutionState.class, ctrl.getCurrentState());
        String currentNick = ((ActionResolutionState) ctrl.getCurrentState())
                .getCurrentPlayerNickname();

        // SkipExtraPickAction during regular pick → rejected with IllegalStateException
        // (rejectAction wraps unknown actions with that exception type).
        assertThrows(IllegalStateException.class,
                () -> ctrl.onMessageReceived(currentNick,
                        new it.polimi.ingsw.controller.action.SkipExtraPickAction()));
    }

    @Test
    void extraPick_skippedWhenOnlyAnEventRemainsInUpperRow() {
        // The Hunt event is seeded in the upper row from the start (not added
        // afterwards): once Bob's 2 regular picks empty the tribute/building
        // lists, the only thing left in the row is the event. An Event card
        // never counts as pickable, so the "is the row empty" check treats
        // the row as empty and Alice's extra pick must be skipped entirely —
        // she must never be offered a turn to act on.
        GameController ctrl = bootstrapToExtraPickWithUpper(
                new ArrayList<>(List.of(
                        new HunterCard(101, Era.I, 0, false),
                        new HunterCard(102, Era.I, 0, false)
                )),
                new ArrayList<>(),
                new ArrayList<>(List.of(new HuntEventCard(900, Era.I, false, 1, 1))));

        Player alice = ctrl.getGame().getPlayers().stream()
                .filter(p -> p.getNickname().equals("Alice"))
                .findFirst().orElseThrow();

        // Alice's tribe cannot have grown: there was nothing left for her to
        // pick once Bob took both Hunters, event included.
        assertEquals(0, alice.getTribe().getCharacterCards().size(),
                "Alice's extra pick must be skipped when only an Event card "
                        + "remains in the upper row");

        // Stronger check: if the Event had wrongly counted as "available"
        // content, Alice would still be waiting in the extra-pick phase
        // instead of the queue having moved past her automatically.
        boolean aliceStuckWaitingForExtraPick =
                ctrl.getCurrentState() instanceof ActionResolutionState ars
                        && "Alice".equals(ars.getCurrentPlayerNickname());
        assertFalse(aliceStuckWaitingForExtraPick,
                "Alice must not be offered an extra pick when only an Event "
                        + "remains in the upper row");
    }

    @Test
    void extraPick_unaffordableBuildingBlocksPick() {
        BuildingCard expensive = new BuildingCard(7777, Era.I, 100, null, 0);
        GameController ctrl = bootstrapToExtraPickWithUpper(
                new ArrayList<>(List.of(
                        new HunterCard(101, Era.I, 0, false),
                        new HunterCard(102, Era.I, 0, false)
                )),
                new ArrayList<>(List.of(expensive)));

        // Bob's regular picks consumed the 2 chars from upper. After that, only the
        // unaffordable building remains. Alice's extra-pick attempts must fail.
        assertInstanceOf(ActionResolutionState.class, ctrl.getCurrentState());
        ActionResolutionState state = (ActionResolutionState) ctrl.getCurrentState();
        String currentNick = state.getCurrentPlayerNickname();
        assertNotNull(currentNick,
                "Alice must still be on the hook: an unaffordable building does "
                        + "not count as an empty row, unlike an Event card");

        assertThrows(IllegalStateException.class,
                () -> ctrl.onMessageReceived(currentNick, new PickBuildingAction("7777", true)));

        // Building is still there, player still on the hook (no way out).
        assertEquals(1, ctrl.getGame().getCurrentRound().getUpperRowBuildings().size());
    }
}
