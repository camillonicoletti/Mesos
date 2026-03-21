package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public class EndFullSetPPEffect implements EndGameEffect{

    @Override
    public void apply(Player player) {

        int sets = player.getTribe().countFullSets();
        player.addPrestigePoints(sets * 6);

    }
}
