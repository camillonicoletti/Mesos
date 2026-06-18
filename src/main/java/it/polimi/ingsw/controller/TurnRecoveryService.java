package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.state.ActionResolutionState;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.controller.state.SuspendedState;
import it.polimi.ingsw.controller.state.TotemPlacementState;

import java.io.Serializable;

/**
 * Centralizes read-only turn lookup across normal and suspended states.
 */
public class TurnRecoveryService implements Serializable {

    private static final long serialVersionUID = 1L;

    public String currentActionPlayer(GameState state) {
        GameState activeState = unwrapSuspendedState(state);
        if (activeState instanceof ActionResolutionState actionResolutionState) {
            return actionResolutionState.getCurrentPlayerNickname();
        }
        return null;
    }

    public String currentTurnPlayer(GameController controller, GameState state) {
        GameState activeState = unwrapSuspendedState(state);
        if (activeState instanceof TotemPlacementState totemPlacementState) {
            return totemPlacementState.getCurrentPlayerNickname(controller);
        }
        if (activeState instanceof ActionResolutionState actionResolutionState) {
            return actionResolutionState.getCurrentPlayerNickname();
        }
        return null;
    }

    public GameState unwrapSuspendedState(GameState state) {
        return SuspendedState.unwrap(state);
    }
}
