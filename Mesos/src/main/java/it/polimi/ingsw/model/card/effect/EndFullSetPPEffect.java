package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public class EndFullSetPPEffect implements EndGameEffect {

    @Override
    public int calculatePoints(Player player) {
        return player.getTribe().countFullSets() * 6;
    }

    @Override
    public void apply(Player player) {
        player.addPrestigePoints(calculatePoints(player));
    }
}
