package it.polimi.ingsw.model;

import it.polimi.ingsw.model.enums.SlotEffect;

public class TurnSlot {

    private int position;
    private SlotEffect effect;
    private int effectValue;
    private int penaltyPoints;
    private Totem occupiedBy;

    public TurnSlot(int position, SlotEffect effect, int effectValue, int penaltyPoints) {
        this.position = position;
        this.effect = effect;
        this.effectValue = effectValue;
        this.penaltyPoints = penaltyPoints;
        this.occupiedBy = null;
    }

    public int getPosition() {
        return position;
    }

    public SlotEffect getEffect() {
        return effect;
    }

    public int getEffectValue() {
        return effectValue;
    }

    public int getPenaltyPoints() {
        return penaltyPoints;
    }

    public boolean isOccupied() {
        return occupiedBy != null;
    }

    public Totem getOccupiedBy() {
        return occupiedBy;
    }

    public void placeTotem(Totem t) {
        this.occupiedBy = t;
    }

    public Totem removeTotem() {
        Totem removed = this.occupiedBy;
        this.occupiedBy = null;
        return removed;
    }
}
