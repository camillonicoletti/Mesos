package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public class EndFullSetPPEffect implements Effect{

    private final int ppPerSet;

    public EndFullSetPPEffect(int ppPerSet) {
        this.ppPerSet = ppPerSet;
    }

    @Override
    public void apply(Player player) {

        int sets = player.getTribe().countFullSets();
        player.addPrestigePoints(sets * ppPerSet);

    }
}
