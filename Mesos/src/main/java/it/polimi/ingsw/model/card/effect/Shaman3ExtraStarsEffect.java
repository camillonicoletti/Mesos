package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public class Shaman3ExtraStarsEffect implements Effect {

    public int getExtraStars() {
        return 3;
    }

    @Override
    public void apply(Player player) {
        // Passive effect managed in ShamanicRitual
        // by doing stars += effect.getExtraStars()
    }
}