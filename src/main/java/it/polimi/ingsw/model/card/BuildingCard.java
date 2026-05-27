package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.card.effect.EffectVisitor;
import it.polimi.ingsw.model.enums.Era;

/**
 * A building card players acquire by paying food. Carries an attached
 * {@link Effect} (typically passive, triggered during specific events or at
 * end-of-game) and a fixed end-of-match point value added to the final score.
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

    public <R> R acceptEffect(EffectVisitor<R> visitor) {
        if (effect == null) {
            return visitor.visitEffect(null);
        }
        return effect.accept(visitor);
    }

    public int getPointsEndOfMatch() {
        return pointsEndOfMatch;
    }

    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitBuilding(this);
    }

}
