package it.polimi.ingsw.model.board;

import java.io.Serializable;

/**
 * Common behavior for action slots on offer tiles.
 * Each implementation represents one kind of action printed on a tile.
 */
public interface SlotEffect extends Serializable {

    /**
     * Lets a visitor handle this slot effect.
     */
    default <R> R accept(SlotEffectVisitor<R> visitor) {
        return visitor.visitEffect(this);
    }
}
