package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * End-game effect that gives a fixed 25 point bonus.
 */
public class EndPPEffect implements EndGameEffect {
    private static final long serialVersionUID = 1L;

    /**
     * Returns the fixed bonus value.
     */
    @Override
    public int calculatePoints(Player player) {
        return 25;
    }

    /**
     * Adds the fixed bonus to the player.
     */
    @Override
    public void apply(Player player) {
        player.addPrestigePoints(calculatePoints(player));
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitEndPP(this);
    }
}
