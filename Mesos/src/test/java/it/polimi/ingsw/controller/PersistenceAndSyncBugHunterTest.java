package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.controller.state.EventResolutionState;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.effect.EndPPEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.RoundPhase;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.network.protocol.clientbound.GameSnapshot;
import it.polimi.ingsw.network.protocol.clientbound.GameSyncMessage;
import it.polimi.ingsw.network.protocol.clientbound.PlayerSnapshot;
import it.polimi.ingsw.network.protocol.clientbound.RoundEndedMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug-hunter tests for areas not yet covered:
 *   - Persistence layer (notifier wiring after restore)
 *   - Client model out-of-order message handling
 *   - Disconnection mid-event-resolution
 *   - ExtraCardPickAvailable information preservation
 */
class PersistenceAndSyncBugHunterTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static <T> T deepCopyViaSerialization(T obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(baos)) {
            out.writeObject(obj);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            return (T) in.readObject();
        }
    }

    private static Game minimalGame() {
        List<Player> players = new ArrayList<>(List.of(
                new Player("Alice", new Totem(TotemColor.RED,  "OFFER")),
                new Player("Bob",   new Totem(TotemColor.BLUE, "OFFER"))
        ));
        Game game = new Game(players, 1, null, Era.I, LocalDateTime.now(),
                BoardFactory.createBoard(2),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        for (Player p : players) p.setGame(game);
        Round round = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>());
        game.setCurrentRound(round);
        return game;
    }

    private static Object readField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║ P1 — Persistence: Game.notifier is not re-wired after restore        ║
    // ║                                                                      ║
    // ║ Symptom: after a server crash + load, food/PP changes emitted by    ║
    // ║ the model (Game.notifyListeners) are silently dropped because       ║
    // ║ Game.notifier is transient and never re-set.                         ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Game.notifier is transient.  After deserialization it must be null,
     * confirming the design problem: nobody re-wires it automatically.
     */
    @Test
    void gameNotifierIsLostAfterSerializationRoundTrip() throws Exception {
        Game game = minimalGame();
        AtomicInteger fired = new AtomicInteger(0);
        game.setNotifier(event -> fired.incrementAndGet());

        // Sanity: pre-roundtrip the notifier works.
        game.notifyListeners(l -> {});
        assertEquals(1, fired.get(), "Sanity: notifier must work pre-roundtrip");

        Game restored = deepCopyViaSerialization(game);

        Object notifier = readField(restored, "notifier");
        assertNull(notifier,
                "After serialization round-trip, Game.notifier (transient) is null. " +
                "If the loader does not re-wire it (via game.setNotifier), every " +
                "model-emitted event after restore is silently lost.");
    }

    /**
     * End-to-end regression: after Persistence.load, model-emitted food
     * changes MUST reach the listeners.  Persistence.load re-wires
     * Game.notifier explicitly to fix the transient-field gap.
     */
    @Test
    void afterPersistenceLoadModelEventsArePropagatedToListeners(@TempDir Path tempDir) throws Exception {
        // 1) Start a game so notifier is wired to the controller.
        GameController controller = new GameController();
        controller.onMessageReceived("Alice", new JoinGameAction(new Totem(TotemColor.RED,  "OFFER")));
        controller.onMessageReceived("Bob",   new JoinGameAction(new Totem(TotemColor.BLUE, "OFFER")));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));
        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onMessageReceived("Bob",   new ReadyAction(true));
        assertNotNull(controller.getGame(), "Sanity: game must be started");

        // 2) Save + load.
        Path target = tempDir.resolve("savestate.bin");
        Persistence.save(controller, target);
        Optional<GameController> loaded = Persistence.load(target);
        assertTrue(loaded.isPresent());
        GameController restored = loaded.get();

        // 3) Attach a listener AFTER restore.
        AtomicInteger foodEvents = new AtomicInteger(0);
        restored.addListener(new SilentListener() {
            @Override public void onFoodChanged(String n, int v, int d) { foodEvents.incrementAndGet(); }
        });

        // 4) Emit a food change directly via the model (simulating what
        //    Game.takeBuildingCard or Round.placeTotemOrderForPlayer would do).
        Player alice = restored.getGame().getPlayers().get(0);
        alice.addFood(3);
        restored.getGame().notifyListeners(
                l -> l.onFoodChanged(alice.getNickname(), alice.getFood(), 3));

        // 5) Persistence.load re-wires Game.notifier → exactly 1 event must fire.
        assertEquals(1, foodEvents.get(),
                "Persistence.load must re-wire Game.notifier so model-emitted " +
                "events still reach controller listeners after restore.");
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║ P2 — ClientModel: out-of-order RoundEnded vs GameSync                ║
    // ║                                                                      ║
    // ║ In the normal flow (EndRoundState), RoundEnded is emitted BEFORE     ║
    // ║ GameSync, both via the same TCP connection so order is preserved.    ║
    // ║ But the apply* methods are individually fragile to a swap — verify.  ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Normal in-order flow: RoundEnded first, then GameSync.
     * Final state: roundNumber = new round, lastFinishedRound = old round.
     */
    @Test
    void normalOrderRoundEndedThenGameSyncProducesCorrectState() {
        ClientModel m = new ClientModel();
        // Simulate end of round 1, transition to round 2.
        m.applyRoundEnded(new RoundEndedMessage(1));
        m.applyGameSync(new GameSyncMessage(snapshotForRound(2)));

        assertEquals(2, m.getRoundNumber(), "Expected current round = 2 after sync");
        assertEquals(1, m.getLastFinishedRound(), "Expected last finished = 1");
    }

    /**
     * Reverse order: GameSync first, then RoundEnded (degenerate case, e.g.
     * if a future protocol change splits emission across channels).  Verify
     * what happens — currently roundNumber regresses.  Documents the fragility.
     */
    @Test
    void reverseOrderGameSyncThenRoundEndedRegressesRoundNumber() {
        ClientModel m = new ClientModel();
        m.applyGameSync(new GameSyncMessage(snapshotForRound(2)));
        m.applyRoundEnded(new RoundEndedMessage(1));

        // BUG/Fragility: applyRoundEnded unconditionally writes m.getRoundNumber().
        // After this swap, the client reverts to roundNumber = 1.
        assertEquals(1, m.getRoundNumber(),
                "applyRoundEnded clobbers roundNumber unconditionally — fragile " +
                "if message order is ever swapped.  A safer impl would only " +
                "update roundNumber if it is currently behind.");
        assertEquals(1, m.getLastFinishedRound());
    }

    private GameSnapshot snapshotForRound(int round) {
        return new GameSnapshot(
                List.of(new PlayerSnapshot("Alice", TotemColor.RED, 0, 0, List.of(), List.of())),
                List.of(), List.of(), List.of(), List.of(),
                Era.I, round, Map.of(), List.of("Alice"));
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║ P3 — ClientModel: ExtraCardPickAvailable info is dropped             ║
    // ║                                                                      ║
    // ║ ClientModel.applyExtraCardPickAvailable only calls notifyChanged()   ║
    // ║ — it does NOT store the nickname or the number of remaining picks.   ║
    // ║ The TUI cannot tell the user "you have N extra picks from your       ║
    // ║ building, take from upper row".                                      ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    @Test
    void clientModelDoesNotExposeExtraCardPickAvailableInformation() {
        // Inspect ClientModel's public API: there is no getter that returns
        // a non-empty extra-pick-available signal.  This is a feature gap.
        // We assert by reflection that no field of type int/String stores
        // it (best-effort heuristic).
        ClientModel m = new ClientModel();

        boolean hasExtraField = false;
        for (Field f : ClientModel.class.getDeclaredFields()) {
            String name = f.getName().toLowerCase();
            if (name.contains("extra") || name.contains("pick")) {
                hasExtraField = true;
                break;
            }
        }
        assertFalse(hasExtraField,
                "ClientModel currently has no field tracking the extra-pick " +
                "info — confirms the feature gap.  If you add one, this " +
                "assertion should be inverted.");
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║ P4 — EventResolutionState marks disconnections during the phase      ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    @Test
    void eventResolutionStateMarksPlayerDisconnectedWhenNotified() {
        // Build a controller with a game so containsGamePlayer returns true.
        GameController controller = new GameController();
        controller.onMessageReceived("Alice", new JoinGameAction(new Totem(TotemColor.RED,  "OFFER")));
        controller.onMessageReceived("Bob",   new JoinGameAction(new Totem(TotemColor.BLUE, "OFFER")));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));
        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onMessageReceived("Bob",   new ReadyAction(true));

        EventResolutionState state = new EventResolutionState();
        // Don't call onEnter (it blocks 5s+).  Just exercise the disconnect hook.
        assertFalse(controller.isPlayerDisconnected("Alice"));

        state.onPlayerDisconnected(controller, "Alice");

        assertTrue(controller.isPlayerDisconnected("Alice"),
                "EventResolutionState.onPlayerDisconnected must mark the player " +
                "disconnected so subsequent states (EndRound, TotemPlacement) " +
                "skip them correctly");
    }

    // ── Mock listener (overrides only food) ──────────────────────────────────

    static class SilentListener implements GameListener {
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
    }
}
