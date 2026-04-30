package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.ArtistCard;

public class CavePaintingsFoodEffect implements Effect{
    private static final long serialVersionUID = 1L;
    // During Cave Paintings Event Only
    @Override
    public void apply(Player player) {
        int artists = player.getTribe().countCharactersByType(ArtistCard.class);
        player.addFood(artists);
    }

}
