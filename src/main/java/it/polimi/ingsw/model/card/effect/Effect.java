package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

import java.io.Serializable;

/**
 * Behavior attached to a building card.
 * Some effects apply immediately, while passive effects are read by events or scoring through EffectVisitor.
 */
public interface Effect extends Serializable {

    /**
     * Applies the effect directly when the effect has active behavior.
     */
    void apply(Player player);

    /**
     * Lets a visitor read this effect without using instanceof chains.
     */
    default <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitEffect(this);
    }

}
