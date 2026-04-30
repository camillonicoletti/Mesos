package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public class ShamanicRitualDoublePPEffect implements Effect{
    private static final long serialVersionUID = 1L;
    @Override
    public void apply(Player player) {
        // Passive effect managed in ShamanicRitual resolve
        // by adding points twice
    }
}
