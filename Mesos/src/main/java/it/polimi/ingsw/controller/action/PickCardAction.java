package it.polimi.ingsw.controller.action;

import java.io.Serializable;

public class PickCardAction implements GameAction, Serializable {

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