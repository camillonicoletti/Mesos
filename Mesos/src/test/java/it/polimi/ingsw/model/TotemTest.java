package it.polimi.ingsw.model;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.enums.TotemPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TotemTest {

    @Test
    void testConstructorAndGetColor() {
        Totem totem = new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK);
        assertEquals(TotemColor.RED, totem.getColor());
    }

    @Test
    void testConstructorAndGetPosition() {
        Totem totem = new Totem(TotemColor.BLUE, TotemPosition.TURN_ORDER);
        assertEquals(TotemPosition.TURN_ORDER, totem.getPosition());
    }

    @Test
    void testSetPosition() {
        Totem totem = new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK);
        totem.setPosition(TotemPosition.TURN_ORDER);
        assertEquals(TotemPosition.TURN_ORDER, totem.getPosition());
    }

    @Test
    void testColorDoesNotChangeAfterSetPosition() {
        Totem totem = new Totem(TotemColor.PURPLE, TotemPosition.OFFER_TRACK);
        totem.setPosition(TotemPosition.TURN_ORDER);
        assertEquals(TotemColor.PURPLE, totem.getColor());
    }
}
