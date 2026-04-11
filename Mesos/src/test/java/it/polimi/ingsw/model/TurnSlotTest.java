package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.TurnSlot;
import it.polimi.ingsw.model.enums.SlotEffect;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.enums.TotemPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TurnSlot.java
 *
 * Covers:
 * 1. placeTotem  — occupies the slot and updates TotemPosition to TURN_ORDER
 * 2. removeTotem — frees the slot, returns the totem, resets TotemPosition to OFFER_TRACK
 * 3. isOccupied  — reflects the current state correctly
 */
public class TurnSlotTest {

    private TurnSlot slot;
    private Totem totem;

    @BeforeEach
    void setUp() {
        slot = new TurnSlot(1, SlotEffect.GAIN_FOOD, 2, 0);
        totem = new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK);
    }

    // -------------------------------------------------------
    // placeTotem
    // -------------------------------------------------------

    @Test
    void testPlaceTotemOccupiesSlot() {
        slot.placeTotem(totem);
        assertTrue(slot.isOccupied());
    }

    @Test
    void testPlaceTotemUpdatesTotemPositionToTurnOrder() {
        // totem starts in OFFER_TRACK — after placing on TurnSlot it must be TURN_ORDER
        slot.placeTotem(totem);
        assertEquals(TotemPosition.TURN_ORDER, totem.getPosition());
    }

    @Test
    void testPlaceTotemStoresCorrectTotem() {
        slot.placeTotem(totem);
        assertEquals(totem, slot.getOccupiedBy());
    }

    // -------------------------------------------------------
    // removeTotem
    // -------------------------------------------------------

    @Test
    void testRemoveTotemFreesSlot() {
        slot.placeTotem(totem);
        slot.removeTotem();
        assertFalse(slot.isOccupied());
    }

    @Test
    void testRemoveTotemReturnsCorrectTotem() {
        slot.placeTotem(totem);
        Totem removed = slot.removeTotem();
        assertEquals(totem, removed);
    }

    @Test
    void testRemoveTotemResetsPositionToOfferTrack() {
        // after being removed from TurnSlot, totem must go back to OFFER_TRACK
        slot.placeTotem(totem);
        slot.removeTotem();
        assertEquals(TotemPosition.OFFER_TRACK, totem.getPosition());
    }

    @Test
    void testRemoveTotemFromEmptySlotReturnsNull() {
        assertNull(slot.removeTotem());
    }

    @Test
    void testRemoveTotemFromEmptySlotDoesNotThrow() {
        assertDoesNotThrow(() -> slot.removeTotem());
    }
}
