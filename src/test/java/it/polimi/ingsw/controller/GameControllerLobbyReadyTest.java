package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.controller.state.LobbyState;
import it.polimi.ingsw.controller.state.ReadyCheckState;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameController lobby ready flow.
 *
 * <p>Verifies: ready / unready transitions and game-start trigger when all players are ready.
 */
public class GameControllerLobbyReadyTest {

    @Test
    void newControllerStartsInLobbyState() {
        GameController controller = new GameController();

        assertInstanceOf(LobbyState.class, controller.getCurrentState());
        assertNull(controller.getGame());
        assertTrue(controller.getLobbyPlayers().isEmpty());
    }

    @Test
    void playerCanJoinLobby() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertEquals(1, controller.getLobbyPlayers().size());
        assertEquals("Alice", controller.getLobbyPlayers().get(0).getNickname());
        assertEquals(TotemColor.RED, controller.getLobbyPlayers().get(0).getTotem().getColor());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    @Test
    void duplicateNicknameIsRejected() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.BLUE)))
        );

        assertEquals(1, controller.getLobbyPlayers().size());
    }

    @Test
    void duplicateTotemColorIsRejected() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.RED)))
        );

        assertEquals(1, controller.getLobbyPlayers().size());
    }

    @Test
    void firstPlayerCanSetPlayerCount() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));

        assertEquals(2, controller.getChosenPlayersCount());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    @Test
    void nonFirstPlayerCannotSetPlayerCount() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived("Bob", new SetPlayerCountAction(2))
        );
    }

    @Test
    void invalidPlayerCountIsRejected() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived("Alice", new SetPlayerCountAction(1))
        );

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived("Alice", new SetPlayerCountAction(6))
        );
    }

    @Test
    void lobbyMovesToReadyCheckWhenExpectedPlayersJoined() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));

        assertEquals(2, controller.getLobbyPlayers().size());
        assertInstanceOf(ReadyCheckState.class, controller.getCurrentState());
    }

    @Test
    void choosingPlayerCountStartsImmediatelyIfJoinedPlayersWereAlreadyReady() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Charlie", new JoinGameAction(createTotem(TotemColor.PURPLE)));

        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onMessageReceived("Bob", new ReadyAction(true));
        controller.onMessageReceived("Charlie", new ReadyAction(true));

        controller.onMessageReceived("Alice", new SetPlayerCountAction(3));

        assertNotNull(controller.getGame());
        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
    }

    @Test
    void lobbyRejectsGameActionsBeforeReadyCheck() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertDoesNotThrow(() ->
                controller.onMessageReceived("Alice", new ReadyAction(true))
        );
        assertTrue(controller.isPlayerReady("Alice"));

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived("Alice", new PlaceTotemAction('A'))
        );
    }

    @Test
    void nullNicknameIsRejected() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived(null, new JoinGameAction(createTotem(TotemColor.RED)))
        );
    }

    @Test
    void blankNicknameIsRejected() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived("   ", new JoinGameAction(createTotem(TotemColor.RED)))
        );
    }

    @Test
    void nullActionIsRejected() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived("Alice", null)
        );
    }

    @Test
    void readyCheckAcceptsReadyActions() {
        GameController controller = createReadyCheckController();

        controller.onMessageReceived("Alice", new ReadyAction(true));

        assertTrue(controller.isPlayerReady("Alice"));
        assertFalse(controller.isPlayerReady("Bob"));
        assertInstanceOf(ReadyCheckState.class, controller.getCurrentState());
    }

    @Test
    void readyFalseRemovesPlayerFromReadySet() {
        GameController controller = createReadyCheckController();

        controller.onMessageReceived("Alice", new ReadyAction(true));
        assertTrue(controller.isPlayerReady("Alice"));

        controller.onMessageReceived("Alice", new ReadyAction(false));
        assertFalse(controller.isPlayerReady("Alice"));
    }

    @Test
    void unknownPlayerCannotSendReadyAction() {
        GameController controller = createReadyCheckController();

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived("Charlie", new ReadyAction(true))
        );
    }

    @Test
    void readyCheckRejectsLobbyAndTotemActions() {
        GameController controller = createReadyCheckController();

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.PURPLE)))
        );

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived("Alice", new PlaceTotemAction('A'))
        );
    }

    @Test
    void disconnectionInLobbyRemovesPlayer() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));

        controller.onPlayerDisconnection("Alice");

        assertEquals(1, controller.getLobbyPlayers().size());
        assertEquals("Bob", controller.getLobbyPlayers().get(0).getNickname());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    @Test
    void disconnectionInReadyCheckReturnsToLobby() {
        GameController controller = createReadyCheckController();

        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onPlayerDisconnection("Bob");

        assertInstanceOf(LobbyState.class, controller.getCurrentState());
        assertEquals(1, controller.getLobbyPlayers().size());
        assertEquals("Alice", controller.getLobbyPlayers().get(0).getNickname());
        assertFalse(controller.isPlayerReady("Bob"));
        assertNull(controller.getGame());
    }

    @Test
    void controllerNotifiesLobbyAndReadyEvents() {
        GameController controller = new GameController();
        RecordingListener listener = new RecordingListener();
        controller.addListener(listener);

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));

        assertTrue(listener.events.contains("joined:Alice"));
        assertTrue(listener.events.contains("joined:Bob"));
        assertTrue(listener.events.contains("count:2"));
        assertTrue(listener.events.stream().anyMatch(event -> event.startsWith("state:ReadyCheckState")));
    }

    private GameController createReadyCheckController() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));

        assertInstanceOf(ReadyCheckState.class, controller.getCurrentState());
        return controller;
    }

    private static Totem createTotem(TotemColor color) {
        return new Totem(color, "OFFER_TRACK");
    }

    private static final class RecordingListener implements GameListener {
        private final List<String> events = new ArrayList<>();

        @Override
        public void onPlayerJoined(String nickname) {
            events.add("joined:" + nickname);
        }

        @Override
        public void onPlayersCountSet(int count) {
            events.add("count:" + count);
        }

        @Override
        public void onPlayerDisconnected(String nickname) {
            events.add("disconnected:" + nickname);
        }

        @Override
        public void onPlayerReconnected(String nickname) {
            events.add("reconnected:" + nickname);
        }

        @Override
        public void onGameSuspended() {
            events.add("suspended");
        }

        @Override
        public void onGameResumed() {
            events.add("resumed");
        }

        @Override
        public void onGameStarted() {
            events.add("started");
        }

        @Override
        public void onStateChanged(GameState newState) {
            events.add("state:" + newState.getClass().getSimpleName());
        }

        @Override
        public void onTotemPlaced(String nickname, char tileId) {
        }

        @Override
        public void onFoodChanged(String nickname, int newAmount, int delta) {
        }

        @Override
        public void onPointsChanged(String nickname, int newAmount, int delta) {
        }

        @Override
        public void onEraChanged(Era newEra) {
        }

        @Override
        public void onCharacterCardTaken(String nickname, String cardId, boolean fromUpperRow) {
        }

        @Override
        public void onBuildingCardTaken(String nickname, String cardId, boolean fromUpperRow) {
        }

        @Override
        public void onRoundEnded(int roundNumber) {
        }

        @Override
        public void onGameEnded(Player winner, Map<Player, Integer> finalScores) {
        }

        @Override
        public void onExtraCardPickAvailable(String nickname, int picks) {
        }
    }
}
