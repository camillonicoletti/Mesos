package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.effect.Shaman3ExtraStarsEffect;
import it.polimi.ingsw.model.card.effect.ShamanicRitualDoublePPEffect;
import it.polimi.ingsw.model.card.effect.ShamanicRitualImmunityEffect;
import it.polimi.ingsw.model.enums.Era;


import java.util.ArrayList;
import java.util.List;

public class ShamanicRitualEventCard extends EventCard {
    private static final long serialVersionUID = 1L;
    private final int ppGain;
    private final int ppLoss;

    public ShamanicRitualEventCard(int id, Era era, boolean closeEvent,
                                   int ppGain, int ppLoss) {
        super(id, era, closeEvent);
        this.ppGain = ppGain;
        this.ppLoss = ppLoss;
    }

    public int getPpGain() {
        return ppGain;
    }

    public int getPpLoss() {
        return ppLoss;
    }

    // counts shaman stars for a player, adding 3 extra if they have Shaman3ExtraStarsEffect
    private int countStarsForPlayer(Player player) {
        int stars = player.getTribe().countShamanStars();
        for (BuildingCard building : player.getTribe().getBuildingCards()) {
            if (building.getEffect() instanceof Shaman3ExtraStarsEffect effect) {
                stars += effect.getExtraStars();
            }
        }
        return stars;
    }

    @Override
    public void resolve(Game game) {
        List<Player> players = game.getPlayers();

        if (players == null || players.isEmpty()) {
            return;
        }

        // count stars for each player including Shaman3ExtraStarsEffect bonus
        int maxStars = Integer.MIN_VALUE;
        int minStars = Integer.MAX_VALUE;

        for (Player player : players) {
            int stars = countStarsForPlayer(player);
            maxStars = Math.max(maxStars, stars);
            minStars = Math.min(minStars, stars);
        }

        List<Player> playersWithMostStars = new ArrayList<>();
        List<Player> playersWithLeastStars = new ArrayList<>();

        for (Player player : players) {
            int stars = countStarsForPlayer(player);
            if (stars == maxStars) playersWithMostStars.add(player);
            if (stars == minStars) playersWithLeastStars.add(player);
        }

        // award PP to players with most stars — doubled only if strictly alone at the top
        boolean soloLeader = playersWithMostStars.size() == 1;
        for (Player player : playersWithMostStars) {
            int pp = ppGain;
            if (soloLeader) {
                for (BuildingCard building : player.getTribe().getBuildingCards()) {
                    if (building.getEffect() instanceof ShamanicRitualDoublePPEffect) {
                        pp *= 2;
                    }
                }
            }
            player.addPrestigePoints(pp);
        }

        // remove PP from players with least stars — unless they have ShamanicRitualImmunityEffect
        for (Player player : playersWithLeastStars) {
            boolean immune = false;
            for (BuildingCard building : player.getTribe().getBuildingCards()) {
                if (building.getEffect() instanceof ShamanicRitualImmunityEffect) {
                    immune = true;
                    break;
                }
            }
            if (!immune) {
                player.removePrestigePoints(ppLoss);
            }
        }
    }
}
