package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

/**
 * Character used during Cave Paintings events (where minimum-artist thresholds
 * decide PP gain vs. penalty) and at end-of-match scoring through pairs.
 */
public class ArtistCard extends CharacterCard{
    private static final long serialVersionUID = 1L;
    public ArtistCard(int id, Era era, int minPlayers) {

        super(id, era, minPlayers);

    }

    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitArtist(this);
    }

}
