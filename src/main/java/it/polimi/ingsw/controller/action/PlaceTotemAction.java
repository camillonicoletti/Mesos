package it.polimi.ingsw.controller.action;

public class PlaceTotemAction implements GameAction {

    private static final long serialVersionUID = 1L;

    private final char tileId;

    public PlaceTotemAction(char tileId) {
        this.tileId = tileId;
    }

    public char getTileId() {
        return tileId;
    }
}