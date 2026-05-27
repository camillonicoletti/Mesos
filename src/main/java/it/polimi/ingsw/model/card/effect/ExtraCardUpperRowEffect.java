package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * Passive marker effect: presence of a building with this effect lets the
 * action-resolution phase grant the owning player one extra pick from the
 * upper row.
 */
public class ExtraCardUpperRowEffect implements Effect{
    private static final long serialVersionUID = 1L;
    @Override
    public void apply(Player player){
        // Passive: apply() is a no-op; the extra pick is granted by ActionResolutionState.startExtraPickPhase().
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitExtraCardUpperRow(this);
    }

}
