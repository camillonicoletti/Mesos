package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

public abstract class CharacterCard extends Card {

    private final int minPlayers;

    public CharacterCard(int id, Era era, int minPlayers) {
        super(id, era);
        this.minPlayers = minPlayers;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public boolean isAvailableFor(int numPlayers) {
        return numPlayers >= minPlayers;
    }

}