package it.polimi.ingsw.model.board;

import java.io.Serializable;

/**
 * Represents one action printed on an offer tile.
 * It stores the effect type and how many times or cards that effect allows.
 */
public class ActionSlot implements Serializable {
    private static final long serialVersionUID = 1L;
    private SlotEffect effect;
    private int amount;

    public ActionSlot(SlotEffect effect, int amount) {
        this.effect = effect;
        this.amount = amount;
    }

    public SlotEffect getEffect() {
        return effect;
    }

    public int getAmount() {
        return amount;
    }
}
