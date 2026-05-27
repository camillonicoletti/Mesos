package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * Building effect that grants 5 food per newly completed full set. Tracks the
 * number of sets already rewarded to avoid paying twice for the same set when
 * the tribe composition changes.
 */
public class FullSetFoodEffect implements Effect{

    private static final long serialVersionUID = 1L;
    private int rewardedSets = 0;

    public void setRewardedSets(int rewardedSets) {
        this.rewardedSets = rewardedSets;
    }

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

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitFullSetFood(this);
    }

}
