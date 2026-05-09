package it.polimi.ingsw.model;

import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug hunter tests for food spending semantics after spendFood was made strict.
 */
class PlayerFoodBugHunterTest {

    /**
     * Generic food spending must be strict: penalties are handled by dedicated rule methods.
     */
    @Test
    void spendFoodMoreThanAvailableThrowsAndDoesNotChangePrestige() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "TURN_ORDER"));
        player.addFood(2);

        assertThrows(IllegalStateException.class, () -> player.spendFood(5));

        assertEquals(2, player.getFood());
        assertEquals(0, player.getPrestigePoints());
    }

    /**
     * Turn order penalty must still convert missing food into prestige loss.
     */
    @Test
    void turnPenaltyStillRemovesPrestigeWhenFoodIsMissing() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "TURN_ORDER"));

        player.applyTurnPenalty(1, 2);

        assertEquals(0, player.getFood());
        assertEquals(-2, player.getPrestigePoints());
    }
}