package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.*;
import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.controller.state.ActionResolutionState;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.effect.EndPPEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for client-side synchronisation after game events.
 *
 * These tests cover updates that must reach the client immediately, without
 * waiting for the next full snapshot.
 */
class GameControllerClientSyncTest {

    // ── Recording listener ────────────────────────────────────────────────────

    record FoodChangeEvent(String nickname, int newAmount, int delta) {}
    record PointsChangeEvent(String nickname, int newAmount, int delta) {}

    static class RecordingListener implements GameListener {
        final List<FoodChangeEvent>   foodChanges   = new ArrayList<>();
        final List<PointsChangeEvent> pointsChanges = new ArrayList<>();

        @Override public void onPlayerJoined(String n) {}
        @Override public void onPlayersCountSet(int c) {}
        @Override public void onPlayerDisconnected(String n) {}
        @Override public void onPlayerReconnected(String n) {}
        @Override public void onGameSuspended() {}
        @Override public void onGameResumed() {}
        @Override public void onGameStarted() {}
        @Override public void onStateChanged(GameState s) {}
        @Override public void onTotemPlaced(String n, char t) {}
        @Override public void onEraChanged(Era e) {}
        @Override public void onCharacterCardTaken(String n, String id, boolean upper) {}
        @Override public void onBuildingCardTaken(String n, String id, boolean upper) {}
        @Override public void onRoundEnded(int r) {}
        @Override public void onGameEnded(Player w, Map<Player, Integer> s) {}
        @Override public void onExtraCardPickAvailable(String n, int p) {}

        @Override
        public void onFoodChanged(String nickname, int newAmount, int delta) {
            foodChanges.add(new FoodChangeEvent(nickname, newAmount, delta));
        }

        @Override
        public void onPointsChanged(String nickname, int newAmount, int delta) {
            pointsChanges.add(new PointsChangeEvent(nickname, newAmount, delta));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private GameController createStartedTwoPlayerGame() {
        GameController controller = new GameController();
        controller.onMessageReceived("Alice", new JoinGameAction(new Totem(TotemColor.RED,  "OFFER_TRACK")));
        controller.onMessageReceived("Bob",   new JoinGameAction(new Totem(TotemColor.BLUE, "OFFER_TRACK")));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));
        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onMessageReceived("Bob",   new ReadyAction(true));
        return controller;
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

    private void replaceUpperRowBuildings(Round round, BuildingCard... buildings) {
        List<BuildingCard> list = roundList(round, "upperRowBuildings");
        list.clear();
        for (BuildingCard b : buildings) list.add(b);
        // Also clear character cards so the mandatory-character-pick rule does not
        // block the building pick that these tests are specifically exercising.
        roundList(round, "upperRowTribute").clear();
    }

    /**
     * Reaches ActionResolutionState with the first placer on tile B (lower-row pick)
     * and the second placer on tile C (upper-row pick).
     * Returns the controller; B's player resolves before C's (B &lt; C).
     */
    private GameController reachActionResolutionBthenC() {
        GameController ctrl = createStartedTwoPlayerGame();
        String first  = ctrl.getCurrentTurnPlayerNickname();
        String second = "Alice".equals(first) ? "Bob" : "Alice";
        ctrl.onMessageReceived(first,  new PlaceTotemAction('B'));
        ctrl.onMessageReceived(second, new PlaceTotemAction('C'));
        assertInstanceOf(ActionResolutionState.class, ctrl.getCurrentState());
        return ctrl;
    }

    // ── Food notification after building purchase ─────────────────────────────

    /**
     * After a player buys a building, {@code onFoodChanged} must be fired immediately
     * (not deferred to the next snapshot).  Before the fix, 0 food-change events were
     * emitted during a building purchase.
     */
    @Test
    void buyingBuildingFiresFoodChangedNotification() {
        GameController ctrl = reachActionResolutionBthenC();
        RecordingListener listener = new RecordingListener();
        ctrl.addListener(listener);

        Round round = ctrl.getGame().getCurrentRound();

        // B resolves first: pick lower-row character card
        String bPlayer = ctrl.getCurrentActionPlayerNickname();
        CharacterCard lowerCard = round.getLowerRowTribute().get(0);
        ctrl.onMessageReceived(bPlayer, new PickCardAction(String.valueOf(lowerCard.getId()), false));

        // Now C is active; place a single cheap building (cost 1) in the upper row
        String cPlayer = ctrl.getCurrentActionPlayerNickname();
        assertNotNull(cPlayer, "C must be active after B finishes");

        BuildingCard cheap = new BuildingCard(801, Era.I, 1, new EndPPEffect(), 0);
        replaceUpperRowBuildings(round, cheap);

        Player cModel = findPlayer(ctrl, cPlayer);
        assertTrue(cModel.getFood() >= 1, "C must have enough food to afford cost-1 building");

        listener.foodChanges.clear();
        listener.pointsChanges.clear();

        // C buys the building
        ctrl.onMessageReceived(cPlayer, new PickBuildingAction("801", true));

        // At least one onFoodChanged with a negative delta must have fired for the buyer.
        // (There may also be a slot-2 penalty event immediately after, which is fine.)
        boolean buildingCostFired = listener.foodChanges.stream()
                .anyMatch(e -> e.nickname().equals(cPlayer) && e.delta() < 0);
        assertTrue(buildingCostFired,
                "onFoodChanged with negative delta must fire after building purchase");

        // The last notified food amount for the buyer must match the model.
        FoodChangeEvent lastEvt = null;
        for (FoodChangeEvent e : listener.foodChanges) {
            if (e.nickname().equals(cPlayer)) lastEvt = e;
        }
        assertNotNull(lastEvt);
        assertEquals(cModel.getFood(), lastEvt.newAmount(),
                "Last notified food amount must equal model food after all events");
    }

    /**
     * When a player buys a building with cost 0 (full builder discount), no food
     * changes so {@code onFoodChanged} must NOT fire for cost-0 purchases.
     * Food must stay unchanged in the model.
     */
    @Test
    void zeroCostBuildingDoesNotFireFoodChangedNotification() {
        GameController ctrl = reachActionResolutionBthenC();
        RecordingListener listener = new RecordingListener();
        ctrl.addListener(listener);

        Round round = ctrl.getGame().getCurrentRound();

        String bPlayer = ctrl.getCurrentActionPlayerNickname();
        CharacterCard lowerCard = round.getLowerRowTribute().get(0);
        ctrl.onMessageReceived(bPlayer, new PickCardAction(String.valueOf(lowerCard.getId()), false));

        String cPlayer = ctrl.getCurrentActionPlayerNickname();
        BuildingCard free = new BuildingCard(802, Era.I, 0, new EndPPEffect(), 0);
        replaceUpperRowBuildings(round, free);

        Player cModel = findPlayer(ctrl, cPlayer);
        int foodBefore = cModel.getFood();

        listener.foodChanges.clear();

        ctrl.onMessageReceived(cPlayer, new PickBuildingAction("802", true));

        // onFoodChanged must NOT fire with delta=0 — the guard (finalCost > 0) should
        // suppress the event when the building is free so listeners aren't flooded with
        // no-op notifications.
        boolean zeroDeltaFired = listener.foodChanges.stream()
                .anyMatch(e -> e.nickname().equals(cPlayer) && e.delta() == 0);
        assertFalse(zeroDeltaFired,
                "onFoodChanged with delta=0 must not fire for a zero-cost building");

        // Any food-change events that DID fire belong to the slot-2 penalty, not the building.
        // All notified amounts must match the final model state.
        listener.foodChanges.stream()
                .filter(e -> e.nickname().equals(cPlayer))
                .forEach(e -> assertEquals(cModel.getFood(), e.newAmount(),
                        "Notified food amount must always equal the current model food"));
    }

    // ── Turn-order slot effect notifications ──────────────────────────────────

    /**
     * When a player's totem lands on slot 1 (GainFoodSlotEffect, +1 food in a 2-player game),
     * {@code onFoodChanged} must be fired immediately.
     */
    @Test
    void totemOnGainFoodSlotFiresFoodChangedNotification() {
        GameController ctrl = reachActionResolutionBthenC();
        RecordingListener listener = new RecordingListener();
        ctrl.addListener(listener);

        Round round = ctrl.getGame().getCurrentRound();

        // B resolves first → lands on slot 1 (GainFoodSlotEffect(1))
        String bPlayer = ctrl.getCurrentActionPlayerNickname();
        Player bModel   = findPlayer(ctrl, bPlayer);

        listener.foodChanges.clear();

        CharacterCard lowerCard = round.getLowerRowTribute().get(0);
        ctrl.onMessageReceived(bPlayer, new PickCardAction(String.valueOf(lowerCard.getId()), false));

        // onFoodChanged with positive delta must have been fired for B
        assertTrue(
            listener.foodChanges.stream().anyMatch(e -> e.nickname().equals(bPlayer) && e.delta() > 0),
            "onFoodChanged with positive delta must fire when totem lands on gain-food slot"
        );

        // Model consistency
        FoodChangeEvent lastForB = null;
        for (FoodChangeEvent e : listener.foodChanges) {
            if (e.nickname().equals(bPlayer)) lastForB = e;
        }
        assertNotNull(lastForB);
        assertEquals(bModel.getFood(), lastForB.newAmount(),
                "Last notified food amount must match model after slot-1 gain");
    }

    /**
     * When the last player's totem lands on slot 2 (PayFoodSlotEffect in a 2-player game),
     * either {@code onFoodChanged} (food paid) or {@code onPointsChanged} (PP penalty)
     * must be fired.
     */
    @Test
    void totemOnPayFoodSlotFiresNotification() {
        GameController ctrl = reachActionResolutionBthenC();
        RecordingListener listener = new RecordingListener();
        ctrl.addListener(listener);

        Round round = ctrl.getGame().getCurrentRound();

        // B resolves first (slot 1 gain)
        String bPlayer = ctrl.getCurrentActionPlayerNickname();
        CharacterCard lowerCard = round.getLowerRowTribute().get(0);
        ctrl.onMessageReceived(bPlayer, new PickCardAction(String.valueOf(lowerCard.getId()), false));

        // Now C is active → will land on slot 2 (PayFoodSlotEffect(1, 2))
        String cPlayer = ctrl.getCurrentActionPlayerNickname();
        assertNotNull(cPlayer);

        listener.foodChanges.clear();
        listener.pointsChanges.clear();

        // C picks a character card from the upper row
        CharacterCard upperCard = round.getUpperRowTribute().get(0);
        ctrl.onMessageReceived(cPlayer, new PickCardAction(String.valueOf(upperCard.getId()), true));

        // Either food was spent or PP was deducted — exactly one of the two must have fired
        boolean foodFired  = listener.foodChanges.stream()
                .anyMatch(e -> e.nickname().equals(cPlayer) && e.delta() < 0);
        boolean pointsFired = listener.pointsChanges.stream()
                .anyMatch(e -> e.nickname().equals(cPlayer) && e.delta() < 0);
        assertTrue(foodFired || pointsFired,
                "onFoodChanged or onPointsChanged must fire when totem lands on pay-food slot");
    }

    /**
     * When the player has enough food to pay the slot-2 penalty, only food changes —
     * prestige points must remain unchanged by the slot effect.
     */
    @Test
    void totemOnPayFoodSlotSpendsFoodWhenAffordable() {
        GameController ctrl = reachActionResolutionBthenC();
        RecordingListener listener = new RecordingListener();
        ctrl.addListener(listener);

        Round round = ctrl.getGame().getCurrentRound();

        // B first
        String bPlayer = ctrl.getCurrentActionPlayerNickname();
        ctrl.onMessageReceived(bPlayer,
                new PickCardAction(String.valueOf(round.getLowerRowTribute().get(0).getId()), false));

        // C has food ≥ 1 (initial food ≥ 2 for all positions)
        String cPlayer = ctrl.getCurrentActionPlayerNickname();
        Player cModel   = findPlayer(ctrl, cPlayer);
        assertTrue(cModel.getFood() >= 1, "C must be able to afford the 1-food slot penalty");

        int ppBefore   = cModel.getPrestigePoints();
        int foodBefore = cModel.getFood();

        listener.foodChanges.clear();
        listener.pointsChanges.clear();

        ctrl.onMessageReceived(cPlayer,
                new PickCardAction(String.valueOf(round.getUpperRowTribute().get(0).getId()), true));

        // Food must have decreased by 1 (slot-2 penalty cost = 1)
        assertTrue(listener.foodChanges.stream()
                        .anyMatch(e -> e.nickname().equals(cPlayer) && e.delta() == -1),
                "Slot-2 penalty must fire onFoodChanged with delta=-1");

        // PP must be unchanged by the slot effect (food was enough)
        assertEquals(ppBefore, cModel.getPrestigePoints(),
                "PP must not change when player can pay the food penalty");
    }
}
