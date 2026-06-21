package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

/**
 * Character used for Cave Paintings events and artist-pair end-game scoring.
 */
public class ArtistCard extends CharacterCard{
    private static final long serialVersionUID = 1L;
    public ArtistCard(int id, Era era, int minPlayers) {

        super(id, era, minPlayers);

    }

    /**
     * Sends this card to the artist branch of the visitor.
     */
    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitArtist(this);
    }

}
