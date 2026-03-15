package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.ArtistCard;

public class RockPaintingsEffect implements Effect{
    private final int foodPerArtist;
    private final int ppPerArtist;

    public RockPaintingsEffect(int foodPerArtist, int ppPerArtist) {
        this.foodPerArtist= foodPerArtist;
        this.ppPerArtist = ppPerArtist;
    }

    @Override
    public void apply(Player player) {
        int artists = player.getTribe().countCharactersByType(ArtistCard.class);
        player.addFood(artists * foodPerArtist);
        player.addPrestigePoints(artists * ppPerArtist);
    }
}
