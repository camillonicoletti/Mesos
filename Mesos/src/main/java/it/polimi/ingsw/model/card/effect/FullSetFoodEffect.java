package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public class FullSetFoodEffect implements Effect{

    private int rewardedSets = 0;

       @Override
    public void apply(Player player) {

        int currentSets = player.getTribe().countFullSets();
        int newSets = currentSets - rewardedSets;

        // Only rewards new sets
        if (newSets > 0) {
            player.addFood(newSets * 5);
            rewardedSets = currentSets;
        }
    }

}
