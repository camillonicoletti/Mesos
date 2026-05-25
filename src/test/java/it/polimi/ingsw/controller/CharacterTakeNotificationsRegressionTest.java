package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.controller.state.ActionResolutionState;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.effect.FullSetFoodEffect;
import it.polimi.ingsw.model.card.effect.InventorPairFoodEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug-hunter tests for client-side synchronisation when a character card is
 * picked.  Several model side-effects mutate food without firing
 * {@code onFoodChanged}, leaving the TUI showing stale food values.
 *
 * Suspect points:
 *   1. {@link Player#addCharacterCard(CharacterCard)} triggers
 *      {@link FullSetFoodEffect} / {@link InventorPairFoodEffect} which add food
 *      but never notify.
 *   2. {@code Game.applyCharacterImmediateEffects} adds food for hunters with
 *      the food icon without notifying.
 *
 * These tests confirm whether the bug exists and document the expected fix.
 */
class CharacterTakeNotificationsRegressionTest {

    record FoodEvent(String nick, int newAmount, int delta) {}

    static class RecordingListener implements GameListener {
        final List<FoodEvent> food = new ArrayList<>();

        @Override public void onPlayerJoined(String n) {}
        @Override public void onPlayersCountSet(int c) {}
        @Override public void onPlayerDisconnected(String n) {}
        @Override public void onPlayerReconnected(String n) {}
        @Override public void onGameSuspended() {}
        @Override public void onGameResumed() {}
        @Override public void onGameStarted() {}
        @Override public void onStateChanged(GameState s) {}
        @Override public void onTotemPlaced(String n, char t) {}
        @Override public void onPointsChanged(String n, int v, int d) {}
        @Override public void onEraChanged(Era e) {}
        @Override public void onCharacterCardTaken(String n, String id, boolean upper) {}
        @Override public void onBuildingCardTaken(String n, String id, boolean upper) {}
        @Override public void onRoundEnded(int r) {}
        @Override public void onGameEnded(Player w, Map<Player, Integer> s) {}
        @Override public void onExtraCardPickAvailable(String n, int p) {}

        @Override
        public void onFoodChanged(String nickname, int newAmount, int delta) {
            food.add(new FoodEvent(nickname, newAmount, delta));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private GameController createStartedTwoPlayerGame() {
        GameController c = new GameController();
        c.onMessageReceived("Alice", new JoinGameAction(new Totem(TotemColor.RED,  "OFFER")));
        c.onMessageReceived("Bob",   new JoinGameAction(new Totem(TotemColor.BLUE, "OFFER")));
        c.onMessageReceived("Alice", new SetPlayerCountAction(2));
        c.onMessageReceived("Alice", new ReadyAction(true));
        c.onMessageReceived("Bob",   new ReadyAction(true));
        return c;
    }

    private Player findPlayer(GameController ctrl, String nick) {
        return ctrl.getGame().getPlayers().stream()
                .filter(p -> p.getNickname().equals(nick))
                .findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> roundList(Round round, String field) {
        try {
            Field f = Round.class.getDeclaredField(field);
            f.setAccessible(true);
            return (List<T>) f.get(round);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot access Round." + field, e);
        }
    }

    /**
     * Drives the controller into ActionResolutionState with first placer on B
     * (lower row pick) and second on C (upper row pick).  Returns the
     * controller; B's player resolves first.
     */
    private GameController reachActionResolutionBthenC() {
        GameController c = createStartedTwoPlayerGame();
        String first  = c.getCurrentTurnPlayerNickname();
        String second = "Alice".equals(first) ? "Bob" : "Alice";
        c.onMessageReceived(first,  new PlaceTotemAction('B'));
        c.onMessageReceived(second, new PlaceTotemAction('C'));
        assertInstanceOf(ActionResolutionState.class, c.getCurrentState());
        return c;
    }

    // ── Hunter immediate food bonus notification ─────────────────────────────

    /**
     * When a Hunter with the "food icon" (Hunter that grants +1 food per Hunter
     * already in tribe) is picked, the player gains food via
     * {@code applyCharacterImmediateEffects} and the client must be notified.
     *
     * Reproduction:
     *   1. Give the player a Hunter card (no food icon) before the action.
     *   2. Replace lower row with a Hunter card that has hasFoodIcon = true.
     *   3. Pick that Hunter from lower row.
     *   4. Expect onFoodChanged with delta &gt; 0 (the hunter immediate effect).
     */
    @Test
    void hunterWithFoodIconPickedFiresFoodChanged() {
        GameController ctrl = reachActionResolutionBthenC();
        RecordingListener listener = new RecordingListener();
        ctrl.addListener(listener);

        String resolver = ctrl.getCurrentActionPlayerNickname();
        Player resolverPlayer = findPlayer(ctrl, resolver);
        Round round = ctrl.getGame().getCurrentRound();

        // Pre-seed with one Hunter (no food icon).  Hunter id 600 only counts
        // for Hunter set; the new Hunter must trigger the immediate +food effect.
        resolverPlayer.addCharacterCard(new HunterCard(600, Era.I, 0, false));

        // Replace lower row with a Hunter that has the food icon — picking it
        // should trigger applyCharacterImmediateEffects → addFood(huntersInTribe).
        List<CharacterCard> lower = roundList(round, "lowerRowTribute");
        lower.clear();
        lower.add(new HunterCard(601, Era.I, 1, true));

        listener.food.clear();
        int foodBefore = resolverPlayer.getFood();

        ctrl.onMessageReceived(resolver, new PickCardAction("601", false));

        // The slot-1 GainFoodSlotEffect also fires here, so we check that the
        // immediate hunter bonus is emitted as a separate event.
        long positiveEvents = listener.food.stream()
                .filter(e -> e.nick().equals(resolver) && e.delta() > 0)
                .count();

        // After this pick, the player's food should have risen by:
        //   +1 (slot-1 GainFoodSlotEffect)
        //   +2 (hunter immediate: 2 hunters in tribe AFTER add)
        // = +3 total.
        int expectedDelta = 3;
        int actualDelta   = resolverPlayer.getFood() - foodBefore;
        assertEquals(expectedDelta, actualDelta,
                "Sanity: food should have grown by 1 (slot) + 2 (hunter immediate)");

        assertEquals(2L, positiveEvents,
                "Expected TWO positive food events — slot-1 gain and hunter " +
                "immediate bonus.");
    }

    // ── FullSetFoodEffect notification ────────────────────────────────────────

    /**
     * When picking a character card that completes a "full set" of all 6
     * character types AND the player owns a building with FullSetFoodEffect,
     * the player gains +5 food via the effect's apply() inside
     * {@link Player#addCharacterCard(CharacterCard)}.  This food change is not
     * notified anywhere.
     */
    @Test
    void fullSetFoodEffectFromBuildingDoesNotFireFoodChanged() {
        GameController ctrl = reachActionResolutionBthenC();
        RecordingListener listener = new RecordingListener();
        ctrl.addListener(listener);

        String resolver = ctrl.getCurrentActionPlayerNickname();
        Player resolverPlayer = findPlayer(ctrl, resolver);
        Round round = ctrl.getGame().getCurrentRound();

        // Give the player 5 of the 6 character types and the FullSetFood building.
        resolverPlayer.addCharacterCard(new it.polimi.ingsw.model.card.ArtistCard(700, Era.I, 0));
        resolverPlayer.addCharacterCard(new it.polimi.ingsw.model.card.BuilderCard(701, Era.I, 0, 0, 1));
        resolverPlayer.addCharacterCard(new it.polimi.ingsw.model.card.GathererCard(702, Era.I, 0));
        resolverPlayer.addCharacterCard(new HunterCard(703, Era.I, 0, false));
        resolverPlayer.addCharacterCard(new it.polimi.ingsw.model.card.InventorCard(704, Era.I, 0,
                it.polimi.ingsw.model.enums.InventorIcons.INVENTION_1));
        resolverPlayer.addBuildingCard(new BuildingCard(705, Era.I, 0,
                new FullSetFoodEffect(), 0));

        // Replace lower row with a single Shaman — picking it completes the set.
        List<CharacterCard> lower = roundList(round, "lowerRowTribute");
        lower.clear();
        lower.add(new it.polimi.ingsw.model.card.ShamanCard(706, Era.I, 0, 0));

        listener.food.clear();
        int foodBefore = resolverPlayer.getFood();

        ctrl.onMessageReceived(resolver, new PickCardAction("706", false));

        // Food should have grown by:
        //   +1 from slot-1 GainFoodSlotEffect
        //   +5 from FullSetFoodEffect triggered in addCharacterCard
        // = +6
        int actualDelta = resolverPlayer.getFood() - foodBefore;
        assertEquals(6, actualDelta,
                "Sanity: full set food + slot gain should add 6 food");

        // Sum of all positive food deltas notified for the resolver
        int notifiedDelta = listener.food.stream()
                .filter(e -> e.nick().equals(resolver) && e.delta() > 0)
                .mapToInt(FoodEvent::delta)
                .sum();

        assertEquals(6, notifiedDelta,
                "Notified positive food delta must equal model delta. " +
                "If only +1 is reported, the FullSetFoodEffect is silently " +
                "adding food without notification.");
    }

    // ── InventorPairFoodEffect notification ──────────────────────────────────

    @Test
    void inventorPairFoodEffectFromBuildingFiresFoodChanged() {
        GameController ctrl = reachActionResolutionBthenC();
        RecordingListener listener = new RecordingListener();
        ctrl.addListener(listener);

        String resolver = ctrl.getCurrentActionPlayerNickname();
        Player resolverPlayer = findPlayer(ctrl, resolver);
        Round round = ctrl.getGame().getCurrentRound();

        // Pre-seed the player with one Inventor (FIRE) and the building that
        // grants +3 food per pair of matching inventors.
        resolverPlayer.addCharacterCard(new it.polimi.ingsw.model.card.InventorCard(800, Era.I, 0,
                it.polimi.ingsw.model.enums.InventorIcons.INVENTION_1));
        resolverPlayer.addBuildingCard(new BuildingCard(801, Era.I, 0,
                new InventorPairFoodEffect(), 0));

        // Replace lower row with another FIRE inventor — picking it forms a pair.
        List<CharacterCard> lower = roundList(round, "lowerRowTribute");
        lower.clear();
        lower.add(new it.polimi.ingsw.model.card.InventorCard(802, Era.I, 0,
                it.polimi.ingsw.model.enums.InventorIcons.INVENTION_1));

        listener.food.clear();
        int foodBefore = resolverPlayer.getFood();

        ctrl.onMessageReceived(resolver, new PickCardAction("802", false));

        // Food should have grown by:
        //   +1 from slot-1 GainFoodSlotEffect
        //   +3 from InventorPairFoodEffect (1 new pair × 3)
        // = +4
        int actualDelta = resolverPlayer.getFood() - foodBefore;
        assertEquals(4, actualDelta,
                "Sanity: inventor pair effect + slot gain should add 4 food");

        int notifiedDelta = listener.food.stream()
                .filter(e -> e.nick().equals(resolver) && e.delta() > 0)
                .mapToInt(FoodEvent::delta)
                .sum();

        assertEquals(4, notifiedDelta,
                "Notified positive food delta must equal model delta. " +
                "Otherwise InventorPairFoodEffect is silently adding food.");
    }
}
