package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Totem;

import java.io.Serializable;

public class TurnSlot implements Serializable {

    private static final long serialVersionUID = 1L;
    private final int position;
    private final TurnSlotEffect effect;
    private Totem occupiedBy;

    public TurnSlot(int position, TurnSlotEffect effect) {
        this.position = position;
        this.effect = effect;
        this.occupiedBy = null;
    }

    public int getPosition() {
        return position;
    }

    public TurnSlotEffect getEffect() {
        return effect;
    }

    public boolean isOccupied() {
        return occupiedBy != null;
    }

    public Totem getOccupiedBy() {
        return occupiedBy;
    }

    public void placeTotem(Totem t) {
        this.occupiedBy = t;
        t.setPosition("TURN_ORDER");
    }

    public Totem removeTotem() {
        Totem removed = this.occupiedBy;
        this.occupiedBy = null;
        if (removed != null) removed.setPosition("OFFER_TRACK");
        return removed;
    }
}