package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * Building effect that grants 3 food per newly completed inventor pair
 * (two inventors sharing the same icon). The {@code rewardedPairs} counter
 * prevents the same pair from being paid twice.
 */
public class InventorPairFoodEffect implements Effect{
    private static final long serialVersionUID = 1L;
    private int rewardedPairs = 0;

    public void setRewardedPairs(int rewardedPairs) {
        this.rewardedPairs = rewardedPairs;
    }

    @Override
    public void apply(Player player) {
        // rewardedPairs prevents already rewarded pairs from granting food again.
        int currentPairs = player.getTribe().countInventorPairs();
        int newPairs = currentPairs - rewardedPairs;

        if (newPairs > 0) {
            player.addFood(newPairs * 3);
            rewardedPairs = currentPairs;
        }
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitInventorPairFood(this);
    }
}
