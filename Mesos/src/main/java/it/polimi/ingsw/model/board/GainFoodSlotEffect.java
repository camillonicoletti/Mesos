package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Player;

public class GainFoodSlotEffect implements TurnSlotEffect {

    private static final long serialVersionUID = 1L;

    private final int amount;

    public GainFoodSlotEffect(int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public void apply(Player player) {
        player.addFood(amount);
    }
}