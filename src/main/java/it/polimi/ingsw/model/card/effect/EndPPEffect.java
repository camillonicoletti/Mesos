package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/** End-game effect that grants a flat 25 PP bonus regardless of tribe composition. */
public class EndPPEffect implements EndGameEffect {
    private static final long serialVersionUID = 1L;
    @Override
    public int calculatePoints(Player player) {
        return 25;
    }

    @Override
    public void apply(Player player) {
        player.addPrestigePoints(calculatePoints(player));
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitEndPP(this);
    }
}
