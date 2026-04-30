package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Player;

import java.io.Serializable;

public class NoSlotEffect implements TurnSlotEffect, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public void apply(Player player) {
        // nessun effetto
    }
}
