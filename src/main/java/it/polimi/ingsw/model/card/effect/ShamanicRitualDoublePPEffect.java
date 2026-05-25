package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

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
