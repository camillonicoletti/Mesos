package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.CharacterCard;

/**
 * Passive building effect: during a Sustenance event, reduces the food
 * required by one for every character of the configured {@code targetType}
 * in the tribe.
 */
public class SustenanceFoodDiscountEffect implements Effect{
    private static final long serialVersionUID = 1L;
    private final Class<? extends CharacterCard> targetType;

    public SustenanceFoodDiscountEffect(Class<? extends CharacterCard> targetType) {
        this.targetType = targetType;
    }

    public Class<? extends CharacterCard> getTargetType() {
        return targetType;
    }

    @Override
    public void apply(Player player) {
        // Passive: apply() is a no-op; SustenanceEventCard reads getTargetType() to compute the food discount.
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitSustenanceFoodDiscount(this);
    }
}
