package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.card.effect.EffectVisitor;
import it.polimi.ingsw.model.enums.Era;

/**
 * Building card that a player can buy with food.
 * It may have an effect and always has a fixed end-game point value.
 */
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

    /**
     * Lets a visitor handle the building effect, if the building has one.
     */
    public <R> R acceptEffect(EffectVisitor<R> visitor) {
        if (effect == null) {
            return visitor.visitEffect(null);
        }
        return effect.accept(visitor);
    }

    public int getPointsEndOfMatch() {
        return pointsEndOfMatch;
    }

    /**
     * Sends this card to the building branch of the card visitor.
     */
    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitBuilding(this);
    }

}
