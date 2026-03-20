package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public class TotemSlotFoodBonusEffect implements Effect{

    @Override
    public void apply(Player player){
        player.addFood(1);
    }

}
