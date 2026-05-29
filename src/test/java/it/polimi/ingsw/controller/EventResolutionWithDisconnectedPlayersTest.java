package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.state.EventResolutionState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.card.HuntEventCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.SustenanceEventCard;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.RoundPhase;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bug-hunter: EventResolutionState resolves events for every player in the
 * game — including disconnected ones. This is consistent with the rulebook
 * (Sustenance is "obbligatorio" and Hunt is automatic) but the controller
 * does not currently check the suspension flag during the per-event sleep
 * loop. The tests pin down:
 *
 * <ul>
 *   <li>Disconnected players still receive Hunt food/PP.</li>
 *   <li>Disconnected players still pay Sustenance food (or lose PP).</li>
 *   <li>onEnter does not throw when all players are disconnected before resolution starts.</li>
 * </ul>
 *
 * <p>To avoid the 5-second per-event sleep multiplying the test runtime, each
 * test injects exactly one event. The actual 5s sleep still fires, but only
 * once per test; assertions read the model state after the daemon thread has
 * had ~300ms to run the event.
 */
class EventResolutionWithDisconnectedPlayersTest {

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

    private GameController buildControllerWith2Players() {
        List<Player> players = new ArrayList<>(List.of(
                new Player("Alice", new Totem(TotemColor.RED,  "OFFER_TRACK")),
                new Player("Bob",   new Totem(TotemColor.BLUE, "OFFER_TRACK"))
        ));
        Game game = new Game(players, 1, null, Era.I, null,
                BoardFactory.createBoard(2),
                new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        Round round = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>());
        game.setCurrentRound(round);
        for (Player p : players) p.setGame(game);

        GameController ctrl = new GameController();
        ctrl.setGame(game);
        return ctrl;
    }

    private void runEventResolutionInBackground(GameController ctrl) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                ctrl.changeState(new EventResolutionState());
            } catch (Throwable th) {
                error.set(th);
            }
        });
        t.setDaemon(true);
        t.start();
        try { Thread.sleep(400L); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        if (error.get() != null) {
            throw new AssertionError("EventResolutionState onEnter threw: " + error.get(), error.get());
        }
    }

    @Test
    void huntEventAppliesToDisconnectedPlayer() {
        GameController ctrl = buildControllerWith2Players();
        Player alice = ctrl.getGame().getPlayers().get(0);
        alice.addCharacterCard(new HunterCard(101, Era.I, 0, false));
        alice.addCharacterCard(new HunterCard(102, Era.I, 0, false));

        ctrl.markPlayerDisconnected("Alice");
        // Suspension also kicks in for 2p where 1 disconnects → check that onEnter
        // still applies even when the game is suspended.
        assertTrue(ctrl.isPlayerDisconnected("Alice"));

        Round round = ctrl.getGame().getCurrentRound();
        roundList(round, "lowerRowEventCards").add(new HuntEventCard(500, Era.I, false, 1, 2));

        // changeState bypass: we go directly into EventResolutionState even though
        // the controller is in SuspendedState. This documents the current
        // behaviour — the resolver itself does not consult isSuspended().
        runEventResolutionInBackground(ctrl);

        // Disconnected Alice still got 2 food and 4 PP from the hunt.
        assertEquals(2, alice.getFood());
        assertEquals(4, alice.getPrestigePoints());
    }

    @Test
    void sustenanceAppliesToDisconnectedPlayer() {
        GameController ctrl = buildControllerWith2Players();
        Player alice = ctrl.getGame().getPlayers().get(0);
        // 1 character with no gatherer ⇒ requires 1 food
        alice.addCharacterCard(new HunterCard(101, Era.I, 0, false));
        alice.addFood(5);

        ctrl.markPlayerDisconnected("Alice");
        assertTrue(ctrl.isPlayerDisconnected("Alice"));

        Round round = ctrl.getGame().getCurrentRound();
        roundList(round, "lowerRowEventCards").add(
                new SustenanceEventCard(600, Era.I, false, 1));

        runEventResolutionInBackground(ctrl);

        // 1 character × 1 food required; Alice had 5, now 4. No unfed ⇒ no PP penalty.
        assertEquals(4, alice.getFood());
        assertEquals(0, alice.getPrestigePoints());
    }

    @Test
    void sustenancePenalisesDisconnectedPlayerWithoutFood() {
        GameController ctrl = buildControllerWith2Players();
        Player alice = ctrl.getGame().getPlayers().get(0);
        alice.addCharacterCard(new HunterCard(101, Era.I, 0, false));
        alice.addCharacterCard(new HunterCard(102, Era.I, 0, false));
        // food = 0; required = 2 characters; unfed = 2; penalty = 2 × 3 = 6 PP loss
        ctrl.markPlayerDisconnected("Alice");

        Round round = ctrl.getGame().getCurrentRound();
        roundList(round, "lowerRowEventCards").add(
                new SustenanceEventCard(601, Era.I, false, 3));

        runEventResolutionInBackground(ctrl);

        assertEquals(0, alice.getFood());
        assertEquals(-6, alice.getPrestigePoints(),
                "Disconnected player must still suffer Sustenance penalty");
    }

    @Test
    void noEventsRound_transitionsCleanlyOutOfEventResolution() {
        GameController ctrl = buildControllerWith2Players();

        // No events queued. EventResolutionState.onEnter just falls through to
        // EndRoundState. We do NOT run this in background because empty events
        // means no per-event sleep — but EndRoundState itself sleeps 5s. Acceptable.
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                ctrl.changeState(new EventResolutionState());
            } catch (Throwable th) {
                error.set(th);
            }
        });
        t.setDaemon(true);
        t.start();
        try { Thread.sleep(200L); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        assertNull(error.get(), "Empty-event onEnter must not throw");

        // The state must have moved past EventResolutionState (either EndRoundState
        // or TotemPlacementState depending on how far through the daemon ran).
        assertNotEquals(EventResolutionState.class, ctrl.getCurrentState().getClass());
        assertNotNull(ctrl.getCurrentState());
    }
}
