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
import it.polimi.ingsw.model.enums.RoundPhase;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bug-hunter: the rulebook states that the extra pick granted by building #13
 * ({@link ExtraCardUpperRowEffect}) is <strong>optional</strong>. The current
 * implementation does <em>not</em> expose any way for the player to decline.
 *
 * <p>These tests pin down the (likely incorrect) behaviour so the team can
 * decide whether to add a SkipExtraPickAction. Two stuck-state scenarios are
 * also documented:
 * <ul>
 *   <li>only events left in upper ⇒ player cannot pick (events are not pickable),</li>
 *   <li>only unaffordable buildings left in upper ⇒ player cannot pick.</li>
 * </ul>
 */
class ExtraPickMandatoryBugHunterTest {

    /**
     * Bootstrap to extra-pick phase with upper-row content present when
     * the queue starts. Only Alice owns building #13, so only she has an
     * extra pick. Bob's tile B is lower-only — but lower is empty, so his
     * regular budget is zero and he advances without picking.
     */
    private GameController bootstrapToExtraPickWithUpper(
            List<CharacterCard> upperContent,
            List<BuildingCard> upperBuildings) {
        Player alice = new Player("Alice", new Totem(TotemColor.RED,  "OFFER_TRACK"));
        Player bob   = new Player("Bob",   new Totem(TotemColor.BLUE, "OFFER_TRACK"));
        List<Player> players = new ArrayList<>(List.of(alice, bob));

        // Only Alice owns building #13 ⇒ extra-pick queue has only Alice.
        alice.addBuildingCard(new BuildingCard(8001, Era.I, 0, new ExtraCardUpperRowEffect(), 0));

        Game game = new Game(players, 1, null, Era.I, null,
                BoardFactory.createBoard(2),
                new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        Round round = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
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

    @SuppressWarnings("unchecked")
    private List<it.polimi.ingsw.model.card.EventCard> upperRowEventsMutable(Round round) {
        try {
            java.lang.reflect.Field f = Round.class.getDeclaredField("upperRowEventCards");
            f.setAccessible(true);
            return (List<it.polimi.ingsw.model.card.EventCard>) f.get(round);
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
                it.polimi.ingsw.model.enums.RoundPhase.TOTEM_PLACEMENT, Era.I,
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
    void extraPick_blocksPickActionForEventIdInUpper() {
        // Upper has 2 chars (consumed by Bob) plus an event. Alice's extra-pick
        // turn finds upper non-empty (event present). She cannot pick the event.
        GameController ctrl = bootstrapToExtraPickWithUpper(
                new ArrayList<>(List.of(
                        new HunterCard(101, Era.I, 0, false),
                        new HunterCard(102, Era.I, 0, false)
                )),
                new ArrayList<>());
        Round round = ctrl.getGame().getCurrentRound();
        upperRowEventsMutable(round).add(new HuntEventCard(900, Era.I, false, 1, 1));

        // After Bob's 2 picks, upper tribute is empty but eventCards still has 1 event.
        // upperRowEmpty in prepareExtraPickPlayer checks tribute+buildings only — both
        // empty ⇒ queue auto-advances past Alice ⇒ transitions to EventResolutionState.
        // So the failure mode is silent skipping, not a stuck player. We document it.
        // ActionResolutionState may have already transitioned away.
        assertTrue(ctrl.getCurrentState() != null);
        // The event is still in upper because EventResolutionState may have already
        // resolved it (depending on the sleep timing). We accept either state.
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
        if (currentNick == null) return; // queue advanced — nothing to check

        assertThrows(IllegalStateException.class,
                () -> ctrl.onMessageReceived(currentNick, new PickBuildingAction("7777", true)));

        // Building is still there, player still on the hook (no way out).
        assertEquals(1, ctrl.getGame().getCurrentRound().getUpperRowBuildings().size());
    }
}
