package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * Passive effect that protects the player from Shamanic Ritual point loss.
 * It is ignored when every player is tied in stars.
 */
public class ShamanicRitualImmunityEffect implements Effect{
    private static final long serialVersionUID = 1L;
    /**
     * Does nothing directly because the ritual event checks this protection.
     */
    @Override
    public void apply(Player player)
    {
        // Passive: ShamanicRitualEventCard skips PP removal for immune players.
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitShamanicRitualImmunity(this);
    }
}
