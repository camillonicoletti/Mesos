package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public class Shaman3ExtraStarsEffect implements Effect {
    private static final long serialVersionUID = 1L;
    public int getExtraStars() {
        return 3;
    }

    @Override
    public void apply(Player player) {
        // Passive: apply() is a no-op; ShamanicRitualEventCard reads getExtraStars() directly.
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitShaman3ExtraStars(this);
    }
}
