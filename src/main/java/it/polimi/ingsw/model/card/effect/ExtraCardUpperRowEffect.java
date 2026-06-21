package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * Passive effect that allows one extra pick from the upper row.
 * The action-resolution phase checks if the player owns this effect.
 */
public class ExtraCardUpperRowEffect implements Effect{
    private static final long serialVersionUID = 1L;
    /**
     * Does nothing directly because the extra pick is handled by the controller state.
     */
    @Override
    public void apply(Player player){
        // Passive: the extra pick is granted by ActionResolutionState.startExtraPickPhase().
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitExtraCardUpperRow(this);
    }

}
