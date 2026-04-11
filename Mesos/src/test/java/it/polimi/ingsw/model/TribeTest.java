package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.InventorIcons;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TribeTest {

    // =========================================================
    // countCharacterCards
    // =========================================================

    @Test
    void countCharacterCards_shouldReturnZero_whenTribeIsEmpty() {
        Tribe tribe = new Tribe();

        assertEquals(0, tribe.countCharacterCards());
    }

    @Test
    void countCharacterCards_shouldReturnTotalNumberOfCharacters() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new ArtistCard(Era.I));
        tribe.addCharacterCard(new BuilderCard(2, 1, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.II));

        assertEquals(3, tribe.countCharacterCards());
    }

    // =========================================================
    // countCharactersByType
    // =========================================================

    @Test
    void countCharactersByType_shouldReturnZero_whenTribeIsEmpty() {
        Tribe tribe = new Tribe();

        assertEquals(0, tribe.countCharactersByType(ArtistCard.class));
    }

    @Test
    void countCharactersByType_shouldReturnZero_whenTypeIsNotPresent() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new ArtistCard(Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));

        assertEquals(0, tribe.countCharactersByType(BuilderCard.class));
    }

    @Test
    void countCharactersByType_shouldCountOnlyCardsOfRequestedType() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new ArtistCard(Era.I));
        tribe.addCharacterCard(new ArtistCard(Era.II));
        tribe.addCharacterCard(new BuilderCard(2, 1, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.III));

        assertEquals(2, tribe.countCharactersByType(ArtistCard.class));
    }

    @Test
    void countCharactersByType_shouldHandleManyCardsOfSameType() {
        Tribe tribe = new Tribe();

        for (int i = 0; i < 20; i++) {
            tribe.addCharacterCard(new ArtistCard(Era.I));
        }

        assertEquals(20, tribe.countCharactersByType(ArtistCard.class));
    }

    // =========================================================
    // getBuildingDiscount
    // =========================================================

    @Test
    void getBuildingDiscount_shouldReturnZero_whenTribeIsEmpty() {
        Tribe tribe = new Tribe();

        assertEquals(0, tribe.getBuildingDiscount());
    }

    @Test
    void getBuildingDiscount_shouldReturnZero_whenThereAreNoBuilders() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new ArtistCard(Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.II));

        assertEquals(0, tribe.getBuildingDiscount());
    }

    @Test
    void getBuildingDiscount_shouldReturnSingleBuilderDiscount() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new BuilderCard(2, 1, Era.I));

        assertEquals(1, tribe.getBuildingDiscount());
    }

    @Test
    void getBuildingDiscount_shouldSumAllBuilderDiscounts() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new BuilderCard(2, 1, Era.I));
        tribe.addCharacterCard(new BuilderCard(3, 2, Era.II));
        tribe.addCharacterCard(new BuilderCard(4, 3, Era.III));

        assertEquals(6, tribe.getBuildingDiscount());
    }

    @Test
    void getBuildingDiscount_shouldIgnoreNonBuilderCards() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new BuilderCard(2, 1, Era.I));
        tribe.addCharacterCard(new ArtistCard(Era.II));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_2, Era.III));
        tribe.addCharacterCard(new BuilderCard(3, 2, Era.III));

        assertEquals(3, tribe.getBuildingDiscount());
    }

    @Test
    void getBuildingDiscount_shouldReturnZero_whenAllBuildersHaveZeroDiscount() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new BuilderCard(1, 0, Era.I));
        tribe.addCharacterCard(new BuilderCard(2, 0, Era.II));
        tribe.addCharacterCard(new BuilderCard(3, 0, Era.III));

        assertEquals(0, tribe.getBuildingDiscount());
    }

    @Test
    void getBuildingDiscount_shouldHandleVeryLargeDiscountSum() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new BuilderCard(1, 10, Era.I));
        tribe.addCharacterCard(new BuilderCard(1, 20, Era.II));
        tribe.addCharacterCard(new BuilderCard(1, 30, Era.III));

        assertEquals(60, tribe.getBuildingDiscount());
    }

    // =========================================================
    // countArtistPairs
    // =========================================================

    @Test
    void countArtistPairs_shouldReturnZero_whenTribeIsEmpty() {
        Tribe tribe = new Tribe();

        assertEquals(0, tribe.countArtistPairs());
    }

    @Test
    void countArtistPairs_shouldReturnZero_whenThereIsOneArtist() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new ArtistCard(Era.I));

        assertEquals(0, tribe.countArtistPairs());
    }

    @Test
    void countArtistPairs_shouldReturnOne_whenThereAreTwoArtists() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new ArtistCard(Era.I));
        tribe.addCharacterCard(new ArtistCard(Era.II));

        assertEquals(1, tribe.countArtistPairs());
    }

    @Test
    void countArtistPairs_shouldUseFloorDivision_whenThereAreThreeArtists() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new ArtistCard(Era.I));
        tribe.addCharacterCard(new ArtistCard(Era.II));
        tribe.addCharacterCard(new ArtistCard(Era.III));

        assertEquals(1, tribe.countArtistPairs());
    }

    @Test
    void countArtistPairs_shouldCountMultiplePairs() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new ArtistCard(Era.I));
        tribe.addCharacterCard(new ArtistCard(Era.I));
        tribe.addCharacterCard(new ArtistCard(Era.II));
        tribe.addCharacterCard(new ArtistCard(Era.III));
        tribe.addCharacterCard(new ArtistCard(Era.III));

        assertEquals(2, tribe.countArtistPairs());
    }

    @Test
    void countArtistPairs_shouldIgnoreNonArtistCards() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new ArtistCard(Era.I));
        tribe.addCharacterCard(new BuilderCard(2, 1, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));
        tribe.addCharacterCard(new ArtistCard(Era.II));

        assertEquals(1, tribe.countArtistPairs());
    }

    @Test
    void countArtistPairs_shouldHandleLargeOddNumberOfArtists() {
        Tribe tribe = new Tribe();

        for (int i = 0; i < 11; i++) {
            tribe.addCharacterCard(new ArtistCard(Era.I));
        }

        assertEquals(5, tribe.countArtistPairs());
    }

    // =========================================================
    // countDistinctInventorIcons
    // =========================================================

    @Test
    void countDistinctInventorIcons_shouldReturnZero_whenThereAreNoInventors() {
        Tribe tribe = new Tribe();

        assertEquals(0, tribe.countDistinctInventorIcons());
    }

    @Test
    void countDistinctInventorIcons_shouldReturnOne_whenThereIsOneInventor() {
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
        tribe.addCharacterCard(new InventorCard(5, Era.III, 2, InventorIcons.INVENTION_5));

        assertEquals(3, tribe.countDistinctInventorIcons());
    }

    @Test
    void countDistinctInventorIcons_shouldCountAllDifferentIcons() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        tribe.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_2));
        tribe.addCharacterCard(new InventorCard(3, Era.II, 2, InventorIcons.INVENTION_3));
        tribe.addCharacterCard(new InventorCard(4, Era.III, 2, InventorIcons.INVENTION_4));

        assertEquals(4, tribe.countDistinctInventorIcons());
    }

    @Test
    void countDistinctInventorIcons_shouldIgnoreNonInventorCards() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new ArtistCard(Era.I));
        tribe.addCharacterCard(new BuilderCard(2, 1, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_2, Era.II));

        assertEquals(1, tribe.countDistinctInventorIcons());
    }

    // =========================================================
    // countInventorPairs
    // =========================================================

    @Test
    void countInventorPairs_shouldReturnZero_whenTribeIsEmpty() {
        Tribe tribe = new Tribe();

        assertEquals(0, tribe.countInventorPairs());
    }

    @Test
    void countInventorPairs_shouldReturnZero_whenThereIsOneInventorOnly() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));

        assertEquals(0, tribe.countInventorPairs());
    }

    @Test
    void countInventorPairs_shouldCountOnePair_whenTwoInventorsHaveSameIcon() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.II));

        assertEquals(1, tribe.countInventorPairs());
    }

    @Test
    void countInventorPairs_shouldReturnZero_whenInventorsHaveDifferentIcons() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_2, Era.II));

        assertEquals(0, tribe.countInventorPairs());
    }

    @Test
    void countInventorPairs_shouldUseFloorDivisionForEachIconGroup() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.II));

        assertEquals(1, tribe.countInventorPairs());
    }

    @Test
    void countInventorPairs_shouldSumPairsAcrossDifferentIcons() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.II));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_2, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_2, Era.III));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_2, Era.III));

        assertEquals(2, tribe.countInventorPairs());
    }

    @Test
    void countInventorPairs_shouldIgnoreNonInventorCards() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new ArtistCard(Era.I));
        tribe.addCharacterCard(new BuilderCard(2, 1, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_3, Era.II));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_3, Era.III));

        assertEquals(1, tribe.countInventorPairs());
    }

    // =========================================================
    // countFullSets
    // =========================================================

    @Test
    void countFullSets_shouldReturnZero_whenTribeIsEmpty() {
        Tribe tribe = new Tribe();

        assertEquals(0, tribe.countFullSets());
    }

    @Test
    void countFullSets_shouldReturnZero_whenAtLeastOneTypeIsMissing() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new ArtistCard(Era.I));
        tribe.addCharacterCard(new BuilderCard(2, 1, Era.I));
        tribe.addCharacterCard(new HunterCard(false, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));

        assertEquals(0, tribe.countFullSets());
    }

    @Test
    void countFullSets_shouldReturnOne_whenThereIsExactlyOneOfEachType() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new ArtistCard(Era.I));
        tribe.addCharacterCard(new BuilderCard(2, 1, Era.I));
        tribe.addCharacterCard(new GathererCard( Era.I));
        tribe.addCharacterCard(new HunterCard(false, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));
        tribe.addCharacterCard(new ShamanCard(1, Era.I));

        assertEquals(1, tribe.countFullSets());
    }

    @Test
    void countFullSets_shouldReturnMinimumCountAmongAllTypes() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new ArtistCard(Era.I));
        tribe.addCharacterCard(new ArtistCard(Era.II));

        tribe.addCharacterCard(new BuilderCard(2, 1, Era.I));
        tribe.addCharacterCard(new BuilderCard(2, 1, Era.II));
        tribe.addCharacterCard(new BuilderCard(2, 1, Era.III));

        tribe.addCharacterCard(new GathererCard( Era.I));
        tribe.addCharacterCard(new GathererCard( Era.II));

        tribe.addCharacterCard(new HunterCard(false, Era.I));
        tribe.addCharacterCard(new HunterCard(false, Era.II));

        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_2, Era.II));

        tribe.addCharacterCard(new ShamanCard(1, Era.I));

        assertEquals(1, tribe.countFullSets());
    }

    @Test
    void countFullSets_shouldHandleMultipleCompleteSets() {
        Tribe tribe = new Tribe();

        for (int i = 0; i < 3; i++) {
            tribe.addCharacterCard(new ArtistCard(Era.I));
            tribe.addCharacterCard(new BuilderCard(2, 1, Era.I));
            tribe.addCharacterCard(new GathererCard( Era.I));
            tribe.addCharacterCard(new HunterCard(false, Era.I));
            tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));
            tribe.addCharacterCard(new ShamanCard(1, Era.I));
        }

        assertEquals(3, tribe.countFullSets());
    }

    // =========================================================
    // countShamanStars
    // =========================================================

    @Test
    void countShamanStars_shouldReturnZero_whenThereAreNoShamans() {
        Tribe tribe = new Tribe();

        assertEquals(0, tribe.countShamanStars());
    }

    @Test
    void countShamanStars_shouldReturnStarsOfSingleShaman() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new ShamanCard(3, Era.I));

        assertEquals(3, tribe.countShamanStars());
    }

    @Test
    void countShamanStars_shouldSumStarsOfAllShamans() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new ShamanCard(1, Era.I));
        tribe.addCharacterCard(new ShamanCard(2, Era.II));
        tribe.addCharacterCard(new ShamanCard(4, Era.III));

        assertEquals(7, tribe.countShamanStars());
    }

    @Test
    void countShamanStars_shouldIgnoreNonShamanCards() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new ShamanCard(2, Era.I));
        tribe.addCharacterCard(new ArtistCard(Era.I));
        tribe.addCharacterCard(new BuilderCard(2, 1, Era.II));

        assertEquals(2, tribe.countShamanStars());
    }

    // ------------------------------------------------------- //
    // getBuildingDiscount
    // ------------------------------------------------------- //

    @Test
    void getBuildingDiscountReturnsZeroWithNoBuilders() {
        Tribe tribe = new Tribe();
        assertEquals(0, tribe.getBuildingDiscount());
    }

    @Test
    void getBuildingDiscountSumsFoodDiscountOfAllBuilders() {
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new BuilderCard(1, Era.I, 2, 2, 1)); // foodDiscount = 1
        tribe.addCharacterCard(new BuilderCard(2, Era.I, 2, 3, 2)); // foodDiscount = 2
        assertEquals(3, tribe.getBuildingDiscount()); // 1 + 2
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
        // mancano Hunter, Inventor, Shaman
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
        // 2 artisti, 1 di tutto il resto → solo 1 set completo
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
        tribe.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_1)); // coppia 1
        tribe.addCharacterCard(new InventorCard(3, Era.I, 2, InventorIcons.INVENTION_2));
        tribe.addCharacterCard(new InventorCard(4, Era.I, 2, InventorIcons.INVENTION_2)); // coppia 2
        assertEquals(2, tribe.countInventorPairs());
    }

    @Test
    void countInventorPairs_shouldReturnOnePair_whenThreeSameIcon() {
        // 3 stessa icona → 1 coppia, 1 singolo
        Tribe tribe = new Tribe();
        tribe.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        tribe.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_1));
        tribe.addCharacterCard(new InventorCard(3, Era.I, 2, InventorIcons.INVENTION_1));
        assertEquals(1, tribe.countInventorPairs());
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
