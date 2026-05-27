package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * Building effect triggered when the owner places their totem on a food-reward
 * turn-order slot: grants 1 extra food on top of the slot's base payout.
 */
public class TotemSlotFoodBonusEffect implements Effect{
    private static final long serialVersionUID = 1L;
    @Override
    public void apply(Player player){
        player.addFood(1);
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitTotemSlotFoodBonus(this);
    }

}
