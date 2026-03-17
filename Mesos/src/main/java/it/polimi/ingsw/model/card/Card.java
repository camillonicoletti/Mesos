package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

public abstract class Card {

    private final Era era;

    public Card(Era era) {
        this.era = era;
    }

    public Era getEra() {
        return era;
    }
}
