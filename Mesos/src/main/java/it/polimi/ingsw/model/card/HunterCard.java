package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

public class HunterCard extends CharacterCard{
    private final boolean foodIcon;

    public HunterCard(boolean foodIcon, Era era) {

        super(era);
        this.foodIcon = foodIcon;

    }

    public boolean hasFoodIcon() {
        return foodIcon;
    }

    // TODO: getExtraFood()
}
