package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * Passive effect that doubles points gained from a Shamanic Ritual reward.
 * It also works when all players are tied.
 */
public class ShamanicRitualDoublePPEffect implements Effect{
    private static final long serialVersionUID = 1L;
    /**
     * Does nothing directly because the event doubles the reward when it reads this effect.
     */
    @Override
    public void apply(Player player) {
        // Passive: ShamanicRitualEventCard doubles the PP amount before awarding.
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitShamanicRitualDoublePP(this);
    }
}
