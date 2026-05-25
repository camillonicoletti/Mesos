package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Player;
import java.io.Serializable;

public interface TurnSlotEffect extends Serializable {
    void apply(Player player);
}