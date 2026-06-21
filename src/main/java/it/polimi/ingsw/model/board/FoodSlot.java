package it.polimi.ingsw.model.board;

/**
 * Action slot that gives food to the player.
 */
public class FoodSlot implements SlotEffect {
    private static final long serialVersionUID = 1L;

    /**
     * Sends this slot to the food branch of the visitor.
     */
    @Override
    public <R> R accept(SlotEffectVisitor<R> visitor) {
        return visitor.visitFood(this);
    }
}
