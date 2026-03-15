package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public class EndPPEffect implements Effect {

    private final int prestigePoints;

    public EndPPEffect(int prestigePoints) {
        this.prestigePoints = prestigePoints;
    }

    @Override
    public void apply(Player player){
        player.addPrestigePoints(prestigePoints);
    }
}
