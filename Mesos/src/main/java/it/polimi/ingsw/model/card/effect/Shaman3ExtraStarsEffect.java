package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public class Shaman3ExtraStarsEffect implements Effect {

    private final int extraStars = 3;

    public int getExtraStars() {
        return extraStars;
    }

    @Override
    public void apply(Player player) {
        // Passive effect managed in Game
    }
}