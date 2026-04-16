package it.polimi.ingsw.controller.action;

import java.io.Serializable;

public class PlaceTotemAction implements GameAction, Serializable {

    private final char tileId;

    public PlaceTotemAction(char tileId) {
        this.tileId = tileId;
    }

    @Override
    public String getType() {
        return "PLACE_TOTEM";
    }

    public char getTileId() {
        return tileId;
    }
}