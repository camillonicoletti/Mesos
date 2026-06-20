package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.InventorIcons;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Tribe (model).
 *
 * Verifies character counting, type filtering, building discount and inventor-set logic.
 */
public class TribeTest {

    // -------------------------------------------------------
    // countCharacterCards
    // -------------------------------------------------------

    @Test
    void countCharacterCards_shouldReturnZero_whenTribeIsEmpty() {
        Tribe tribe = new Tribe();
        assertEquals(0, tribe.countCharacterCards());
    }

    @Test
    void countCharacterCards_shouldReturnTotalNumberOfCards() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new ArtistCard(1, Era.I, 2));
        tribe.addCharacterCard(new HunterCard(2, Era.I, 2, false));
        tribe.addCharacterCard(new ShamanCard(3, Era.I, 2, 1));
        assertEquals(3, tribe.countCharacterCards());
    }

    // -------------------------------------------------------
    // countCharactersByType
    // -------------------------------------------------------

    @Test
    void countCharactersByType_shouldReturnZero_whenNoCardsOfThatType() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new ArtistCard(1, Era.I, 2));
        assertEquals(0, tribe.countCharactersByType(HunterCard.class));
    }

    @Test
    void countCharactersByType_shouldCountOnlyMatchingType() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new HunterCard(1, Era.I, 2, false));
        tribe.addCharacterCard(new HunterCard(2, Era.I, 2, false));
        tribe.addCharacterCard(new ArtistCard(3, Era.I, 2));
        assertEquals(2, tribe.countCharactersByType(HunterCard.class));
        assertEquals(1, tribe.countCharactersByType(ArtistCard.class));
        assertEquals(0, tribe.countCharactersByType(GathererCard.class));
    }

    // -------------------------------------------------------
    // getBuildingDiscount
    // -------------------------------------------------------

    @Test
    void getBuildingDiscount_shouldReturnZero_whenNoBuilders() {
        Tribe tribe = new Tribe();
        assertEquals(0, tribe.getBuildingDiscount());
    }

    @Test
    void getBuildingDiscount_shouldSumFoodDiscountOfAllBuilders() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new BuilderCard(1, Era.I, 2, 2, 1));
        tribe.addCharacterCard(new BuilderCard(2, Era.I, 2, 3, 2));
        assertEquals(3, tribe.getBuildingDiscount());
    }

    // -------------------------------------------------------
    // countArtistPairs
    // -------------------------------------------------------

    @Test
    void countArtistPairs_shouldReturnZero_whenTribeIsEmpty() {
        Tribe tribe = new Tribe();
        assertEquals(0, tribe.countArtistPairs());
    }

    @Test
    void countArtistPairs_shouldReturnZero_whenOneArtist() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new ArtistCard(1, Era.I, 2));
        assertEquals(0, tribe.countArtistPairs());
    }

    @Test
    void countArtistPairs_shouldReturnOne_whenTwoArtists() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new ArtistCard(1, Era.I, 2));
        tribe.addCharacterCard(new ArtistCard(2, Era.II, 2));
        assertEquals(1, tribe.countArtistPairs());
    }

    @Test
    void countArtistPairs_shouldUseFloorDivision_whenThreeArtists() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new ArtistCard(1, Era.I, 2));
        tribe.addCharacterCard(new ArtistCard(2, Era.II, 2));
        tribe.addCharacterCard(new ArtistCard(3, Era.III, 2));
        assertEquals(1, tribe.countArtistPairs());
    }

    @Test
    void countArtistPairs_shouldIgnoreNonArtistCards() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new ArtistCard(1, Era.I, 2));
        tribe.addCharacterCard(new BuilderCard(2, Era.I, 2, 0, 0));
        tribe.addCharacterCard(new ArtistCard(3, Era.II, 2));
        assertEquals(1, tribe.countArtistPairs());
    }

    // -------------------------------------------------------
    // countDistinctInventorIcons
    // -------------------------------------------------------

    @Test
    void countDistinctInventorIcons_shouldReturnZero_whenNoInventors() {
        Tribe tribe = new Tribe();
        assertEquals(0, tribe.countDistinctInventorIcons());
    }

    @Test
    void countDistinctInventorIcons_shouldReturnOne_whenOneInventor() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        assertEquals(1, tribe.countDistinctInventorIcons());
    }

    @Test
    void countDistinctInventorIcons_shouldCountOnlyDifferentIcons() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        tribe.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_1));
        tribe.addCharacterCard(new InventorCard(3, Era.II, 2, InventorIcons.INVENTION_3));
        tribe.addCharacterCard(new InventorCard(4, Era.III, 2, InventorIcons.INVENTION_5));
        assertEquals(3, tribe.countDistinctInventorIcons());
    }

    @Test
    void countDistinctInventorIcons_shouldIgnoreNonInventorCards() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new ArtistCard(1, Era.I, 2));
        tribe.addCharacterCard(new InventorCard(2, Era.II, 2, InventorIcons.INVENTION_2));
        assertEquals(1, tribe.countDistinctInventorIcons());
    }

    // -------------------------------------------------------
    // countInventorPairs
    // -------------------------------------------------------

    @Test
    void countInventorPairs_shouldReturnZero_whenNoInventors() {
        Tribe tribe = new Tribe();
        assertEquals(0, tribe.countInventorPairs());
    }

    @Test
    void countInventorPairs_shouldReturnZero_whenAllIconsDifferent() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        tribe.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_2));
        assertEquals(0, tribe.countInventorPairs());
    }

    @Test
    void countInventorPairs_shouldReturnOnePair_whenTwoSameIcon() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        tribe.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_1));
        assertEquals(1, tribe.countInventorPairs());
    }

    @Test
    void countInventorPairs_shouldCountPairsAcrossMultipleIcons() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        tribe.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_1));
        tribe.addCharacterCard(new InventorCard(3, Era.I, 2, InventorIcons.INVENTION_2));
        tribe.addCharacterCard(new InventorCard(4, Era.I, 2, InventorIcons.INVENTION_2));
        assertEquals(2, tribe.countInventorPairs());
    }

    @Test
    void countInventorPairs_shouldReturnOnePair_whenThreeSameIcon() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        tribe.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_1));
        tribe.addCharacterCard(new InventorCard(3, Era.I, 2, InventorIcons.INVENTION_1));
        assertEquals(1, tribe.countInventorPairs());
    }

    // -------------------------------------------------------
    // countFullSets
    // -------------------------------------------------------

    @Test
    void countFullSets_shouldReturnZero_whenTribeIsEmpty() {
        Tribe tribe = new Tribe();
        assertEquals(0, tribe.countFullSets());
    }

    @Test
    void countFullSets_shouldReturnZero_whenSetIsIncomplete() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new ArtistCard(1, Era.I, 2));
        tribe.addCharacterCard(new BuilderCard(2, Era.I, 2, 0, 0));
        tribe.addCharacterCard(new GathererCard(3, Era.I, 2));
        assertEquals(0, tribe.countFullSets());
    }

    @Test
    void countFullSets_shouldReturnOne_whenOneCompleteSet() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new ArtistCard(1, Era.I, 2));
        tribe.addCharacterCard(new BuilderCard(2, Era.I, 2, 0, 0));
        tribe.addCharacterCard(new GathererCard(3, Era.I, 2));
        tribe.addCharacterCard(new HunterCard(4, Era.I, 2, false));
        tribe.addCharacterCard(new InventorCard(5, Era.I, 2, InventorIcons.INVENTION_1));
        tribe.addCharacterCard(new ShamanCard(6, Era.I, 2, 1));
        assertEquals(1, tribe.countFullSets());
    }

    @Test
    void countFullSets_shouldReturnTwo_whenTwoCompleteSets() {
        Tribe tribe = new Tribe();
        for (int i = 0; i < 2; i++) {
            tribe.addCharacterCard(new ArtistCard(i * 6 + 1, Era.I, 2));
            tribe.addCharacterCard(new BuilderCard(i * 6 + 2, Era.I, 2, 0, 0));
            tribe.addCharacterCard(new GathererCard(i * 6 + 3, Era.I, 2));
            tribe.addCharacterCard(new HunterCard(i * 6 + 4, Era.I, 2, false));
            tribe.addCharacterCard(new InventorCard(i * 6 + 5, Era.I, 2, InventorIcons.INVENTION_1));
            tribe.addCharacterCard(new ShamanCard(i * 6 + 6, Era.I, 2, 1));
        }
        assertEquals(2, tribe.countFullSets());
    }

    @Test
    void countFullSets_shouldBeLimitedByRarestType() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new ArtistCard(1, Era.I, 2));
        tribe.addCharacterCard(new ArtistCard(2, Era.I, 2));
        tribe.addCharacterCard(new BuilderCard(3, Era.I, 2, 0, 0));
        tribe.addCharacterCard(new GathererCard(4, Era.I, 2));
        tribe.addCharacterCard(new HunterCard(5, Era.I, 2, false));
        tribe.addCharacterCard(new InventorCard(6, Era.I, 2, InventorIcons.INVENTION_1));
        tribe.addCharacterCard(new ShamanCard(7, Era.I, 2, 1));
        assertEquals(1, tribe.countFullSets());
    }

    // -------------------------------------------------------
    // countShamanStars
    // -------------------------------------------------------

    @Test
    void countShamanStars_shouldReturnZero_whenNoShamans() {
        Tribe tribe = new Tribe();
        assertEquals(0, tribe.countShamanStars());
    }

    @Test
    void countShamanStars_shouldReturnOneShamanStars() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new ShamanCard(1, Era.I, 2, 3));
        assertEquals(3, tribe.countShamanStars());
    }

    @Test
    void countShamanStars_shouldSumStarsFromAllShamans() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new ShamanCard(1, Era.I, 2, 3));
        tribe.addCharacterCard(new ShamanCard(2, Era.II, 2, 2));
        assertEquals(5, tribe.countShamanStars());
    }

    @Test
    void countShamanStars_shouldIgnoreNonShamanCards() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new ShamanCard(1, Era.I, 2, 2));
        tribe.addCharacterCard(new ArtistCard(2, Era.I, 2));
        tribe.addCharacterCard(new HunterCard(3, Era.I, 2, false));
        assertEquals(2, tribe.countShamanStars());
    }

    // -------------------------------------------------------
    // addCharacterCard / getCharacterCards
    // -------------------------------------------------------

    @Test
    void addCharacterCard_shouldAddCardToList() {
        Tribe tribe = new Tribe();
        ArtistCard card = new ArtistCard(1, Era.I, 2);
        tribe.addCharacterCard(card);
        assertEquals(1, tribe.getCharacterCards().size());
        assertTrue(tribe.getCharacterCards().contains(card));
    }

    @Test
    void getCharacterCards_shouldBeEmptyOnNewTribe() {
        Tribe tribe = new Tribe();
        assertNotNull(tribe.getCharacterCards());
        assertTrue(tribe.getCharacterCards().isEmpty());
    }

    // -------------------------------------------------------
    // addBuildingCard / getBuildingCards
    // -------------------------------------------------------

    @Test
    void addBuildingCard_shouldAddCardToList() {
        Tribe tribe = new Tribe();
        BuildingCard card = new BuildingCard(1, Era.I, 0, p -> {}, 0);
        tribe.addBuildingCard(card);
        assertEquals(1, tribe.getBuildingCards().size());
        assertTrue(tribe.getBuildingCards().contains(card));
    }

    @Test
    void getBuildingCards_shouldBeEmptyOnNewTribe() {
        Tribe tribe = new Tribe();
        assertNotNull(tribe.getBuildingCards());
        assertTrue(tribe.getBuildingCards().isEmpty());
    }
}