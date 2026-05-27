package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * End-game effect that grants 6 PP for every full set (one character of each
 * of the six archetypes) present in the tribe.
 */
public class EndFullSetPPEffect implements EndGameEffect {
    private static final long serialVersionUID = 1L;
    @Override
    public int calculatePoints(Player player) {
        return player.getTribe().countFullSets() * 6;
    }

    @Override
    public void apply(Player player) {
        player.addPrestigePoints(calculatePoints(player));
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitEndFullSetPP(this);
    }
}
