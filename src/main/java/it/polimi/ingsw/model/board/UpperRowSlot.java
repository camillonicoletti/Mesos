package it.polimi.ingsw.model.board;

/**
 * Action slot that lets the player take cards from the upper row.
 */
public class UpperRowSlot implements SlotEffect {
    private static final long serialVersionUID = 1L;

    /**
     * Sends this slot to the upper-row branch of the visitor.
     */
    @Override
    public <R> R accept(SlotEffectVisitor<R> visitor) {
        return visitor.visitUpperRow(this);
    }
}
