package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;

import java.io.Serializable;

/**
 * Represents a generic state of the game flow.
 */
public interface GameState extends Serializable {

    /**
     * Called when the controller enters this state.
     *
     * @param ctx the game controller context
     */
    void onEnter(GameController ctx);

    /**
     * Handles an action received from a player while the controller is in this state.
     *
     * @param ctx the game controller context
     * @param playerNickname the nickname of the player who sent the action
     * @param action the received action
     */
    void handleAction(GameController ctx, String playerNickname, GameAction action);

    /**
     * Called when the controller exits this state.
     *
     * @param ctx the game controller context
     */
    void onExit(GameController ctx);
}