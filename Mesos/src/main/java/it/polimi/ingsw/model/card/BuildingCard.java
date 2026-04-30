package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.enums.Era;

public class BuildingCard extends Card{
    private static final long serialVersionUID = 1L;
    private final int foodCost;
    private final Effect effect;
    private final int pointsEndOfMatch;

    public BuildingCard(int id, Era era, int foodCost, Effect effect, int pointsEndOfMatch) {
        super(id, era);
        this.foodCost = foodCost;
        this.effect = effect;
        this.pointsEndOfMatch = pointsEndOfMatch;
    }

    public int getFoodCost() {
        return foodCost;
    }

    public Effect getEffect() {
        return effect;
    }

    public int getPointsEndOfMatch() {
        return pointsEndOfMatch;
    }

}
