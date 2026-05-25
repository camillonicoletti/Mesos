package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public class ShamanicRitualImmunityEffect implements Effect{
    private static final long serialVersionUID = 1L;
    @Override
    public void apply(Player player)
    {
        // Passive: apply() is a no-op; ShamanicRitualEventCard skips PP removal for immune players.
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitShamanicRitualImmunity(this);
    }
}
