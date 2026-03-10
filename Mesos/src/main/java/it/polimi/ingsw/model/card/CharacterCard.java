package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.CharacterType;

public abstract class CharacterCard extends PlayerCard{
    private final CharacterType characterType;

    public CharacterCard(CharacterType characterType) {
        super();
        this.characterType = characterType;
    }

    public CharacterCard(int pointsEndOfMatch, CharacterType characterType) {
        super(pointsEndOfMatch);
        this.characterType = characterType;
    }

    public CharacterType getCharacterType() {
        return characterType;
    }
}
