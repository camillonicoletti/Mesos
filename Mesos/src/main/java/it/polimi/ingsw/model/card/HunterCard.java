package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.CharacterType;

public class HunterCard extends CharacterCard{
    private final boolean foodIcon;

    public HunterCard(boolean foodIcon) {
        super(CharacterType.HUNTER);
        this.foodIcon = foodIcon;
    }

    public boolean hasFoodIcon() {
        return foodIcon;
    }

    // TODO: getExtraFood()
}
