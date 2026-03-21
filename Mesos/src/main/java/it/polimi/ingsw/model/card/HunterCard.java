package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

public class HunterCard extends CharacterCard{
    private final boolean foodIcon;

    public HunterCard(int id, boolean foodIcon, Era era, int minPlayers) {

        super(id, era, minPlayers);
        this.foodIcon = foodIcon;

    }

    public boolean hasFoodIcon() {
        return foodIcon;
    }

    // TODO: getExtraFood()
}
