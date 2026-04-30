package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.card.Card;

import java.io.Serializable;
import java.util.List;

public class ActionSlot implements Serializable {
    private static final long serialVersionUID = 1L;
    private SlotEffect effect;
    private int amount;

    public ActionSlot(SlotEffect effect, int amount) {
        this.effect = effect;
        this.amount = amount;
    }

    public SlotEffect getEffect() {
        return effect;
    }

    public void resolve(Player player, List<Card> chosenCards, Round round) {
        effect.resolve(player, amount, chosenCards, round);
    }

    public int getAmount() {
        return amount;
    }
}