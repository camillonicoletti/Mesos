package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.EventType;

import java.util.ArrayList;
import java.util.List;

public class ShamanicRitualEventCard extends EventCard {

    private final int prestigePointsForMost;
    private final int prestigePointsLostForLeast;

    public ShamanicRitualEventCard(Era era, boolean closeEvent,
                                   int prestigePointsForMost, int prestigePointsLostForLeast) {
        super(era, closeEvent, EventType.SHAMANIC_RITUAL);
        this.prestigePointsForMost = prestigePointsForMost;
        this.prestigePointsLostForLeast = prestigePointsLostForLeast;
    }

    public int getPrestigePointsForMost() {
        return prestigePointsForMost;
    }

    public int getPrestigePointsLostForLeast() {
        return prestigePointsLostForLeast;
    }

    @Override
    public void resolve(Game game) {
        List<Player> players = game.getPlayers();

        if (players == null || players.isEmpty()) {
            return;
        }

        int maxStars = Integer.MIN_VALUE;
        int minStars = Integer.MAX_VALUE;

        for (Player player : players) {
            int stars = player.getTribe().countShamanStars();
            maxStars = Math.max(maxStars, stars);
            minStars = Math.min(minStars, stars);
        }

        List<Player> playersWithMostStars = new ArrayList<>();
        List<Player> playersWithLeastStars = new ArrayList<>();

        for (Player player : players) {
            int stars = player.getTribe().countShamanStars();

            if (stars == maxStars) {
                playersWithMostStars.add(player);
            }

            if (stars == minStars) {
                playersWithLeastStars.add(player);
            }
        }

        for (Player player : playersWithMostStars) {
            player.addPrestigePoints(prestigePointsForMost);
        }

        for (Player player : playersWithLeastStars) {
            player.removePrestigePoints(prestigePointsLostForLeast);
        }
    }
}