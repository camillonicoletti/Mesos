package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Player;

import java.io.Serializable;

/**
 * Turn-order effect that gives food to the player.
 */
public class GainFoodSlotEffect implements TurnSlotEffect, Serializable {

    private static final long serialVersionUID = 1L;

    private final int amount;

    public GainFoodSlotEffect(int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    /**
     * Adds the configured amount of food.
     */
    @Override
    public void apply(Player player) {
        player.addFood(amount);
    }

    @Override
    public boolean triggersTotemFoodBonus() {
        return true;
    }
}
