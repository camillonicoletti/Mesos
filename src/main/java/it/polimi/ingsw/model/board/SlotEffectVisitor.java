package it.polimi.ingsw.model.board;

/**
 * Visitor for offer-tile slot effects.
 * It lets callers handle food, upper-row and lower-row slots without instanceof checks.
 *
 * @param <R> result type produced by the visit
 */
public interface SlotEffectVisitor<R> {

    default R visitFood(FoodSlot effect) {
        return visitEffect(effect);
    }

    default R visitUpperRow(UpperRowSlot effect) {
        return visitEffect(effect);
    }

    default R visitLowerRow(LowerRowSlot effect) {
        return visitEffect(effect);
    }

    R visitEffect(SlotEffect effect);
}
