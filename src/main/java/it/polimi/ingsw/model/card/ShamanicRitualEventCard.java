package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.card.effect.EffectVisitor;
import it.polimi.ingsw.model.card.effect.Shaman3ExtraStarsEffect;
import it.polimi.ingsw.model.card.effect.ShamanicRitualDoublePPEffect;
import it.polimi.ingsw.model.card.effect.ShamanicRitualImmunityEffect;
import it.polimi.ingsw.model.enums.Era;

import java.util.ArrayList;
import java.util.List;

/**
 * Event that ranks players by shaman stars.
 * Players with most stars gain points, players with least stars lose points,
 * and building effects can add stars, double rewards or give immunity.
 */
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

    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitShamanicRitualEvent(this);
    }

    /**
     * Counts a player's shaman stars, including building bonuses.
     */
    private int countStarsForPlayer(Player player) {
        int stars = player.getTribe().countShamanStars();
        for (BuildingCard building : player.getTribe().getBuildingCards()) {
            stars += extraStarsFrom(building);
        }
        return stars;
    }

    /**
     * Resolves the ritual by finding the players with most and least shaman stars.
     */
    @Override
    public void resolve(Game game) {
        List<Player> players = game.getPlayers();

        if (players == null || players.isEmpty()) {
            return;
        }

        // Count stars for each player, including building bonuses.
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

        // Players with most stars gain points; a building can double this reward.
        for (Player player : playersWithMostStars) {
            int pp = ppGain;

            for (BuildingCard building : player.getTribe().getBuildingCards()) {
                if (doublesShamanicRitualPoints(building)) {
                    pp *= 2;
                }
            }

            player.addPrestigePoints(pp);
        }

        // Players with least stars lose points.
        // Immunity works only if the player really has fewer stars than someone else.
        boolean allPlayersTied = maxStars == minStars;
        for (Player player : playersWithLeastStars) {
            if (!allPlayersTied && hasShamanicRitualImmunity(player)) {
                continue;
            }
            player.removePrestigePoints(ppLoss);
        }
    }

    /**
     * Reads extra shaman stars from a building effect.
     */
    private int extraStarsFrom(BuildingCard building) {
        return building.acceptEffect(new EffectVisitor<>() {
            @Override public Integer visitShaman3ExtraStars(Shaman3ExtraStarsEffect effect) {
                return effect.getExtraStars();
            }

            @Override public Integer visitEffect(Effect effect) {
                return 0;
            }
        });
    }

    /**
     * Checks if a building doubles the ritual reward.
     */
    private boolean doublesShamanicRitualPoints(BuildingCard building) {
        return Boolean.TRUE.equals(building.acceptEffect(new EffectVisitor<>() {
            @Override public Boolean visitShamanicRitualDoublePP(ShamanicRitualDoublePPEffect effect) {
                return true;
            }

            @Override public Boolean visitEffect(Effect effect) {
                return false;
            }
        }));
    }

    /**
     * Checks if the player is protected from the ritual penalty.
     */
    private boolean hasShamanicRitualImmunity(Player player) {
        for (BuildingCard building : player.getTribe().getBuildingCards()) {
            if (Boolean.TRUE.equals(building.acceptEffect(new EffectVisitor<>() {
                @Override public Boolean visitShamanicRitualImmunity(ShamanicRitualImmunityEffect effect) {
                    return true;
                }

                @Override public Boolean visitEffect(Effect effect) {
                    return false;
                }
            }))) {
                return true;
            }
        }
        return false;
    }
}
