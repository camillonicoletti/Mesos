package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.effect.CavePaintingsFoodEffect;
import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.card.effect.EffectVisitor;
import it.polimi.ingsw.model.enums.Era;


public class CavePaintingsEventCard extends EventCard {

    private static final long serialVersionUID = 1L;
    private final int minArtists;
    private final int ppPenalty;
    private final int ppPerArtist;

    public CavePaintingsEventCard(int id, Era era, boolean closeEvent,
                                  int minArtists,
                                  int ppPenalty,
                                  int ppPerArtist) {
        super(id, era, closeEvent);

        this.minArtists = minArtists;
        this.ppPenalty = ppPenalty;
        this.ppPerArtist = ppPerArtist;
    }

    public int getMinArtists() {
        return minArtists;
    }

    public int getPpPenalty() {
        return ppPenalty;
    }

    public int getPpPerArtists() {
        return ppPerArtist;
    }

    @Override
    public void resolve(Game game) {
        for (Player player : game.getPlayers()) {
            int artists = player.getTribe().countCharactersByType(ArtistCard.class);

            if (artists < minArtists) {
                player.removePrestigePoints(ppPenalty);
            } else {
                player.addPrestigePoints(artists * ppPerArtist);
            }

            // Apply building bonuses triggered by the Cave Paintings event.
            for (BuildingCard building : player.getTribe().getBuildingCards()) {
                applyCavePaintingsEffect(player, building);
            }
        }
    }

    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitCavePaintingsEvent(this);
    }

    private void applyCavePaintingsEffect(Player player, BuildingCard building) {
        building.acceptEffect(new EffectVisitor<Void>() {
            @Override public Void visitCavePaintingsFood(CavePaintingsFoodEffect effect) {
                effect.apply(player);
                return null;
            }

            @Override public Void visitEffect(Effect effect) {
                return null;
            }
        });
    }

}
