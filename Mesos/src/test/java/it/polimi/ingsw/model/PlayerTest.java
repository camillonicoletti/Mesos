package it.polimi.ingsw.model;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.enums.TotemPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerTest {

    @Test
    void constructorShouldInitializePlayerCorrectly() {
        Totem totem = new Totem(TotemColor.RED, TotemPosition.TURN_ORDER);
        Player player = new Player("Luca", totem);

        assertEquals("Luca", player.getNickname());
        assertEquals(0, player.getFood());
        assertEquals(0, player.getPrestigePoints());
        assertNotNull(player.getTribe());
        assertSame(totem, player.getTotem());
    }

    @Test
    void addFoodShouldIncreaseFood() {
        Player player = new Player("Luca", new Totem(TotemColor.BLUE, TotemPosition.TURN_ORDER));

        player.addFood(5);

        assertEquals(5, player.getFood());
    }

    @Test
    void addFoodShouldAccumulateFood() {
        Player player = new Player("Luca", new Totem(TotemColor.BLUE, TotemPosition.TURN_ORDER));

        player.addFood(2);
        player.addFood(3);

        assertEquals(5, player.getFood());
    }

    @Test
    void canAffordShouldReturnTrueWhenPlayerHasExactFood() {
        Player player = new Player("Luca", new Totem(TotemColor.YELLOW, TotemPosition.TURN_ORDER));
        player.addFood(4);

        assertTrue(player.canAfford(4));
    }

    @Test
    void canAffordShouldReturnTrueWhenPlayerHasMoreThanRequiredFood() {
        Player player = new Player("Luca", new Totem(TotemColor.YELLOW, TotemPosition.TURN_ORDER));
        player.addFood(6);

        assertTrue(player.canAfford(4));
    }

    @Test
    void canAffordShouldReturnFalseWhenPlayerHasNotEnoughFood() {
        Player player = new Player("Luca", new Totem(TotemColor.WHITE, TotemPosition.TURN_ORDER));
        player.addFood(2);

        assertFalse(player.canAfford(3));
    }

    @Test
    void spendFoodShouldReturnTrueAndDecreaseFoodWhenAffordable() {
        Player player = new Player("Luca", new Totem(TotemColor.RED, TotemPosition.TURN_ORDER));
        player.addFood(5);

        boolean result = player.spendFood(3);

        assertTrue(result);
        assertEquals(2, player.getFood());
    }

    @Test
    void spendFoodShouldReturnTrueAndKeepFoodAtZeroWhenSpendingAllFood() {
        Player player = new Player("Luca", new Totem(TotemColor.RED, TotemPosition.TURN_ORDER));
        player.addFood(5);

        boolean result = player.spendFood(5);

        assertTrue(result);
        assertEquals(0, player.getFood());
    }

    @Test
    void spendFoodShouldReturnFalseAndNotChangeFoodWhenNotAffordable() {
        Player player = new Player("Luca", new Totem(TotemColor.PURPLE, TotemPosition.TURN_ORDER));
        player.addFood(2);

        boolean result = player.spendFood(5);

        assertFalse(result);
        assertEquals(2, player.getFood());
    }

    @Test
    void addPrestigePointsShouldIncreasePrestigePoints() {
        Player player = new Player("Luca", new Totem(TotemColor.BLUE, TotemPosition.TURN_ORDER));

        player.addPrestigePoints(7);

        assertEquals(7, player.getPrestigePoints());
    }

    @Test
    void addPrestigePointsShouldAccumulatePrestigePoints() {
        Player player = new Player("Luca", new Totem(TotemColor.BLUE, TotemPosition.TURN_ORDER));

        player.addPrestigePoints(4);
        player.addPrestigePoints(6);

        assertEquals(10, player.getPrestigePoints());
    }

    @Test
    void removePrestigePointsShouldDecreasePrestigePoints() {
        Player player = new Player("Luca", new Totem(TotemColor.WHITE, TotemPosition.TURN_ORDER));
        player.addPrestigePoints(8);

        player.removePrestigePoints(3);

        assertEquals(5, player.getPrestigePoints());
    }

    @Test
    void removePrestigePointsShouldAllowNegativePrestigePoints() {
        Player player = new Player("Luca", new Totem(TotemColor.WHITE, TotemPosition.TURN_ORDER));

        player.removePrestigePoints(4);

        assertEquals(-4, player.getPrestigePoints());
    }

    @Test
    void getTribeShouldAlwaysReturnTheSameTribeInstance() {
        Player player = new Player("Luca", new Totem(TotemColor.YELLOW, TotemPosition.TURN_ORDER));

        Tribe tribe1 = player.getTribe();
        Tribe tribe2 = player.getTribe();

        assertSame(tribe1, tribe2);
    }

    @Test
    void getTotemShouldReturnTheSameTotemPassedToConstructor() {
        Totem totem = new Totem(TotemColor.RED, TotemPosition.TURN_ORDER);
        Player player = new Player("Luca", totem);

        assertSame(totem, player.getTotem());
    }
}