package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

public class BuilderCard extends CharacterCard{
    private final int foodDiscount;
    private final int pointsEndOfMatch;

    public BuilderCard(int id, int pointsEndOfMatch, int foodDiscount, Era era,
                       int minPlayers) {
        super(id, era, minPlayers);
        this.foodDiscount = foodDiscount;
        this.pointsEndOfMatch = pointsEndOfMatch;
    }

    public int getFoodDiscount() {
        return foodDiscount;
    }

    public int getPointsEndOfMatch() {
        return pointsEndOfMatch;
    }

}
