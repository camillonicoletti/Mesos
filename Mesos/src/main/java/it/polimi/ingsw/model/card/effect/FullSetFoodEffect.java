package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public class FullSetFoodEffect implements Effect{
    private final int foodPerSet;
    private int rewardedSets = 0;

    public FullSetFoodEffect(int foodPerSet) {
        this.foodPerSet = foodPerSet;
    }

    @Override
    public void apply(Player player) {

        int currentSets = player.getTribe().countFullSets();
        int newSets = currentSets - rewardedSets;

        // Only rewards new sets
        if (newSets > 0) {
            player.addFood(newSets * foodPerSet);
            rewardedSets = currentSets;
        }
    }

}
