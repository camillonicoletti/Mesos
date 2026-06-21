package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.ArtistCard;

/**
 * Building effect used during Cave Paintings.
 * It gives one food for each Artist in the player's tribe.
 */
public class CavePaintingsFoodEffect implements Effect{
    private static final long serialVersionUID = 1L;

    /**
     * Adds food based on the number of Artist cards owned by the player.
     */
    @Override
    public void apply(Player player) {
        int artists = player.getTribe().countCharactersByType(ArtistCard.class);
        player.addFood(artists);
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitCavePaintingsFood(this);
    }

}
