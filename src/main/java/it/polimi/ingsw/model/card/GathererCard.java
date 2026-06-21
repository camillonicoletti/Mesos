package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

/**
 * Character that reduces the food needed during Sustenance events.
 */
public class GathererCard extends CharacterCard{
    private static final long serialVersionUID = 1L;
    public GathererCard(int id, Era era, int minPlayers) {

        super(id, era, minPlayers);

    }

    /**
     * Sends this card to the gatherer branch of the visitor.
     */
    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitGatherer(this);
    }

}
