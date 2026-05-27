package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

/**
 * Character that contributes shaman stars used to rank players during a
 * Shamanic Ritual event. The top-ranked players gain PP, the bottom-ranked
 * lose PP (subject to immunity effects).
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

    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitShaman(this);
    }
}
