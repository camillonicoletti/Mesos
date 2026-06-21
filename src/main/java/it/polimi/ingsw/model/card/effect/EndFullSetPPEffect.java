package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * End-game effect that gives 6 points for each complete character set.
 */
public class EndFullSetPPEffect implements EndGameEffect {
    private static final long serialVersionUID = 1L;

    /**
     * Counts full sets and converts them into points.
     */
    @Override
    public int calculatePoints(Player player) {
        return player.getTribe().countFullSets() * 6;
    }

    /**
     * Adds the calculated end-game points to the player.
     */
    @Override
    public void apply(Player player) {
        player.addPrestigePoints(calculatePoints(player));
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitEndFullSetPP(this);
    }
}
