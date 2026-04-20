package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.card.Card;

import java.util.List;

public class FoodSlot implements SlotEffect {
    @Override
    public void resolve(Player player, int amount, List<Card> chosenCards, Round round) {
        player.addFood(amount);
    }
}