package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

import java.io.Serializable;

/**
 * Behaviour attached to a {@link it.polimi.ingsw.model.card.BuildingCard}.
 * Most effects are passive: their {@link #apply(Player)} is a no-op, and the
 * actual rule is read by the event/scoring code via the {@link EffectVisitor}.
 * The visitor dispatch lets new effects be added without touching callers.
 */
public interface Effect extends Serializable {

    void apply(Player player);

    default <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitEffect(this);
    }

}
