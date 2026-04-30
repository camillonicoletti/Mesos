package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

import java.io.Serializable;

public abstract class Card implements Serializable {
    private static final long serialVersionUID = 1L;
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
