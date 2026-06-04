package it.polimi.ingsw.controller.action;

import java.io.Serializable;

/**
 * Command: the player places their totem on the offer tile identified by
 * {@code tileId} during the totem-placement phase.
 */
public class PlaceTotemAction implements GameAction {

    private final char tileId;

    public PlaceTotemAction(char tileId) {
        this.tileId = tileId;
    }

    public char getTileId() {
        return tileId;
    }
}