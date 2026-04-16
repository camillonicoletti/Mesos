package it.polimi.ingsw.controller.action;

import java.io.Serializable;

/**
 * Represents a generic action sent by a player to the controller.
 */
public interface GameAction extends Serializable {

    /**
     * Returns the type of the action.
     *
     * @return the action type
     */
    String getType();
}