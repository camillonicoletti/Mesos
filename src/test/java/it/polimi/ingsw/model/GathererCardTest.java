package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.ArtistCard;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.GathererCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.SustenanceEventCard;
import it.polimi.ingsw.model.card.effect.EndPPEffect;
import it.polimi.ingsw.model.card.effect.SustenanceFoodDiscountEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for GathererCard during the Sustenance event.
 *
 * Verifies the Mesos rulebook constraints on Gatherers: they never grant food directly,
 * each one reduces the Sustenance food requirement by 3 (clamped at zero, counting only
 * character cards), discounts stack linearly and combine with SustenanceFoodDiscountEffect
 * building discounts, and any prestige loss is proportional to the missing food after discount.
 */
class GathererCardTest {

    private Game buildGame(List<Player> players) {
        return new Game(players, 1, null, Era.I, null, null,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    private Player newPlayer() {
        return new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
    }

    /**
     * Adding a Gatherer to the tribe must not change the player's food:
     * the discount is a passive Sustenance effect, not a direct food gain.
     */
    @Test
    void addingGathererDoesNotGrantFoodImmediately() {
        Player player = newPlayer();

        player.addCharacterCard(new GathererCard(1, Era.I, 2));
        player.addCharacterCard(new GathererCard(2, Era.I, 2));

        assertEquals(0, player.getFood(),
                "Gatherers must not grant food when added to the tribe.");
        assertEquals(0, player.getPrestigePoints(),
                "Adding a Gatherer must not alter prestige points.");
    }

    /**
     * Sustenance counts characters only. Buildings (without a Sustenance
     * discount effect) must neither add to the required food nor reduce it.
     * Setup: 2 Hunters + 3 generic buildings -> required = 2 food, not 5.
     */
    @Test
    void buildingsDoNotCountAsCharactersDuringSustenance() {
        Player player = newPlayer();
        player.addFood(10);
        player.addCharacterCard(new HunterCard(1, Era.I, 2, false));
        player.addCharacterCard(new HunterCard(2, Era.I, 2, false));
        player.addBuildingCard(new BuildingCard(101, Era.I, 0, new EndPPEffect(), 0));
        player.addBuildingCard(new BuildingCard(102, Era.I, 0, new EndPPEffect(), 0));
        player.addBuildingCard(new BuildingCard(103, Era.I, 0, new EndPPEffect(), 0));

        Game game = buildGame(List.of(player));
        new SustenanceEventCard(1, Era.I, false, 2).resolve(game);

        assertEquals(8, player.getFood(),
                "Only the 2 characters must be fed; the 3 buildings must not raise the cost.");
        assertEquals(0, player.getPrestigePoints(),
                "Buildings must not generate any sustenance penalty.");
    }

    /**
     * Three Gatherers must stack as 3 x 3 = 9 food of discount.
     * Setup: 3 Gatherers + 10 Hunters = 13 characters, discount 9,
     * required = 4 food. With 10 available food the player pays 4 and
     * keeps 6, without any prestige loss.
     */
    @Test
    void multipleGathererDiscountsStackLinearly() {
        Player player = newPlayer();
        player.addFood(10);
        player.addCharacterCard(new GathererCard(1, Era.I, 2));
        player.addCharacterCard(new GathererCard(2, Era.I, 2));
        player.addCharacterCard(new GathererCard(3, Era.I, 2));
        for (int i = 0; i < 10; i++) {
            player.addCharacterCard(new HunterCard(100 + i, Era.I, 2, false));
        }

        Game game = buildGame(List.of(player));
        new SustenanceEventCard(1, Era.I, false, 2).resolve(game);

        assertEquals(6, player.getFood(),
                "13 characters minus 9 discount = 4 food required; 10 - 4 = 6 remaining.");
        assertEquals(0, player.getPrestigePoints(),
                "All characters are fed: no prestige must be lost.");
    }

    /**
     * Probe: when the Gatherer discount exceeds the character count, the
     * required food must be clamped at zero and the player must not receive
     * food, gain prestige or otherwise be rewarded for the excess discount.
     * Setup: 4 Gatherers + 1 Artist = 5 characters, discount 12,
     * required = max(0, 5 - 12) = 0.
     */
    @Test
    void sustenanceDiscountIsClampedAtZero() {
        Player player = newPlayer();
        player.addCharacterCard(new GathererCard(1, Era.I, 2));
        player.addCharacterCard(new GathererCard(2, Era.I, 2));
        player.addCharacterCard(new GathererCard(3, Era.I, 2));
        player.addCharacterCard(new GathererCard(4, Era.I, 2));
        player.addCharacterCard(new ArtistCard(5, Era.I, 2));

        Game game = buildGame(List.of(player));
        new SustenanceEventCard(1, Era.I, false, 2).resolve(game);

        assertEquals(0, player.getFood(),
                "An excess Gatherer discount must not grant food to the player.");
        assertEquals(0, player.getPrestigePoints(),
                "A negative theoretical cost must not produce a prestige gain.");
    }

    /**
     * When the available food does not cover the discounted requirement, the
     * prestige penalty must be proportional to the missing food units, not
     * to the number of characters before the discount.
     * Setup: 1 Gatherer + 5 Hunters = 6 characters, discount 3, required 3.
     * With only 1 food the player pays 1 and has 2 missing food units;
     * with ppPenaltyPerUnfed = 2 the prestige loss is 2 * 2 = 4 PP.
     */
    @Test
    void partiallyMissingFoodCausesProportionalPrestigeLoss() {
        Player player = newPlayer();
        player.addFood(1);
        player.addCharacterCard(new GathererCard(1, Era.I, 2));
        for (int i = 0; i < 5; i++) {
            player.addCharacterCard(new HunterCard(100 + i, Era.I, 2, false));
        }

        Game game = buildGame(List.of(player));
        new SustenanceEventCard(1, Era.I, false, 2).resolve(game);

        assertEquals(0, player.getFood(),
                "All available food must be paid before any prestige loss.");
        assertEquals(-4, player.getPrestigePoints(),
                "2 missing food units * ppPenaltyPerUnfed (2) = 4 PP lost.");
    }

    /**
     * Gatherer discount and SustenanceFoodDiscountEffect building discounts
     * are cumulative.
     * Setup: 1 Gatherer + 2 Hunters + 1 building that gives 1 food off per
     * Hunter in the tribe = 3 characters, discount 3 (Gatherer) + 2 (building
     * on 2 Hunters) = 5, required = max(0, 3 - 5) = 0.
     */
    @Test
    void gathererAndSustenanceBuildingDiscountsStack() {
        Player player = newPlayer();
        player.addCharacterCard(new GathererCard(1, Era.I, 2));
        player.addCharacterCard(new HunterCard(2, Era.I, 2, false));
        player.addCharacterCard(new HunterCard(3, Era.I, 2, false));
        player.addBuildingCard(new BuildingCard(
                10, Era.I, 0,
                new SustenanceFoodDiscountEffect(HunterCard.class),
                0));

        Game game = buildGame(List.of(player));
        new SustenanceEventCard(1, Era.I, false, 2).resolve(game);

        assertEquals(0, player.getFood(),
                "Cumulative discount fully covers the cost: no food must be spent.");
        assertEquals(0, player.getPrestigePoints(),
                "Cumulative discount fully covers the cost: no prestige must be lost.");
    }
}
