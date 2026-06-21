package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.HunterCard;

/**
 * Building effect used during Hunt.
 * It gives one extra food and one extra point for each Hunter in the tribe.
 */
public class Hunt1Food1PPEffect implements Effect{
    private static final long serialVersionUID = 1L;

    /**
     * Adds the extra Hunt reward based on Hunter count.
     */
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
