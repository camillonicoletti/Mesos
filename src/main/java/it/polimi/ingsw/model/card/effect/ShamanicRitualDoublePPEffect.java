package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * Passive marker effect: during a Shamanic Ritual, doubles the PP gained by
 * the owning player when they end up among the top-ranked. Applies even when
 * every player is tied.
 */
public class ShamanicRitualDoublePPEffect implements Effect{
    private static final long serialVersionUID = 1L;
    @Override
    public void apply(Player player) {
        // Passive: apply() is a no-op; ShamanicRitualEventCard doubles the PP amount before awarding.
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitShamanicRitualDoublePP(this);
    }
}
