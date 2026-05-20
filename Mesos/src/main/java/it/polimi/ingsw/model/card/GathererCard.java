package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

public class GathererCard extends CharacterCard{
    private static final long serialVersionUID = 1L;
    public GathererCard(int id, Era era, int minPlayers) {

        super(id, era, minPlayers);

    }

    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitGatherer(this);
    }

}
