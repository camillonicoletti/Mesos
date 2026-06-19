package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.state.EndGameState;
import it.polimi.ingsw.controller.state.GameState;

import java.io.Serializable;

// Coordinates controller-level persistence decisions.
public class GamePersistenceService implements Serializable {
    private static final long serialVersionUID = 1L;

    // Saves once at the action boundary, not per intermediate event (state handlers are still
    // mutating their internal progress while those fire). Skipped at end-game because the
    // persistence listener clears the save file when the match ends instead.
    public void saveAfterAction(GameController controller, GameState currentState) {
        if (controller == null || currentState instanceof EndGameState) {
            return;
        }

        Persistence.save(controller);
    }
}
