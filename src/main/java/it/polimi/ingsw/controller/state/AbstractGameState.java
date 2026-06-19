package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;

// Base GameState with default no-op enter/exit/disconnect hooks and a shared helper for rejecting invalid actions.
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

    @Override
    public void onPlayerDisconnected(GameController ctx, String playerNickname) {
        // Default: do nothing
    }

    @Override
    public void onPlayerReconnected(GameController ctx, String playerNickname) {
        // Default: do nothing
    }

    @Override
    public void onResume(GameController ctx) {
        // Default: do nothing
    }

    protected void rejectAction(GameAction action) {
        throw new IllegalStateException(
                "Action not allowed in current state: " + action.getClass().getSimpleName()
        );
    }
}
