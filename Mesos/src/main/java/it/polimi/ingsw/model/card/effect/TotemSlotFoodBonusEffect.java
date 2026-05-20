package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

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
