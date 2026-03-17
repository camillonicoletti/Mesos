package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.enums.SlotType;

public class ActionSlot {
    private SlotType type;
    private int amount;

    public ActionSlot(SlotType type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    public SlotType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }
}











