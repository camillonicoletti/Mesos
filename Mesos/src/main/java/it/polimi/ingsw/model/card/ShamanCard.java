package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.CharacterType;

public class ShamanCard extends CharacterCard{
    private final int stars;

    public ShamanCard(int stars) {
        super(CharacterType.SHAMAN);
        this.stars = stars;
    }

    public int getStars() {
        return stars;
    }
}
