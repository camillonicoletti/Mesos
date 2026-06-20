package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for GameController lobby validation.
 *
 * Verifies nickname uniqueness, totem color uniqueness and player count bounds.
 */
public class GameControllerLobbyValidationTest {

    @Test
    void joinRejectsTotemWithNullColor() {
        GameController controller = new GameController();

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.onMessageReceived("Alice", new JoinGameAction(new Totem(null, "OFFER_TRACK")))
        );
    }

    @Test
    void setPlayersCountRejectsValueLowerThanCurrentLobbySize() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Carol", new JoinGameAction(createTotem(TotemColor.PURPLE)));

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.onMessageReceived("Alice", new SetPlayerCountAction(2))
        );
    }

    private Totem createTotem(TotemColor color) {
        return new Totem(color, "OFFER_TRACK");
    }
}
