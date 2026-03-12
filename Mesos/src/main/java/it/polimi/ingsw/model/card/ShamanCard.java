package it.polimi.ingsw.model.card;

public class ShamanCard extends CharacterCard{
    private final int stars;

    public ShamanCard(int stars) {

        this.stars = stars;

    }

    public int getStars() {
        return stars;
    }
}
