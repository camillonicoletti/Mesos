package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.card.effect.FullSetFoodEffect;
import it.polimi.ingsw.model.card.effect.InventorPairFoodEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.InventorIcons;
import it.polimi.ingsw.model.enums.TotemColor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Player (model).
 *
 * <p>Verifies: tribe management, food / PP accounting and building-effect application on the Player entity.
 */
public class PlayerTest {

    @Test
    void constructorShouldInitializePlayerCorrectly() {
        Totem totem = new Totem(TotemColor.RED, "TURN_ORDER");
        Player player = new Player("Luca", totem);

        assertEquals("Luca", player.getNickname());
        assertEquals(0, player.getFood());
        assertEquals(0, player.getPrestigePoints());
        assertNotNull(player.getTribe());
        assertSame(totem, player.getTotem());
    }

    @Test
    void addFoodShouldIncreaseFood() {
        Player player = new Player("Luca", new Totem(TotemColor.BLUE, "TURN_ORDER"));

        player.addFood(5);

        assertEquals(5, player.getFood());
    }

    @Test
    void addFoodShouldAccumulateFood() {
        Player player = new Player("Luca", new Totem(TotemColor.BLUE, "TURN_ORDER"));

        player.addFood(2);
        player.addFood(3);

        assertEquals(5, player.getFood());
    }

    @Test
    void canAffordShouldReturnTrueWhenPlayerHasExactFood() {
        Player player = new Player("Luca", new Totem(TotemColor.YELLOW, "TURN_ORDER"));
        player.addFood(4);

        assertTrue(player.canAfford(4));
    }

    @Test
    void canAffordShouldReturnTrueWhenPlayerHasMoreThanRequiredFood() {
        Player player = new Player("Luca", new Totem(TotemColor.YELLOW, "TURN_ORDER"));
        player.addFood(6);

        assertTrue(player.canAfford(4));
    }

    @Test
    void canAffordShouldReturnFalseWhenPlayerHasNotEnoughFood() {
        Player player = new Player("Luca", new Totem(TotemColor.WHITE, "TURN_ORDER"));
        player.addFood(2);

        assertFalse(player.canAfford(3));
    }

    @Test
    void spendFoodShouldDecreaseFoodWhenAffordable() {
        Player player = new Player("Luca", new Totem(TotemColor.RED, "TURN_ORDER"));
        player.addFood(5);

        player.spendFood(3);

        assertEquals(2, player.getFood());
    }

    @Test
    void spendFoodShouldKeepFoodAtZeroWhenSpendingAllFood() {
        Player player = new Player("Luca", new Totem(TotemColor.RED, "TURN_ORDER"));
        player.addFood(5);

        player.spendFood(5);

        assertEquals(0, player.getFood());
    }

    /**
     * Spending more food than available must be rejected.
     * Prestige penalties are handled by dedicated rule methods, not by generic food spending.
     */
    @Test
    void spendFoodShouldThrowWhenNotAffordable() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "TURN_ORDER"));

        player.addFood(2);

        assertThrows(IllegalStateException.class, () -> player.spendFood(5));

        assertEquals(2, player.getFood());
        assertEquals(0, player.getPrestigePoints());
    }
    @Test
    void addPrestigePointsShouldIncreasePrestigePoints() {
        Player player = new Player("Luca", new Totem(TotemColor.BLUE, "TURN_ORDER"));

        player.addPrestigePoints(7);

        assertEquals(7, player.getPrestigePoints());
    }

    @Test
    void addPrestigePointsShouldAccumulatePrestigePoints() {
        Player player = new Player("Luca", new Totem(TotemColor.BLUE, "TURN_ORDER"));

        player.addPrestigePoints(4);
        player.addPrestigePoints(6);

        assertEquals(10, player.getPrestigePoints());
    }

    @Test
    void removePrestigePointsShouldDecreasePrestigePoints() {
        Player player = new Player("Luca", new Totem(TotemColor.WHITE, "TURN_ORDER"));
        player.addPrestigePoints(8);

        player.removePrestigePoints(3);

        assertEquals(5, player.getPrestigePoints());
    }

    @Test
    void removePrestigePointsShouldAllowNegativePrestigePoints() {
        Player player = new Player("Luca", new Totem(TotemColor.WHITE, "TURN_ORDER"));

        player.removePrestigePoints(4);

        assertEquals(-4, player.getPrestigePoints());
    }

    @Test
    void getTribeShouldAlwaysReturnTheSameTribeInstance() {
        Player player = new Player("Luca", new Totem(TotemColor.YELLOW, "TURN_ORDER"));

        Tribe tribe1 = player.getTribe();
        Tribe tribe2 = player.getTribe();

        assertSame(tribe1, tribe2);
    }

    @Test
    void getTotemShouldReturnTheSameTotemPassedToConstructor() {
        Totem totem = new Totem(TotemColor.RED, "TURN_ORDER");
        Player player = new Player("Luca", totem);

        assertSame(totem, player.getTotem());
    }

    // -------------------------------------------------------
    // applyTurnPenalty
    // -------------------------------------------------------

    @Test
    void applyTurnPenaltyShouldSpendFoodWhenAffordable() {
        Player player = new Player("Luca", new Totem(TotemColor.RED, "TURN_ORDER"));
        player.addFood(3);

        player.applyTurnPenalty(1, 2);

        assertEquals(2, player.getFood());
        assertEquals(0, player.getPrestigePoints());
    }

    @Test
    void applyTurnPenaltyShouldRemovePrestigeWhenFoodInsufficient() {
        Player player = new Player("Luca", new Totem(TotemColor.RED, "TURN_ORDER"));

        player.applyTurnPenalty(1, 2);

        assertEquals(0, player.getFood());
        assertEquals(-2, player.getPrestigePoints());
    }

    @Test
    void applyTurnPenaltyShouldSpendFoodWhenExactlyAffordable() {
        Player player = new Player("Luca", new Totem(TotemColor.RED, "TURN_ORDER"));
        player.addFood(1);

        player.applyTurnPenalty(1, 2);

        assertEquals(0, player.getFood());
        assertEquals(0, player.getPrestigePoints());
    }

    // -------------------------------------------------------
    // addCharacterCard — building effect triggers
    // -------------------------------------------------------

    @Test
    void addCharacterCardTriggerFullSetFoodEffectWhenSetCompleted() {
        Player player = new Player("Test", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addBuildingCard(new BuildingCard(1, Era.I, 0, new FullSetFoodEffect(), 0));

        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new BuilderCard(2, Era.I, 2, 1, 1));
        player.addCharacterCard(new GathererCard(3, Era.I, 2));
        player.addCharacterCard(new HunterCard(4, Era.I, 2, false));
        player.addCharacterCard(new InventorCard(5, Era.I, 2, InventorIcons.INVENTION_1));
        player.addCharacterCard(new ShamanCard(6, Era.I, 1, 2)); // completes the set

        assertEquals(5, player.getFood()); // +5 food for completing 1 set
    }

    @Test
    void addCharacterCardDoesNotTriggerFullSetFoodEffectWithoutBuilding() {
        Player player = new Player("Test", new Totem(TotemColor.RED, "OFFER_TRACK"));

        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new BuilderCard(2, Era.I, 2, 1, 1));
        player.addCharacterCard(new GathererCard(3, Era.I, 2));
        player.addCharacterCard(new HunterCard(4, Era.I, 2, false));
        player.addCharacterCard(new InventorCard(5, Era.I, 2, InventorIcons.INVENTION_1));
        player.addCharacterCard(new ShamanCard(6, Era.I, 1, 2));

        assertEquals(0, player.getFood()); // no building → no food
    }

    @Test
    void addCharacterCardTriggerInventorPairFoodEffect() {
        Player player = new Player("Test", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addBuildingCard(new BuildingCard(1, Era.I, 0, new InventorPairFoodEffect(), 0));

        player.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        player.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_1)); // pair!

        assertEquals(3, player.getFood()); // +3 food for 1 pair
    }

    @Test
    void addCharacterCardDoesNotRewardPairsFormedBeforeBuildingWasAcquired() {
        Player player = new Player("Test", new Totem(TotemColor.RED, "OFFER_TRACK"));

        // pair formed before building → effect not triggered
        player.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        player.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_1));

        // building added after the pair exists → no addCharacterCard called → no trigger
        player.addBuildingCard(new BuildingCard(3, Era.I, 0, new InventorPairFoodEffect(), 0));

        assertEquals(0, player.getFood()); // no food because effect was never triggered
    }

    @Test
    void testFullSetFoodEffectRewardsTwoSetsIndependently() {
        Player player = new Player("Test", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addBuildingCard(new BuildingCard(1, Era.I, 0, new FullSetFoodEffect(), 0));

        // first set
        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new BuilderCard(2, Era.I, 2, 1, 0));
        player.addCharacterCard(new GathererCard(3, Era.I, 2));
        player.addCharacterCard(new HunterCard(4, Era.I, 2, false));
        player.addCharacterCard(new InventorCard(5, Era.I, 2, InventorIcons.INVENTION_1));
        player.addCharacterCard(new ShamanCard(6, Era.I, 1, 2)); // completes set 1 → +5

        assertEquals(5, player.getFood());

        // second set
        player.addCharacterCard(new ArtistCard(7, Era.I, 2));
        player.addCharacterCard(new BuilderCard(8, Era.I, 2, 1, 0));
        player.addCharacterCard(new GathererCard(9, Era.I, 2));
        player.addCharacterCard(new HunterCard(10, Era.I, 2, false));
        player.addCharacterCard(new InventorCard(11, Era.I, 2, InventorIcons.INVENTION_2));
        player.addCharacterCard(new ShamanCard(12, Era.I, 1, 2)); // completes set 2 → +5

        assertEquals(10, player.getFood()); // 5 + 5
    }

    @Test
    void testInventorPairFoodEffectWithTwoDifferentIconPairs() {
        Player player = new Player("Test", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addBuildingCard(new BuildingCard(1, Era.I, 0, new InventorPairFoodEffect(), 0));

        player.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        player.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_1)); // coppia 1 → +3
        player.addCharacterCard(new InventorCard(3, Era.I, 2, InventorIcons.INVENTION_2));
        player.addCharacterCard(new InventorCard(4, Era.I, 2, InventorIcons.INVENTION_2)); // coppia 2 → +3

        assertEquals(6, player.getFood()); // 3 + 3
    }
}
