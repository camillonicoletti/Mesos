package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public class EndPPEffect implements Effect {

    @Override
    public void apply(Player player){
        player.addPrestigePoints(25);
    }

}
