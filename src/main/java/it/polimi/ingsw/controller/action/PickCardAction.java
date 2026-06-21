package it.polimi.ingsw.controller.action;

public class PickCardAction implements GameAction{

    private static final long serialVersionUID = 1L;

    private final String cardId;
    private final boolean fromUpperRow;

    public PickCardAction(String cardId, boolean fromUpperRow) {
        this.cardId = cardId;
        this.fromUpperRow = fromUpperRow;
    }

    public String getCardId() {
        return cardId;
    }

    public boolean isFromUpperRow() {
        return fromUpperRow;
    }
}