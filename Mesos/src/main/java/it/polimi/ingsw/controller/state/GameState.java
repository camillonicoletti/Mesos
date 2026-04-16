package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;

import java.io.Serializable;

/**
 * Represents one state of the game controller.
 * Each state decides which actions are valid and how to handle them.
 */
public interface GameState extends Serializable {

    /**
     * Called when the controller enters this state.
     *
     * @param ctx the controller context
     */
    void onEnter(GameController ctx);

    /**
     * Handles an action coming from a specific player.
     *
     * @param ctx the controller context
     * @param playerNickname the nickname of the player who sent the action
     * @param action the action to handle
     */
    void handleAction(GameController ctx, String playerNickname, GameAction action);

    /**
     * Called when the controller exits this state.
     *
     * @param ctx the controller context
     */
    void onExit(GameController ctx);
}