package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Player;

import java.io.Serializable;

/**
 * Turn-order effect used when a slot has no reward or penalty.
 */
public class NoSlotEffect implements TurnSlotEffect, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Does nothing by design.
     */
    @Override
    public void apply(Player player) {
        // no effect
    }
}
