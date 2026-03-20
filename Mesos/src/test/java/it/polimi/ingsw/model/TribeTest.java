package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.InventorIcons;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));

        assertEquals(1, tribe.countDistinctInventorIcons());
    }

    @Test
    void countDistinctInventorIcons_shouldCountOnlyDifferentIcons() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_3, Era.II));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_5, Era.III));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_5, Era.III));

        assertEquals(3, tribe.countDistinctInventorIcons());
    }

    @Test
    void countDistinctInventorIcons_shouldCountAllDifferentIcons() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_2, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_3, Era.II));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_4, Era.III));

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
}