package it.polimi.ingsw.model.card;

public abstract class PlayerCard {
    private final int pointsEndOfMatch;

    public PlayerCard() {
        this.pointsEndOfMatch = 0;
    }

    public PlayerCard(int pointsEndOfMatch) {
        this.pointsEndOfMatch = pointsEndOfMatch;
    }

    public int getPointsEndOfMatch() {
        return pointsEndOfMatch;
    }
}
