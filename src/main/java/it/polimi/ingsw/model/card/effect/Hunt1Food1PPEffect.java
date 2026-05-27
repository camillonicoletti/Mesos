package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.HunterCard;

/**
 * Building effect triggered by the Hunt event: grants 1 extra food and 1
 * extra PP per Hunter in the tribe on top of the event base reward.
 */
public class Hunt1Food1PPEffect implements Effect{
    private static final long serialVersionUID = 1L;
    // During Hunt Event Only
    @Override
    public void apply(Player player) {
        int hunters = player.getTribe().countCharactersByType(HunterCard.class);
        player.addFood(hunters);
        player.addPrestigePoints(hunters);
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitHunt1Food1PP(this);
    }

}
