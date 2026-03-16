package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public class InventorPairFoodEffect implements Effect{
    private int rewardedPairs = 0;

    @Override
    public void apply(Player player) {
        int currentPairs = player.getTribe().countInventorPairs();
        int newPairs = currentPairs - rewardedPairs;

        if (newPairs > 0) {
            player.addFood(newPairs * 3);
            rewardedPairs = currentPairs;
        }
    }
}
