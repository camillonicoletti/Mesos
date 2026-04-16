package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;

/**
 * Base implementation of GameState with default empty enter/exit hooks
 * and a helper method for rejecting invalid actions.
 */
public abstract class AbstractGameState implements GameState {

    private static final long serialVersionUID = 1L;

    @Override
    public void onEnter(GameController ctx) {
        // Default: do nothing
    }

    @Override
    public void onExit(GameController ctx) {
        // Default: do nothing
    }

    /**
     * Rejects an action that is not valid in the current state.
     *
     * @param action the invalid action
     */
    protected void rejectAction(GameAction action) {
        throw new IllegalStateException(
                "Action not allowed in current state: " + action.getClass().getSimpleName()
        );
    }
}