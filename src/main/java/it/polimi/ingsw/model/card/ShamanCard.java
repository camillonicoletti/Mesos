package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

/**
 * Character with shaman stars.
 * Shamanic Ritual events rank players using these stars.
 */
public class ShamanCard extends CharacterCard{
    private static final long serialVersionUID = 1L;
    private final int stars;

    public ShamanCard(int id,  Era era, int minPlayers, int stars) {

        super(id, era, minPlayers);
        this.stars = stars;

    }

    public int getStars() {
        return stars;
    }

    /**
     * Sends this card to the shaman branch of the visitor.
     */
    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitShaman(this);
    }
}
