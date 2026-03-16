package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

public class BuilderCard extends CharacterCard{
    private final int foodDiscount;

    public BuilderCard(int pointsEndOfMatch, int foodDiscount, Era era) {
        super(pointsEndOfMatch, era);
        this.foodDiscount = foodDiscount;
    }

    public int getFoodDiscount() {
        return foodDiscount;
    }
}
