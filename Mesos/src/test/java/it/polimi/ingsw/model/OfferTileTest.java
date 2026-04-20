package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.ActionSlot;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OfferTile.java
 *
 * Covers:
 * 1. placeTotem  — occupies the tile and updates TotemPosition to OFFER_TRACK
 * 2. removeTotem — frees the tile and returns the removed totem
 * 3. isOccupied  — reflects the current state correctly
 * 4. isAvailableFor — filters by minimum player count
 */
public class OfferTileTest {

    private OfferTile tile;
    private Totem totem;

    @BeforeEach
    void setUp() {
        tile = new OfferTile('A', 2, List.of(new ActionSlot("FOOD", 1)));
        totem = new Totem(TotemColor.RED, "TURN_ORDER");
    }

    // -------------------------------------------------------
    // placeTotem
    // -------------------------------------------------------

    @Test
    void testPlaceTotemOccupiesTile() {
        tile.placeTotem(totem);
        assertTrue(tile.isOccupied());
    }

    @Test
    void testPlaceTotemUpdatesTotemPositionToOfferTrack() {
        // totem starts in TURN_ORDER — after placing on OfferTile it must be OFFER_TRACK
        tile.placeTotem(totem);
        assertEquals("OFFER_TRACK", totem.getPosition());
    }

    @Test
    void testPlaceTotemStoresCorrectTotem() {
        tile.placeTotem(totem);
        assertEquals(totem, tile.getTotemSlot());
    }

    // -------------------------------------------------------
    // removeTotem
    // -------------------------------------------------------

    @Test
    void testRemoveTotemFreesToile() {
        tile.placeTotem(totem);
        tile.removeTotem();
        assertFalse(tile.isOccupied());
    }

    @Test
    void testRemoveTotemReturnsCorrectTotem() {
        tile.placeTotem(totem);
        Totem removed = tile.removeTotem();
        assertEquals(totem, removed);
    }

    @Test
    void testRemoveTotemFromEmptyTileReturnsNull() {
        assertNull(tile.removeTotem());
    }

    @Test
    void testRemoveTotemFromEmptyTileDoesNotThrow() {
        assertDoesNotThrow(() -> tile.removeTotem());
    }

    // -------------------------------------------------------
    // isAvailableFor
    // -------------------------------------------------------

    @Test
    void testIsAvailableForReturnsTrueWhenExactMinPlayers() {
        assertTrue(tile.isAvailableFor(2));
    }

    @Test
    void testIsAvailableForReturnsTrueWhenMoreThanMinPlayers() {
        assertTrue(tile.isAvailableFor(4));
    }

    @Test
    void testIsAvailableForReturnsFalseWhenFewerThanMinPlayers() {
        assertFalse(tile.isAvailableFor(1));
    }



// -------------------------------------------------------
// ActionSlot getters
// -------------------------------------------------------

    @Test
    void testActionSlotGetType() {
        ActionSlot slot = new ActionSlot("FOOD", 3);
        assertEquals("FOOD", slot.getType());
    }

    @Test
    void testActionSlotGetAmount() {
        ActionSlot slot = new ActionSlot("UPPER_ROW", 2);
        assertEquals(2, slot.getAmount());
    }

// -------------------------------------------------------
// OfferTile getters
// -------------------------------------------------------

    @Test
    void testOfferTileGetLetter() {
        OfferTile t = new OfferTile('C', 2, List.of());
        assertEquals('C', t.getLetter());
    }

    @Test
    void testOfferTileGetSlots() {
        ActionSlot slot = new ActionSlot("FOOD", 2);
        OfferTile t = new OfferTile('A', 2, List.of(slot));
        assertEquals(1, t.getSlots().size());
        assertEquals("FOOD", t.getSlots().get(0).getType());
        assertEquals(2, t.getSlots().get(0).getAmount());
    }

}
