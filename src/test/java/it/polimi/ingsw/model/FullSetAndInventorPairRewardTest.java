package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.ArtistCard;
import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.GathererCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.InventorCard;
import it.polimi.ingsw.model.card.ShamanCard;
import it.polimi.ingsw.model.card.effect.FullSetFoodEffect;
import it.polimi.ingsw.model.card.effect.InventorPairFoodEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.InventorIcons;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bug-hunter for the "only new sets / pairs" tracking in
 * {@link FullSetFoodEffect} and {@link InventorPairFoodEffect}.
 *
 * <p>Pins down:
 * <ul>
 *   <li>Buying a building with an already-completed set does NOT retroactively reward.</li>
 *   <li>Each new set rewards once and only once.</li>
 *   <li>Multiple copies of the building each reward independently (additive).</li>
 *   <li>InventorPair counts floor(count/2) per icon group, rewards on new pairs only.</li>
 * </ul>
 */
class FullSetAndInventorPairRewardTest {

    private Player playerWithOneFullSet() {
        Player p = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        p.addCharacterCard(new ArtistCard(1, Era.I, 2));
        p.addCharacterCard(new BuilderCard(2, Era.I, 2, 0, 0));
        p.addCharacterCard(new GathererCard(3, Era.I, 2));
        p.addCharacterCard(new HunterCard(4, Era.I, 2, false));
        p.addCharacterCard(new InventorCard(5, Era.I, 2, InventorIcons.INVENTION_1));
        p.addCharacterCard(new ShamanCard(6, Era.I, 2, 1));
        return p;
    }

    @Test
    void fullSetFood_doesNotRewardRetroactively_whenBuildingAcquiredAfterSet() {
        Player p = playerWithOneFullSet();
        assertEquals(1, p.getTribe().countFullSets());

        // Acquire building AFTER the set was completed — no retroactive food.
        p.addBuildingCard(new BuildingCard(900, Era.I, 0, new FullSetFoodEffect(), 0));
        assertEquals(0, p.getFood(), "Building #1 must not reward already-completed sets");
    }

    @Test
    void fullSetFood_rewardsExactlyOncePerNewSet() {
        Player p = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        p.addBuildingCard(new BuildingCard(900, Era.I, 0, new FullSetFoodEffect(), 0));

        // Build a full set (6 distinct types). Only the LAST card completes the set.
        p.addCharacterCard(new ArtistCard(1, Era.I, 2));
        p.addCharacterCard(new BuilderCard(2, Era.I, 2, 0, 0));
        p.addCharacterCard(new GathererCard(3, Era.I, 2));
        p.addCharacterCard(new HunterCard(4, Era.I, 2, false));
        p.addCharacterCard(new InventorCard(5, Era.I, 2, InventorIcons.INVENTION_1));
        assertEquals(0, p.getFood(), "No reward until the 6th type is added");

        p.addCharacterCard(new ShamanCard(6, Era.I, 2, 1));
        assertEquals(5, p.getFood(), "First completed set ⇒ +5 food");

        // Add a second card of an existing type — no new set, no food.
        p.addCharacterCard(new ArtistCard(7, Era.I, 2));
        assertEquals(5, p.getFood(), "Duplicate type does not add a new set");
    }

    @Test
    void fullSetFood_multipleBuildingsAddIndependently() {
        Player p = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        p.addBuildingCard(new BuildingCard(900, Era.I, 0, new FullSetFoodEffect(), 0));
        p.addBuildingCard(new BuildingCard(901, Era.I, 0, new FullSetFoodEffect(), 0));

        // Add 5/6 — no reward yet.
        p.addCharacterCard(new ArtistCard(1, Era.I, 2));
        p.addCharacterCard(new BuilderCard(2, Era.I, 2, 0, 0));
        p.addCharacterCard(new GathererCard(3, Era.I, 2));
        p.addCharacterCard(new HunterCard(4, Era.I, 2, false));
        p.addCharacterCard(new InventorCard(5, Era.I, 2, InventorIcons.INVENTION_1));

        // Complete the set — both buildings fire independently.
        p.addCharacterCard(new ShamanCard(6, Era.I, 2, 1));
        assertEquals(10, p.getFood(),
                "Two copies of building #1 each grant 5 food on the first completed set");
    }

    @Test
    void inventorPairFood_rewardsOnlyOnNewPairs() {
        Player p = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        p.addBuildingCard(new BuildingCard(900, Era.I, 0, new InventorPairFoodEffect(), 0));

        // 1 inventor icon-1 ⇒ 0 pairs.
        p.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        assertEquals(0, p.getFood());

        // Second inventor with the same icon ⇒ 1 new pair ⇒ +3 food.
        p.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_1));
        assertEquals(3, p.getFood());

        // Third inventor with the same icon ⇒ still 1 pair (floor(3/2)) ⇒ no extra food.
        p.addCharacterCard(new InventorCard(3, Era.I, 2, InventorIcons.INVENTION_1));
        assertEquals(3, p.getFood());

        // Fourth inventor with the same icon ⇒ 2 pairs ⇒ +3 more food.
        p.addCharacterCard(new InventorCard(4, Era.I, 2, InventorIcons.INVENTION_1));
        assertEquals(6, p.getFood());
    }

    @Test
    void inventorPairFood_doesNotRewardRetroactively_whenBuildingAcquiredAfterPair() {
        Player p = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        p.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        p.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_1));
        assertEquals(0, p.getFood());

        // Building acquired AFTER the pair was formed — no retroactive food.
        p.addBuildingCard(new BuildingCard(900, Era.I, 0, new InventorPairFoodEffect(), 0));
        assertEquals(0, p.getFood(), "Building #5 must not reward already-formed pairs");
    }

    @Test
    void inventorPair_distinctIconsDoNotPairTogether() {
        Player p = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        p.addBuildingCard(new BuildingCard(900, Era.I, 0, new InventorPairFoodEffect(), 0));

        p.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        p.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_2));
        // Two inventors, two distinct icons ⇒ 0 pairs.
        assertEquals(0, p.getFood(),
                "Pairs are per-icon: distinct icons don't pair");
    }
}
