package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.HunterCard;

public class Hunt1Food1PPEffect implements Effect{
    private static final long serialVersionUID = 1L;
    // During Hunt Event Only
    @Override
    public void apply(Player player) {
        int hunters = player.getTribe().countCharactersByType(HunterCard.class);
        player.addFood(hunters);
        player.addPrestigePoints(hunters);
    }

}
