package it.polimi.ingsw.model.card;

public class BuilderCard extends CharacterCard{
    private final int foodDiscount;

    public BuilderCard(int pointsEndOfMatch, int foodDiscount) {
        super(pointsEndOfMatch);
        this.foodDiscount = foodDiscount;
    }

    public int getFoodDiscount() {
        return foodDiscount;
    }
}
