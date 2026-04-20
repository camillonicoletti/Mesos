package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.GainFoodSlotEffect;
import it.polimi.ingsw.model.board.NoSlotEffect;
import it.polimi.ingsw.model.board.PayFoodSlotEffect;
import it.polimi.ingsw.model.board.TurnSlot;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.enums.TotemPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TurnSlotTest {

    private TurnSlot slot;
    private Totem totem;

    @BeforeEach
    void setUp() {
        slot = new TurnSlot(1, new GainFoodSlotEffect(2));
        totem = new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK);
    }

    @Test
    void testPlaceTotemOccupiesSlot() {
        slot.placeTotem(totem);
        assertTrue(slot.isOccupied());
    }

    @Test
    void testPlaceTotemUpdatesTotemPositionToTurnOrder() {
        slot.placeTotem(totem);
        assertEquals(TotemPosition.TURN_ORDER, totem.getPosition());
    }

    @Test
    void testPlaceTotemStoresCorrectTotem() {
        slot.placeTotem(totem);
        assertEquals(totem, slot.getOccupiedBy());
    }

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