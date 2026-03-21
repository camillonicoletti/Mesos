package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.CharacterCard;

public class SustenanceFoodDiscountEffect implements Effect{
    private final Class<? extends CharacterCard> targetType;

    public SustenanceFoodDiscountEffect(Class<? extends CharacterCard> targetType) {
        this.targetType = targetType;
    }

    public Class<? extends CharacterCard> getTargetType() {
        return targetType;
    }

    @Override
    public void apply(Player player) {
        // Managed in SustenanceEventCard by adding sum of targetType
        // to foodDiscount
    }
}
