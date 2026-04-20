package it.polimi.ingsw.model.board;

public class ActionSlot {
    private String type;
    private int amount;

    public ActionSlot(String type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }
}






