package it.polimi.ingsw.model.board;

/**
 * Action slot that lets the player take cards from the lower row.
 */
public class LowerRowSlot implements SlotEffect {
    private static final long serialVersionUID = 1L;

    /**
     * Sends this slot to the lower-row branch of the visitor.
     */
    @Override
    public <R> R accept(SlotEffectVisitor<R> visitor) {
        return visitor.visitLowerRow(this);
    }
}
