package it.polimi.ingsw.controller.action;

import java.io.Serializable;

/**
 * Command: the player buys the building identified by {@code cardId} from
 * either the upper or lower row, paying its discounted food cost.
 */
public class PickBuildingAction implements GameAction {

    private static final long serialVersionUID = 1L;

    private final String cardId;
    private final boolean fromUpperRow;

    public PickBuildingAction(String cardId, boolean fromUpperRow) {
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