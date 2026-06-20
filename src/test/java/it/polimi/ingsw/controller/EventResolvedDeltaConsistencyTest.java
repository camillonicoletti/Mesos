package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.controller.state.EventResolutionState;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.controller.state.LobbyState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.HuntEventCard;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.EventResolvedMessage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Delta-consistency tests for EventResolvedMessage.
 *
 * Verifies that for every resolved event the message carries foodDelta and
 * pointsDelta matching the model delta, and newFood/newPoints matching the
 * player's state after resolution. To keep the test fast, each test injects
 * a single non-Sustenance event and inspects the captured listener message
 * immediately after resolution, bypassing the per-event 5-second sleep used
 * in EventResolutionState.
 */
class EventResolvedDeltaConsistencyTest {

    static class CapturingListener implements GameListener {
        EventResolvedMessage lastEvent;

        @Override public void onPlayerJoined(String n) {}
        @Override public void onPlayersCountSet(int c) {}
        @Override public void onPlayerDisconnected(String n) {}
        @Override public void onPlayerReconnected(String n) {}
        @Override public void onGameSuspended() {}
        @Override public void onGameResumed() {}
        @Override public void onGameStarted() {}
        @Override public void onStateChanged(GameState s) {}
        @Override public void onTotemPlaced(String n, char t) {}
        @Override public void onFoodChanged(String n, int v, int d) {}
        @Override public void onPointsChanged(String n, int v, int d) {}
        @Override public void onEraChanged(Era e) {}
        @Override public void onCharacterCardTaken(String n, String id, boolean upper) {}
        @Override public void onBuildingCardTaken(String n, String id, boolean upper) {}
        @Override public void onRoundEnded(int r) {}
        @Override public void onGameEnded(Player w, Map<Player, Integer> s) {}
        @Override public void onExtraCardPickAvailable(String n, int p) {}

        @Override
        public void onEventResolved(int cardId, String title, String description,
                                    List<EventResolvedMessage.PlayerEffect> effects) {
            lastEvent = new EventResolvedMessage(cardId, title, description, effects);
        }
    }

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
     * Builds a minimal Game with two players, attaches the controller's
     * notifier, and gives Player 0 some hunters so a HuntEvent has a non-zero
     * effect.  Returns the controller already wired to the listener.
     */
    private GameController buildController(CapturingListener listener) {
        List<Player> players = new ArrayList<>(Arrays.asList(
                new Player("Alice", new Totem(TotemColor.RED,  "OFFER")),
                new Player("Bob",   new Totem(TotemColor.BLUE, "OFFER"))
        ));
        List<Card> tribute = new ArrayList<>();
        Game game = new Game(players, 1, null, Era.I, java.time.LocalDateTime.now(),
                BoardFactory.createBoard(2),
                tribute,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        Round round = new Round(1, game, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>());
        game.setCurrentRound(round);
        for (Player p : players) p.setGame(game);

        GameController ctrl = new GameController();
        // Use reflection to inject the game (controller has no public setter for game in tests)
        try {
            Field gf = GameController.class.getDeclaredField("game");
            gf.setAccessible(true);
            gf.set(ctrl, game);
            // Also attach the notifier so Game's notifyListeners reaches our listener
            game.setNotifier(ev -> ctrl.notifyListeners(ev));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }

        ctrl.addListener(listener);
        return ctrl;
    }

    /**
     * HuntEvent — Alice has 2 Hunters, foodPerHunter=1, ppPerHunter=1.
     * Expected: foodDelta=2, pointsDelta=2, newFood=alice.food+2, newPoints=2.
     */
    @Test
    void huntEventReportsCorrectDeltasForEachPlayer() {
        CapturingListener listener = new CapturingListener();
        GameController ctrl = buildController(listener);

        Player alice = ctrl.getGame().getPlayers().get(0);
        Player bob   = ctrl.getGame().getPlayers().get(1);

        alice.addCharacterCard(new HunterCard(101, Era.I, 0, false));
        alice.addCharacterCard(new HunterCard(102, Era.I, 0, false));
        // Bob: no hunters → no effect

        int aliceFoodBefore = alice.getFood();
        int alicePPBefore   = alice.getPrestigePoints();
        int bobFoodBefore   = bob.getFood();
        int bobPPBefore     = bob.getPrestigePoints();

        // Inject a single HuntEventCard into the lower row events so
        // getOrderedEventsForResolution returns it.
        Round round = ctrl.getGame().getCurrentRound();
        roundList(round, "lowerRowEventCards").add(
                new HuntEventCard(500, Era.I, false, 1, 1));

        // EventResolutionState.onEnter blocks for 5s after firing the event
        // (per-event display delay).  We trigger it in a daemon thread and
        // check the captured message after 300ms — by then onEventResolved
        // has fired but the sleep is still running.  The daemon thread will
        // finish in the background or be reaped by the JVM.
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

        try { Thread.sleep(300L); } catch (InterruptedException e) { fail("interrupted"); }

        assertNull(error.get(), "EventResolutionState onEnter must not throw");
        assertNotNull(listener.lastEvent, "Listener must have captured a HuntEvent");

        EventResolvedMessage msg = listener.lastEvent;
        assertEquals("HUNT", msg.getTitle());

        // Find Alice's effect
        EventResolvedMessage.PlayerEffect aliceEff = msg.getEffects().stream()
                .filter(e -> e.getNickname().equals("Alice"))
                .findFirst().orElseThrow();
        EventResolvedMessage.PlayerEffect bobEff = msg.getEffects().stream()
                .filter(e -> e.getNickname().equals("Bob"))
                .findFirst().orElseThrow();

        assertEquals(2, aliceEff.getFoodDelta(),   "Alice foodDelta must equal 2 (2 hunters × 1 food)");
        assertEquals(2, aliceEff.getPointsDelta(), "Alice pointsDelta must equal 2 (2 hunters × 1 PP)");
        assertEquals(aliceFoodBefore + 2, aliceEff.getNewFood(),
                "Alice newFood must equal foodBefore + delta");
        assertEquals(alicePPBefore + 2, aliceEff.getNewPoints(),
                "Alice newPoints must equal pointsBefore + delta");

        assertEquals(0, bobEff.getFoodDelta(),   "Bob foodDelta must be 0 (no hunters)");
        assertEquals(0, bobEff.getPointsDelta(), "Bob pointsDelta must be 0");
        assertEquals(bobFoodBefore, bobEff.getNewFood());
        assertEquals(bobPPBefore,   bobEff.getNewPoints());
    }
}
