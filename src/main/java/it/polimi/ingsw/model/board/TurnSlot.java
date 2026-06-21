package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Totem;

import java.io.Serializable;

/**
 * Represents one position in the turn-order track.
 * A slot can hold one totem and can apply a reward or penalty.
 */
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

    /**
     * Places a totem in this turn-order position.
     */
    public void placeTotem(Totem t) {
        this.occupiedBy = t;
        t.setPosition("TURN_ORDER");
    }

    /**
     * Removes the totem from this position and returns it.
     */
    public Totem removeTotem() {
        Totem removed = this.occupiedBy;
        this.occupiedBy = null;
        if (removed != null) removed.setPosition("OFFER_TRACK");
        return removed;
    }
}
