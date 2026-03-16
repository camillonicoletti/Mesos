package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

public abstract class PlayerCard {

    private final int pointsEndOfMatch;
    private final Era era;

    public PlayerCard(Era era) {

        this.pointsEndOfMatch = 0;
        this.era = era;

    }

    public PlayerCard(int pointsEndOfMatch, Era era) {

        this.pointsEndOfMatch = pointsEndOfMatch;
        this.era = era;

    }

    public int getPointsEndOfMatch() {
        return pointsEndOfMatch;
    }

    public Era getEra() {
        return era;
    }
}
