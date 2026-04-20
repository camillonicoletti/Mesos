package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.controller.state.LobbyState;
import it.polimi.ingsw.controller.state.ReadyCheckState;
import it.polimi.ingsw.controller.state.StartGameState;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.enums.TotemPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameControllerDisconnectionTest {

    @Test
    void disconnectionInLobbyRemovesPlayerFromLobby() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));

        controller.onPlayerDisconnection("Alice");

        assertEquals(1, controller.getLobbyPlayers().size());
        assertEquals("Bob", controller.getLobbyPlayers().get(0).getNickname());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    @Test
    void disconnectionInReadyCheckRemovesPlayerAndReturnsToLobby() {
        GameController controller = createReadyCheckController();

        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onPlayerDisconnection("Bob");

        assertInstanceOf(LobbyState.class, controller.getCurrentState());
        assertEquals(1, controller.getLobbyPlayers().size());
        assertEquals("Alice", controller.getLobbyPlayers().get(0).getNickname());
        assertFalse(controller.isPlayerReady("Bob"));
    }

    @Test
    void disconnectionBeforeAllReadyPreventsGameStart() {
        GameController controller = createReadyCheckController();

        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onPlayerDisconnection("Bob");

        assertNull(controller.getGame());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    @Test
    void startGameStateDisconnectionIsIgnored() {
        GameController controller = new GameController();
        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        StartGameState state = new StartGameState();

        assertDoesNotThrow(() -> state.onPlayerDisconnected(controller, "Alice"));
        assertEquals(1, controller.getLobbyPlayers().size());
    }

    @Test
    void inGameDisconnectionSuspendsWhenOnlyOnePlayerRemainsConnected() {
        GameController controller = createStartedGameController();

        controller.onPlayerDisconnection("Bob");

        assertTrue(controller.isPlayerDisconnected("Bob"));
        assertTrue(controller.isSuspended());
        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new PlaceTotemAction('A')));
    }

    @Test
    void reconnectingAnotherPlayerResumesGame() {
        GameController controller = createStartedGameController();

        controller.onPlayerDisconnection("Bob");
        controller.onPlayerReconnection("Bob");

        assertFalse(controller.isPlayerDisconnected("Bob"));
        assertFalse(controller.isSuspended());
        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
    }

    private GameController createReadyCheckController() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));

        assertInstanceOf(ReadyCheckState.class, controller.getCurrentState());
        return controller;
    }

    private GameController createStartedGameController() {
        GameController controller = new GameController();
        Game game = new Game(
                controller.getLobbyPlayers(),
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.setGame(new Game(
                controller.getLobbyPlayers(),
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));
        controller.changeState(new TotemPlacementState());

        return controller;
    }

    private Totem createTotem(TotemColor color) {
        return new Totem(color, "OFFER_TRACK");
    }
}
