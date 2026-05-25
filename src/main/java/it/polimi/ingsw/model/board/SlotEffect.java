package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.card.Card;

import java.io.Serializable;
import java.util.List;

public interface SlotEffect extends Serializable {

    void resolve(Player player, int amount, List<Card> chosenCards, Round round);
}