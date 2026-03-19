package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.EventType;

public class CavePaintingsEventCard extends EventCard {

    private final int minimumArtistsRequired;
    private final int prestigePointsLostIfBelow;
    private final int prestigePointsPerArtistIfAbove;

    public CavePaintingsEventCard(Era era, boolean closeEvent, int priorityValue,
                                  int minimumArtistsRequired,
                                  int prestigePointsLostIfBelow,
                                  int prestigePointsPerArtistIfAbove) {
        super(era, closeEvent, priorityValue, EventType.ROCK_PAINTINGS);
        this.minimumArtistsRequired = minimumArtistsRequired;
        this.prestigePointsLostIfBelow = prestigePointsLostIfBelow;
        this.prestigePointsPerArtistIfAbove = prestigePointsPerArtistIfAbove;
    }

    public int getMinimumArtistsRequired() {
        return minimumArtistsRequired;
    }

    public int getPrestigePointsLostIfBelow() {
        return prestigePointsLostIfBelow;
    }

    public int getPrestigePointsPerArtistIfAbove() {
        return prestigePointsPerArtistIfAbove;
    }

    @Override
    public void resolve(Game game) {
        for (Player player : game.getPlayers()) {
            int artists = player.getTribe().countCharactersByType(ArtistCard.class);

            if (artists < minimumArtistsRequired) {
                player.removePrestigePoints(prestigePointsLostIfBelow);
            } else {
                player.addPrestigePoints(artists * prestigePointsPerArtistIfAbove);
            }
        }
    }
}