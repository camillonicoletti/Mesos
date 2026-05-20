package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.state.EndGameState;
import it.polimi.ingsw.controller.state.GameState;

import java.io.Serializable;

/**
 * Coordinates controller-level persistence decisions.
 */
public class GamePersistenceService implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Saves after an action has fully settled.
     *
     * <p>Per-event saves fire while state handlers are still mutating their
     * internal progress, so the controller saves once at the action boundary.
     * End-game state is skipped because the persistence listener clears the
     * save file when the match ends.</p>
     */
    public void saveAfterAction(GameController controller, GameState currentState) {
        if (controller == null || currentState instanceof EndGameState) {
            return;
        }

        Persistence.save(controller);
    }
}
