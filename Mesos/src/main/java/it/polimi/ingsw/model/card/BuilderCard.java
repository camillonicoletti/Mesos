package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

public class BuilderCard extends CharacterCard{
    private static final long serialVersionUID = 1L;
    private final int foodDiscount;
    private final int pointsEndOfMatch;

    public BuilderCard(int id, Era era, int minPlayers,
                       int pointsEndOfMatch, int foodDiscount) {
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

    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitBuilder(this);
    }

}
