package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.CharacterCard;

public class SustenanceFoodDiscountEffect implements Effect{
    private final Class<? extends CharacterCard> targetType;

    public SustenanceFoodDiscountEffect(Class<? extends CharacterCard> targetType) {
        this.targetType = targetType;
    }

    @Override
    public void apply(Player player) {
        int count = player.getTribe().countCharactersByType(targetType);
        player.addFood(count);
    }
}
