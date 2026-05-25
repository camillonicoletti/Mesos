package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.InventorIcons;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CharacterCardTest {

    // -------------------------------------------------------
    // HunterCard
    // -------------------------------------------------------

    @Test
    void hunterCard_hasFoodIcon_returnsTrue_whenConstructedWithTrue() {
        HunterCard card = new HunterCard(1, Era.I, 2, true);
        assertTrue(card.hasFoodIcon());
    }

    @Test
    void hunterCard_hasFoodIcon_returnsFalse_whenConstructedWithFalse() {
        HunterCard card = new HunterCard(1, Era.I, 2, false);
        assertFalse(card.hasFoodIcon());
    }

    @Test
    void hunterCard_getEra_returnsCorrectEra() {
        HunterCard card = new HunterCard(1, Era.II, 2, false);
        assertEquals(Era.II, card.getEra());
    }

    // -------------------------------------------------------
    // ShamanCard
    // -------------------------------------------------------

    @Test
    void shamanCard_getStars_returnsCorrectValue() {
        ShamanCard card = new ShamanCard(1, Era.I, 2, 3);
        assertEquals(3, card.getStars());
    }

    @Test
    void shamanCard_getStars_returnsZeroWhenZeroStars() {
        ShamanCard card = new ShamanCard(1, Era.I, 2, 0);
        assertEquals(0, card.getStars());
    }

    // -------------------------------------------------------
    // BuilderCard
    // -------------------------------------------------------

    @Test
    void builderCard_getPointsEndOfMatch_returnsCorrectValue() {
        BuilderCard card = new BuilderCard(1, Era.I, 2, 5, 2);
        assertEquals(5, card.getPointsEndOfMatch());
    }

    @Test
    void builderCard_getFoodDiscount_returnsCorrectValue() {
        BuilderCard card = new BuilderCard(1, Era.I, 2, 5, 2);
        assertEquals(2, card.getFoodDiscount());
    }

    @Test
    void builderCard_getPointsEndOfMatch_returnsZeroWhenZero() {
        BuilderCard card = new BuilderCard(1, Era.I, 2, 0, 0);
        assertEquals(0, card.getPointsEndOfMatch());
        assertEquals(0, card.getFoodDiscount());
    }

    // -------------------------------------------------------
    // ArtistCard
    // -------------------------------------------------------

    @Test
    void artistCard_constructorAndGetEra() {
        ArtistCard card = new ArtistCard(1, Era.II, 2);
        assertNotNull(card);
        assertEquals(Era.II, card.getEra());
    }

    // -------------------------------------------------------
    // GathererCard
    // -------------------------------------------------------

    @Test
    void gathererCard_constructorAndGetEra() {
        GathererCard card = new GathererCard(1, Era.III, 2);
        assertNotNull(card);
        assertEquals(Era.III, card.getEra());
    }

    // -------------------------------------------------------
    // InventorCard
    // -------------------------------------------------------

    @Test
    void inventorCard_getInventionIcon_returnsCorrectIcon() {
        InventorCard card = new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_3);
        assertEquals(InventorIcons.INVENTION_3, card.getInventionIcon());
    }
}
