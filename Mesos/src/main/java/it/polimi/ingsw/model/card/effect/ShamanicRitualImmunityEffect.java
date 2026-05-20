package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public class ShamanicRitualImmunityEffect implements Effect{
    private static final long serialVersionUID = 1L;
    @Override
    public void apply(Player player)
    {
        // Passive effect, managed in ShamanicRitual
        // by skipping points removal
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitShamanicRitualImmunity(this);
    }
}
