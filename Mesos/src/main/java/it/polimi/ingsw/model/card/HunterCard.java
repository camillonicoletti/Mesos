package it.polimi.ingsw.model.card;

public class HunterCard extends CharacterCard{
    private final boolean foodIcon;

    public HunterCard(boolean foodIcon) {

        this.foodIcon = foodIcon;
    }

    public boolean hasFoodIcon() {
        return foodIcon;
    }

    // TODO: getExtraFood()
}
