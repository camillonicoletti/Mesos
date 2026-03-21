package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

public class ShamanCard extends CharacterCard{
    private final int stars;

    public ShamanCard(int id,  Era era, int stars, int minPlayers) {

        super(id, era, minPlayers);
        this.stars = stars;

    }

    public int getStars() {
        return stars;
    }
}
