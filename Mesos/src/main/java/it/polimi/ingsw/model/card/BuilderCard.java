package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.CharacterType;

public class BuilderCard extends CharacterCard{
    private final int foodDiscount;

    public BuilderCard(int pointsEndOfMatch, int foodDiscount) {
        super(pointsEndOfMatch, CharacterType.BUILDER);
        this.foodDiscount = foodDiscount;
    }
}
