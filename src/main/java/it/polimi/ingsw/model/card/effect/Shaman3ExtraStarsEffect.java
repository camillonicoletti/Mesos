package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * Passive effect that adds 3 shaman stars during Shamanic Ritual ranking.
 */
public class Shaman3ExtraStarsEffect implements Effect {
    private static final long serialVersionUID = 1L;

    /**
     * Returns the extra stars added by this building.
     */
    public int getExtraStars() {
        return 3;
    }

    /**
     * Does nothing directly because ShamanicRitualEventCard reads the bonus.
     */
    @Override
    public void apply(Player player) {
        // Passive: ShamanicRitualEventCard reads getExtraStars() directly.
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitShaman3ExtraStars(this);
    }
}
