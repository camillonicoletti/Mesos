package it.polimi.ingsw.model.card;

public class BuildingCard extends PlayerCard{
    private final int foodCost;
    private final BuildingEffect effect;

    public BuildingCard(int foodCost, int pointsEndOfMatch, BuildingEffect effect) {
        super(pointsEndOfMatch);
        this.foodCost = foodCost;
        this.effect = effect;
    }

    public int getFoodCost() {
        return foodCost;
    }

    public BuildingEffect getEffect() {
        return effect;
    }

}
