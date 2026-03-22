package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

public abstract class Card {

    private final int id;
    private final Era era;

    public Card(int id, Era era) {
        this.id = id;
        this.era = era;
    }

    public int getId() {
        return id;
    }

    public Era getEra() {
        return era;
    }

}
