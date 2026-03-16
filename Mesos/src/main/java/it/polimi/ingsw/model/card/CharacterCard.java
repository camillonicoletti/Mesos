package it.polimi.ingsw.model.card;


import it.polimi.ingsw.model.enums.Era;

public abstract class CharacterCard extends PlayerCard {

    public CharacterCard(Era era) {
        super(era);
    }

    public CharacterCard(int pointsEndOfMatch, Era era) {
        super(pointsEndOfMatch, era);
    }

}