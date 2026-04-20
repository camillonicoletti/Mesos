package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Player;

public class PayFoodSlotEffect implements TurnSlotEffect {

    private static final long serialVersionUID = 1L;

    private final int foodCost;
    private final int penaltyPP;

    public PayFoodSlotEffect(int foodCost, int penaltyPP) {
        this.foodCost = foodCost;
        this.penaltyPP = penaltyPP;
    }

    @Override
    public void apply(Player player) {
        player.applyTurnPenalty(foodCost, penaltyPP);
    }
}