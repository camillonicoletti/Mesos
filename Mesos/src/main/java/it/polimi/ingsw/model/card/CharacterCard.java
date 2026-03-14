package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.CharacterType;

public abstract class CharacterCard extends PlayerCard {

    private CharacterType characterType;

    public CharacterCard() {
        super();
    }

    public CharacterCard(int pointsEndOfMatch) {
        super(pointsEndOfMatch);
    }

    public CharacterType getType() {
        return characterType;
    }
}