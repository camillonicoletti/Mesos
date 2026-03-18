package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.InventorCard;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.InventorIcons;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TribeTest {

    @Test
    void countDistinctInventorIcons_shouldReturnZero_whenThereAreNoInventors() {
        Tribe tribe = new Tribe();

        int result = tribe.countDistinctInventorIcons();

        assertEquals(0, result);
    }

    @Test
    void countDistinctInventorIcons_shouldReturnOne_whenThereIsOneInventor() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));

        int result = tribe.countDistinctInventorIcons();

        assertEquals(1, result);
    }

    @Test
    void countDistinctInventorIcons_shouldCountOnlyDifferentIcons() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_3, Era.II));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_5, Era.III));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_5, Era.III));

        int result = tribe.countDistinctInventorIcons();

        assertEquals(3, result);
    }

    @Test
    void countDistinctInventorIcons_shouldCountAllDifferentIcons() {
        Tribe tribe = new Tribe();

        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_1, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_2, Era.I));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_3, Era.II));
        tribe.addCharacterCard(new InventorCard(InventorIcons.INVENTION_4, Era.III));

        int result = tribe.countDistinctInventorIcons();

        assertEquals(4, result);
    }
}