package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * Building effect that gives food for newly completed Inventor pairs.
 * It remembers already rewarded pairs so the same pair is not paid twice.
 */
public class InventorPairFoodEffect implements Effect{
    private static final long serialVersionUID = 1L;
    private int rewardedPairs = 0;

    /**
     * Stores how many pairs already existed when the building was gained.
     */
    public void setRewardedPairs(int rewardedPairs) {
        this.rewardedPairs = rewardedPairs;
    }

    /**
     * Gives food only for Inventor pairs completed after the last reward.
     */
    @Override
    public void apply(Player player) {
        // rewardedPairs prevents already rewarded pairs from giving food again.
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
