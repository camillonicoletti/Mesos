package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public class EndPPEffect implements EndGameEffect {

    @Override
    public int calculatePoints(Player player) {
        return 25;
    }

    @Override
    public void apply(Player player) {
        player.addPrestigePoints(calculatePoints(player));
    }
}
