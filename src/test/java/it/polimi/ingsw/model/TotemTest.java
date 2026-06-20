package it.polimi.ingsw.model;

import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Totem (model).
 *
 * Verifies totem color, position track and equality semantics.
 */
public class TotemTest {

    @Test
    void testConstructorAndGetColor() {
        Totem totem = new Totem(TotemColor.RED, "OFFER_TRACK");
        assertEquals(TotemColor.RED, totem.getColor());
    }

    @Test
    void testConstructorAndGetPosition() {
        Totem totem = new Totem(TotemColor.BLUE, "TURN_ORDER");
        assertEquals("TURN_ORDER", totem.getPosition());
    }

    @Test
    void testSetPosition() {
        Totem totem = new Totem(TotemColor.RED, "OFFER_TRACK");
        totem.setPosition("TURN_ORDER");
        assertEquals("TURN_ORDER", totem.getPosition());
    }

    @Test
    void testColorDoesNotChangeAfterSetPosition() {
        Totem totem = new Totem(TotemColor.PURPLE, "OFFER_TRACK");
        totem.setPosition("TURN_ORDER");
        assertEquals(TotemColor.PURPLE, totem.getColor());
    }
}