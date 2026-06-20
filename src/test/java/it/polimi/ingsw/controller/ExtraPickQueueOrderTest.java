package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.*;
import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.TurnOrderTile;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.effect.ExtraCardUpperRowEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.EventResolvedMessage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the extra-pick queue (triggered by the
 * ExtraCardUpperRowEffect building) is ordered by the turn-order
 * tile reached after action resolution, not by lobby join order.
 *
 * Setup:
 *   - Alice joins first, Bob joins second  → join order = [Alice, Bob]
 *   - Both players receive an ExtraCardUpperRowEffect building
 *   - Tile assignment is arranged so that Bob always lands on tile B
 *     (earlier letter) and Alice lands on tile C (later letter)
 *   - Tile B resolves before tile C, so Bob returns his totem first →
 *     turn-order tile after round = [Bob, Alice]  (reverse of join order)
 *
 * The extra-pick notifications must fire in turn-order-tile order
 * ([Bob, Alice]), not in join order ([Alice, Bob]) — the two orders are
 * deliberately set up to differ so the distinction is provable.
 */
class ExtraPickQueueOrderTest {

    @Test
    void extraPickOrderFollowsTurnOrderTileAfterRound() {
        GameController controller = createTwoPlayerGame();
        Game game = controller.getGame();
        Round round = game.getCurrentRound();

        // Precondition: join order is Alice=0, Bob=1.
        List<String> joinOrder = game.getPlayers().stream()
                .map(Player::getNickname)
                .collect(Collectors.toList());
        assertEquals(List.of("Alice", "Bob"), joinOrder,
                "Join order must be [Alice, Bob]");

        // Give both players an extra-pick building.
        findPlayer(game, "Alice").addBuildingCard(
                new BuildingCard(996, Era.I, 0, new ExtraCardUpperRowEffect(), 0));
        findPlayer(game, "Bob").addBuildingCard(
                new BuildingCard(997, Era.I, 0, new ExtraCardUpperRowEffect(), 0));

        // Clear lower row so any lower-row tile budget is auto-skipped.
        clearLowerRow(round);

        // Capture the order in which onExtraCardPickAvailable fires.
        List<String> extraPickOrder = new ArrayList<>();
        controller.addListener(new SilentListener() {
            @Override
            public void onExtraCardPickAvailable(String nickname, int picks) {
                extraPickOrder.add(nickname);
            }
        });

        // Arrange tiles so Bob always ends up on tile B (earlier letter) and
        // Alice on tile C (later letter), regardless of initial turn order.
        // After action resolution: Bob returns totem first → turn-order tile = [Bob, Alice].
        String firstTurnPlayer = controller.getCurrentTurnPlayerNickname();
        if ("Alice".equals(firstTurnPlayer)) {
            controller.onMessageReceived("Alice", new PlaceTotemAction('C'));
            controller.onMessageReceived("Bob",   new PlaceTotemAction('B'));
        } else {
            controller.onMessageReceived("Bob",   new PlaceTotemAction('B'));
            controller.onMessageReceived("Alice", new PlaceTotemAction('C'));
        }

        // Action resolution is now running.
        // Bob (tile B = lower-row pick, lower row empty) is auto-skipped.
        // Alice (tile C = upper-row pick) must consume her single pick.
        String upperCardId = String.valueOf(round.getUpperRowTribute().get(0).getId());
        controller.onMessageReceived("Alice", new PickCardAction(upperCardId, true));

        // Alice's budget is now depleted → placeTotemOrderForPlayer(Alice) →
        // startExtraPickPhase fires → first onExtraCardPickAvailable captured synchronously.
        assertEquals(1, extraPickOrder.size(),
                "Exactly one onExtraCardPickAvailable should have fired after Alice's pick");

        // The queue follows the turn-order tile: Bob is in slot 1
        // (resolved tile B first), so Bob gets the first extra-pick notification.
        // Advance the queue with Bob's skip to capture Alice's notification.
        controller.onMessageReceived("Bob", new SkipExtraPickAction());

        assertEquals(2, extraPickOrder.size(),
                "Two onExtraCardPickAvailable notifications should have fired in total");

        // ── turn-order tile after action resolution ──────────────────────────
        // Bob resolved tile B first → slot 1; Alice resolved tile C → slot 2.
        List<String> turnOrderAfterRound = turnOrderFromTile(game);
        assertEquals(List.of("Bob", "Alice"), turnOrderAfterRound,
                "Turn-order tile must read [Bob, Alice] after this round's action resolution");

        // ── core assertion: extra-pick order == turn-order tile order ────────
        assertEquals(turnOrderAfterRound, extraPickOrder,
                "After fix: extra-pick order must match turn-order tile " + turnOrderAfterRound);

        // Extra-pick order no longer matches join order — the incoherence is resolved.
        assertNotEquals(joinOrder, extraPickOrder,
                "Extra-pick order [" + extraPickOrder + "] must differ from join order " + joinOrder);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private GameController createTwoPlayerGame() {
        GameController controller = new GameController();
        controller.onMessageReceived("Alice", new JoinGameAction(new Totem(TotemColor.RED,  "OFFER_TRACK")));
        controller.onMessageReceived("Bob",   new JoinGameAction(new Totem(TotemColor.BLUE, "OFFER_TRACK")));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));
        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onMessageReceived("Bob",   new ReadyAction(true));
        return controller;
    }

    private Player findPlayer(Game game, String nickname) {
        return game.getPlayers().stream()
                .filter(p -> p.getNickname().equals(nickname))
                .findFirst()
                .orElseThrow();
    }

    private void clearLowerRow(Round round) {
        roundField(round, "lowerRowTribute").clear();
        roundField(round, "lowerRowEventCards").clear();
        roundField(round, "lowerRowBuildings").clear();
    }

    @SuppressWarnings("unchecked")
    private List<Object> roundField(Round round, String fieldName) {
        try {
            Field f = Round.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            return (List<Object>) f.get(round);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot access Round." + fieldName, e);
        }
    }

    private List<String> turnOrderFromTile(Game game) {
        Map<Totem, String> totemToNickname = new HashMap<>();
        for (Player p : game.getPlayers()) {
            totemToNickname.put(p.getTotem(), p.getNickname());
        }
        return game.getBoard().getTurnOrderTile().getTurnOrder().stream()
                .map(totemToNickname::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ── silent listener ───────────────────────────────────────────────────────

    private static class SilentListener implements GameListener {
        @Override public void onPlayerJoined(String nickname) {}
        @Override public void onPlayersCountSet(int count) {}
        @Override public void onPlayerDisconnected(String nickname) {}
        @Override public void onPlayerReconnected(String nickname) {}
        @Override public void onGameSuspended() {}
        @Override public void onGameResumed() {}
        @Override public void onGameStarted() {}
        @Override public void onStateChanged(GameState newState) {}
        @Override public void onTotemPlaced(String nickname, char tileId) {}
        @Override public void onFoodChanged(String nickname, int newAmount, int delta) {}
        @Override public void onPointsChanged(String nickname, int newAmount, int delta) {}
        @Override public void onEraChanged(Era newEra) {}
        @Override public void onCharacterCardTaken(String nickname, String cardId, boolean fromUpperRow) {}
        @Override public void onBuildingCardTaken(String nickname, String cardId, boolean fromUpperRow) {}
        @Override public void onRoundEnded(int roundNumber) {}
        @Override public void onGameEnded(Player winner, Map<Player, Integer> finalScores) {}
        @Override public void onExtraCardPickAvailable(String nickname, int picks) {}
    }
}
