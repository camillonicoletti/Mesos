package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * Passive marker effect: protects the owning player from PP loss during a
 * Shamanic Ritual. Bypassed when every player is tied in stars, since the
 * rule requires the player to have strictly fewer icons than someone else.
 */
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
