package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * Building effect that gives food for newly completed full sets.
 * It remembers already rewarded sets so the same set is not paid twice.
 */
public class FullSetFoodEffect implements Effect{

    private static final long serialVersionUID = 1L;
    private int rewardedSets = 0;

    /**
     * Stores how many sets were already completed when the building was gained.
     */
    public void setRewardedSets(int rewardedSets) {
        this.rewardedSets = rewardedSets;
    }

    /**
     * Gives food only for full sets completed after the last reward.
     */
    @Override
    public void apply(Player player) {

        int currentSets = player.getTribe().countFullSets();
        int newSets = currentSets - rewardedSets;

        // Only reward sets that were not rewarded before.
        if (newSets > 0) {
            player.addFood(newSets * 5);
            rewardedSets = currentSets;
        }
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitFullSetFood(this);
    }

}
