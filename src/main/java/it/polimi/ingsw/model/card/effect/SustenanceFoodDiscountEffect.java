package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.CharacterCard;

/**
 * Passive effect used during Sustenance.
 * It reduces required food based on how many target characters the player owns.
 */
public class SustenanceFoodDiscountEffect implements Effect{
    private static final long serialVersionUID = 1L;
    private final Class<? extends CharacterCard> targetType;

    public SustenanceFoodDiscountEffect(Class<? extends CharacterCard> targetType) {
        this.targetType = targetType;
    }

    /**
     * Returns the character type used to calculate the discount.
     */
    public Class<? extends CharacterCard> getTargetType() {
        return targetType;
    }

    /**
     * Does nothing directly because SustenanceEventCard reads the target type.
     */
    @Override
    public void apply(Player player) {
        // Passive: SustenanceEventCard reads getTargetType() to compute the food discount.
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitSustenanceFoodDiscount(this);
    }
}
