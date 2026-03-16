package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.enums.Era;

public class BuildingCard extends PlayerCard{
    private final int foodCost;
    private final Effect effect;

    public BuildingCard(int foodCost, int pointsEndOfMatch, Effect effect, Era era) {
        super(pointsEndOfMatch, era);
        this.foodCost = foodCost;
        this.effect = effect;
    }

    public int getFoodCost() {
        return foodCost;
    }

    public Effect getEffect() {
        return effect;
    }

}
