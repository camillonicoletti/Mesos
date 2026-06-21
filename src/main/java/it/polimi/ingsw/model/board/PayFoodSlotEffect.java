package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Player;

import java.io.Serializable;

/**
 * Turn-order effect that asks the player to pay food or lose points.
 */
public class PayFoodSlotEffect implements TurnSlotEffect, Serializable {

    private static final long serialVersionUID = 1L;

    private final int foodCost;
    private final int penaltyPP;

    public PayFoodSlotEffect(int foodCost, int penaltyPP) {
        this.foodCost = foodCost;
        this.penaltyPP = penaltyPP;
    }

    /**
     * Applies the food cost, or the prestige penalty if the player cannot pay.
     */
    @Override
    public void apply(Player player) {
        player.applyTurnPenalty(foodCost, penaltyPP);
    }
}
