package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * Building effect used when the player lands on a food turn-order slot.
 * It adds one extra food to the slot reward.
 */
public class TotemSlotFoodBonusEffect implements Effect{
    private static final long serialVersionUID = 1L;

    /**
     * Adds the extra food bonus.
     */
    @Override
    public void apply(Player player){
        player.addFood(1);
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitTotemSlotFoodBonus(this);
    }

}
