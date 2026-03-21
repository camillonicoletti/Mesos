package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

public abstract class Card {

    private final int id;
    private final Era era;
    private final int minPlayers;

    public Card(int id, Era era, int minPlayers) {
        this.id = id;
        this.era = era;
        this.minPlayers = minPlayers;
    }

    public int getId() {
        return id;
    }

    public Era getEra() {
        return era;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

}
