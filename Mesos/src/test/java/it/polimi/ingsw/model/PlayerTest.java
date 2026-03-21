package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.card.effect.InventorPairFoodEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.InventorIcons;
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
    void spendFoodShouldDecreaseFoodWhenAffordable() {
        Player player = new Player("Luca", new Totem(TotemColor.RED, TotemPosition.TURN_ORDER));
        player.addFood(5);

        player.spendFood(3);

        assertEquals(2, player.getFood());
    }

    @Test
    void spendFoodShouldKeepFoodAtZeroWhenSpendingAllFood() {
        Player player = new Player("Luca", new Totem(TotemColor.RED, TotemPosition.TURN_ORDER));
        player.addFood(5);

        player.spendFood(5);

        assertEquals(0, player.getFood());
    }

    @Test
    void spendFoodShouldDeductPrestigeWhenNotAffordable() {
        // if food is insufficient, the missing amount is taken from prestige points
        Player player = new Player("Luca", new Totem(TotemColor.PURPLE, TotemPosition.TURN_ORDER));
        player.addFood(2);

        player.spendFood(5);

        assertEquals(0, player.getFood());
        assertEquals(-3, player.getPrestigePoints()); // 5 - 2 = 3 missing
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
    // -------------------------------------------------------
// applyTurnPenalty
// -------------------------------------------------------

    @Test
    void applyTurnPenaltyShouldSpendFoodWhenAffordable() {
        // player has enough food → spends food, prestige unchanged
        Player player = new Player("Luca", new Totem(TotemColor.RED, TotemPosition.TURN_ORDER));
        player.addFood(3);

        player.applyTurnPenalty(1, 2);

        assertEquals(2, player.getFood());
        assertEquals(0, player.getPrestigePoints());
    }

    @Test
    void applyTurnPenaltyShouldRemovePrestigeWhenFoodInsufficient() {
        // player has no food → loses prestige points, food stays at 0
        Player player = new Player("Luca", new Totem(TotemColor.RED, TotemPosition.TURN_ORDER));

        player.applyTurnPenalty(1, 2);

        assertEquals(0, player.getFood());
        assertEquals(-2, player.getPrestigePoints());
    }

    @Test
    void applyTurnPenaltyShouldSpendFoodWhenExactlyAffordable() {
        // player has exactly 1 food → spends it, no prestige lost
        Player player = new Player("Luca", new Totem(TotemColor.RED, TotemPosition.TURN_ORDER));
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
        Player player = new Player("Test", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player.addBuildingCard(new BuildingCard(1, new FullSetFoodEffect(), Era.I, 0));

        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new BuilderCard(2, 1, 1, Era.I, 2));
        player.addCharacterCard(new GathererCard(3, Era.I, 2));
        player.addCharacterCard(new HunterCard(4, false, Era.I, 2));
        player.addCharacterCard(new InventorCard(5, Era.I, InventorIcons.ICON_1, 2));
        player.addCharacterCard(new ShamanCard(6, Era.I, 1, 2)); // completes the set

        assertEquals(5, player.getFood()); // +5 food for completing 1 set
    }

    @Test
    void addCharacterCardDoesNotTriggerFullSetFoodEffectWithoutBuilding() {
        Player player = new Player("Test", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));

        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new BuilderCard(2, 1, 1, Era.I, 2));
        player.addCharacterCard(new GathererCard(3, Era.I, 2));
        player.addCharacterCard(new HunterCard(4, false, Era.I, 2));
        player.addCharacterCard(new InventorCard(5, Era.I, InventorIcons.ICON_1, 2));
        player.addCharacterCard(new ShamanCard(6, Era.I, 1, 2));

        assertEquals(0, player.getFood()); // no building → no food
    }

    @Test
    void addCharacterCardTriggerInventorPairFoodEffect() {
        Player player = new Player("Test", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player.addBuildingCard(new BuildingCard(1, new InventorPairFoodEffect(), Era.I, 0));

        player.addCharacterCard(new InventorCard(1, Era.I, InventorIcons.ICON_1, 2));
        player.addCharacterCard(new InventorCard(2, Era.I, InventorIcons.ICON_1, 2)); // pair!

        assertEquals(3, player.getFood()); // +3 food for 1 pair
    }

    @Test
    void addCharacterCardDoesNotRewardAlreadyExistingPairs() {
        Player player = new Player("Test", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));

        player.addCharacterCard(new InventorCard(1, Era.I, InventorIcons.ICON_1, 2));
        player.addCharacterCard(new InventorCard(2, Era.I, InventorIcons.ICON_1, 2));

        // building added AFTER the pair exists
        player.addBuildingCard(new BuildingCard(3, new InventorPairFoodEffect(), Era.I, 0));

        // adding a third inventor of same type — no new pair formed
        player.addCharacterCard(new InventorCard(4, Era.I, InventorIcons.ICON_1, 2));

        assertEquals(0, player.getFood());
    }


}