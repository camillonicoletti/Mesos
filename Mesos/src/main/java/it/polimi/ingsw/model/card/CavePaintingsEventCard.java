package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.enums.Era;

public class CavePaintingsEventCard extends EventCard {

    private final int minimumArtistsRequired;
    private final int prestigePointsLostIfBelowThreshold;
    private final int prestigePointsPerArtistIfAboveThreshold;

    public CavePaintingsEventCard(Era era, boolean closeEvent, int priorityValue,
                                  int minimumArtistsRequired,
                                  int prestigePointsLostIfBelowThreshold,
                                  int prestigePointsPerArtistIfAboveThreshold) {
        super(era, closeEvent, priorityValue);
        this.minimumArtistsRequired = minimumArtistsRequired;
        this.prestigePointsLostIfBelowThreshold = prestigePointsLostIfBelowThreshold;
        this.prestigePointsPerArtistIfAboveThreshold = prestigePointsPerArtistIfAboveThreshold;
    }

    public int getMinimumArtistsRequired() {
        return minimumArtistsRequired;
    }

    public int getPrestigePointsLostIfBelowThreshold() {
        return prestigePointsLostIfBelowThreshold;
    }

    public int getPrestigePointsPerArtistIfAboveThreshold() {
        return prestigePointsPerArtistIfAboveThreshold;
    }

    @Override
    public void apply(Game game) {
        for (Player player : game.getPlayers()) {
            int artists = player.getTribe().countCharactersByType(ArtistCard.class);

            if (artists < minimumArtistsRequired) {
                player.removePrestigePoints(prestigePointsLostIfBelowThreshold);
            } else {
                player.addPrestigePoints(artists * prestigePointsPerArtistIfAboveThreshold);
            }
        }
    }
}