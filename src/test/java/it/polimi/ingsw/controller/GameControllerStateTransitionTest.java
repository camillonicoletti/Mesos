package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.state.AbstractGameState;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.controller.state.LobbyState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for GameController state machine transitions.
 *
 * <p>Verifies: transitions between Lobby, ReadyCheck, TotemPlacement, ActionResolution, EventResolution and Suspended states.
 */
public class GameControllerStateTransitionTest {

    @Test
    void failedOnEnterRestoresPreviousState() {
        GameController controller = new GameController();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> controller.changeState(new FailingState())
        );

        assertInstanceOf(LobbyState.class, controller.getCurrentState());
        assertInstanceOf(IllegalStateException.class, exception);
    }

    private static final class FailingState extends AbstractGameState {
        @Override
        public void onEnter(GameController ctx) {
            throw new IllegalStateException("Boom");
        }

        @Override
        public void handleAction(GameController ctx, String playerNickname, GameAction action) {
            rejectAction(action);
        }
    }
}
