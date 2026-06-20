package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Model aggregate.
 *
 * Verifies robustness of the model layer against illegal mutations and boundary inputs.
 */
class ModelRobustnessTest {

    /**
     * Verifies that adding a negative amount of food is rejected.
     * Food must never be reduced through addFood; spending must go through spendFood.
     */
    @Test
    void addNegativeFoodIsRejectedAndDoesNotMutatePlayer() {
        Player player = createPlayer("Alice", TotemColor.RED);
        player.addFood(3);

        assertThrows(IllegalArgumentException.class, () -> player.addFood(-1));

        assertEquals(3, player.getFood(),
                "addFood must not be usable to reduce food.");
    }

    /**
     * Verifies that removing a negative amount of prestige points is rejected.
     * Otherwise removePrestigePoints could be used to increase prestige points.
     */
    @Test
    void removeNegativePrestigePointsIsRejectedAndDoesNotMutatePlayer() {
        Player player = createPlayer("Alice", TotemColor.RED);
        player.addPrestigePoints(5);

        assertThrows(IllegalArgumentException.class, () -> player.removePrestigePoints(-2));

        assertEquals(5, player.getPrestigePoints(),
                "removePrestigePoints must not be usable to increase prestige points.");
    }

    /**
     * Verifies that spending zero food is allowed and does not mutate the player.
     * This is useful because discounts can reduce a building cost to zero.
     */
    @Test
    void spendZeroFoodIsAllowedAndDoesNotMutatePlayer() {
        Player player = createPlayer("Alice", TotemColor.RED);
        player.addFood(4);

        assertDoesNotThrow(() -> player.spendFood(0));

        assertEquals(4, player.getFood());
    }

    /**
     * Verifies that spending more food than available is rejected and does not make food negative.
     */
    @Test
    void spendingMoreFoodThanAvailableIsRejectedAndDoesNotMakeFoodNegative() {
        Player player = createPlayer("Alice", TotemColor.RED);
        player.addFood(2);

        assertThrows(IllegalStateException.class, () -> player.spendFood(3));

        assertEquals(2, player.getFood());
        assertTrue(player.getFood() >= 0);
    }

    /**
     * Verifies that the final scores map returned by Game is a defensive copy.
     * External code must not be able to corrupt stored final scores.
     */
    @Test
    void finalScoresGetterReturnsDefensiveCopy() {
        Player alice = createPlayer("Alice", TotemColor.RED);
        Game game = createGame(alice);

        alice.addPrestigePoints(10);
        game.calculateFinalScores();

        Map<Player, Integer> exposedScores = game.getFinalScores();
        exposedScores.put(alice, 999);

        assertEquals(10, game.getFinalScore(alice),
                "Mutating the returned final scores map must not affect the game internal state.");
    }

    /**
     * Verifies that drawing from an empty tribute deck is safe and returns null.
     * This protects refill logic from crashing at the end of the game.
     */
    @Test
    void drawingFromEmptyTributeDeckReturnsNullAndKeepsDeckEmpty() {
        Game game = createGameWithDeck(new ArrayList<>(), createPlayer("Alice", TotemColor.RED));

        assertNull(game.drawTribeCard());
        assertTrue(game.getTributeDeck().isEmpty());
    }

    /**
     * Verifies that drawing a tribe card removes exactly the first card from the deck.
     * This protects the deterministic order of deck consumption.
     */
    @Test
    void drawingTribeCardRemovesOnlyFirstCard() {
        HunterCard first = new HunterCard(1, Era.I, 2, false);
        HunterCard second = new HunterCard(2, Era.I, 2, false);
        Game game = createGameWithDeck(new ArrayList<>(List.of(first, second)), createPlayer("Alice", TotemColor.RED));

        Card drawn = game.drawTribeCard();

        assertSame(first, drawn);
        assertEquals(1, game.getTributeDeck().size());
        assertSame(second, game.getTributeDeck().get(0));
    }

    private Player createPlayer(String nickname, TotemColor color) {
        return new Player(nickname, new Totem(color, "OFFER_TRACK"));
    }

    private Game createGame(Player... players) {
        return createGameWithDeck(new ArrayList<>(), players);
    }

    private Game createGameWithDeck(List<Card> tributeDeck, Player... players) {
        return new Game(
                new ArrayList<>(List.of(players)),
                1,
                null,
                Era.I,
                LocalDateTime.now(),
                (Board) null,
                tributeDeck,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );
    }
}