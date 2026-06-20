package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.controller.state.LobbyState;
import it.polimi.ingsw.controller.state.ReadyCheckState;
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
 * Unit tests for GameController state machine.
 *
 * Verifies lobby/ready validation, listener notification robustness, and
 * state-rollback behavior.
 */
public class GameControllerScenarioCoverageTest {

    @Test
    void settingPlayerCountBeforeAnyPlayerJoinedIsRejected() {
        GameController controller = new GameController();

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived("Alice", new SetPlayerCountAction(2))
        );

        assertEquals(-1, controller.getChosenPlayersCount());
        assertTrue(controller.getLobbyPlayers().isEmpty());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    @Test
    void firstPlayerCanChangePlayerCountAfterItHasAlreadyBeenChosen() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(3));

        controller.onMessageReceived("Alice", new SetPlayerCountAction(4));

        assertEquals(4, controller.getChosenPlayersCount());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    @Test
    void lobbyFullAfterReadyCheckRejectsAnyAdditionalJoin() {
        GameController controller = createReadyCheckController();

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived("Charlie", new JoinGameAction(createTotem(TotemColor.PURPLE)))
        );

        assertEquals(2, controller.getLobbyPlayers().size());
        assertFalse(controller.containsLobbyPlayer("Charlie"));
        assertInstanceOf(ReadyCheckState.class, controller.getCurrentState());
    }

    @Test
    void unknownDisconnectionInLobbyIsIgnoredAndDoesNotNotifyListeners() {
        GameController controller = new GameController();
        RecordingListener listener = new RecordingListener();
        controller.addListener(listener);

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onPlayerDisconnection("Ghost");

        assertEquals(1, controller.getLobbyPlayers().size());
        assertEquals("Alice", controller.getLobbyPlayers().get(0).getNickname());
        assertFalse(listener.events.contains("disconnected:Ghost"));
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    @Test
    void reconnectingBeforeGameStartsIsRejectedBecauseThereIsNoGamePlayer() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertThrows(IllegalArgumentException.class, () ->
                controller.onPlayerReconnection("Alice")
        );
    }

    @Test
    void lobbyPlayersListCannotBeModifiedFromOutside() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertThrows(UnsupportedOperationException.class, () ->
                controller.getLobbyPlayers().add(new Player("Injected", createTotem(TotemColor.BLUE)))
        );

        assertEquals(1, controller.getLobbyPlayers().size());
        assertEquals("Alice", controller.getLobbyPlayers().get(0).getNickname());
    }

    @Test
    void nullListenerCannotBeAddedOrRemoved() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () -> controller.addListener(null));
        assertThrows(IllegalArgumentException.class, () -> controller.removeListener(null));
    }

    @Test
    void listenerExceptionDoesNotPreventOtherListenersFromReceivingTheSameEvent() {
        GameController controller = new GameController();
        RecordingListener listener = new RecordingListener();

        controller.addListener(new ThrowingListener());
        controller.addListener(listener);

        assertDoesNotThrow(() ->
                controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)))
        );

        assertTrue(listener.events.contains("joined:Alice"));
    }

    @Test
    void invalidSuspensionTimeoutIsRejected() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () -> controller.setSuspensionTimeoutMillis(0));
        assertThrows(IllegalArgumentException.class, () -> controller.setSuspensionTimeoutMillis(-1));
    }

    @Test
    void changingToNullStateIsRejected() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () -> controller.changeState(null));
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    @Test
    void allPlayersLeavingLobbyResetChosenPlayerCount() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));
        controller.onPlayerDisconnection("Alice");

        assertTrue(controller.getLobbyPlayers().isEmpty());
        assertEquals(-1, controller.getChosenPlayersCount());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    @Test
    void removingUnknownLobbyPlayerDirectlyIsRejected() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () ->
                controller.removePlayerFromLobby("Ghost")
        );
    }

    @Test
    void markingUnknownPlayerDisconnectedDirectlyIsRejected() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () ->
                controller.markPlayerDisconnected("Ghost")
        );
    }

    @Test
    void readyCheckAllowsFirstPlayerToIncreasePlayerCountAndRejectsTotemPlacementActions() {
        GameController controller = createReadyCheckController();

        controller.onMessageReceived("Alice", new SetPlayerCountAction(3));

        assertEquals(3, controller.getChosenPlayersCount());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived("Alice", new PlaceTotemAction('A'))
        );
    }

    @Test
    void nullTotemJoinIsRejectedAndDoesNotCreatePlayer() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived("Alice", new JoinGameAction(null))
        );

        assertTrue(controller.getLobbyPlayers().isEmpty());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    // Verifies that lobby uniqueness is enforced on TotemColor, not on Totem
    // object identity: two different Totem instances with the same color must
    // not be allowed to coexist in the lobby.
    @Test
    void duplicateTotemColorShouldBeRejectedEvenWhenTotemObjectsAreDifferent() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.RED)))
        );

        assertEquals(1, controller.getLobbyPlayers().size());
    }

    // Verifies that ReadyCheckState validates the sender for both
    // ReadyAction(true) and ReadyAction(false): a nickname not in the lobby
    // must be rejected even when un-readying.
    @Test
    void unknownPlayerCannotSendReadyFalseAction() {
        GameController controller = createReadyCheckController();

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived("Charlie", new ReadyAction(false))
        );
    }

    // Verifies that changeState preserves the previous state when the target
    // state's onEnter throws: the controller must not be left pointing at a
    // state whose entry failed.
    @Test
    void failedStateTransitionShouldKeepPreviousState() {
        GameController controller = new GameController();

        assertThrows(IllegalStateException.class, () ->
                controller.changeState(new ReadyCheckState())
        );

        assertInstanceOf(LobbyState.class, controller.getCurrentState());
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
            events.add("totem:" + nickname + ":" + tileId);
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

    private static final class ThrowingListener implements GameListener {
        @Override
        public void onPlayerJoined(String nickname) {
            throw new RuntimeException("Simulated listener failure");
        }

        @Override
        public void onPlayersCountSet(int count) {
            throw new RuntimeException("Simulated listener failure");
        }

        @Override
        public void onPlayerDisconnected(String nickname) {
            throw new RuntimeException("Simulated listener failure");
        }

        @Override
        public void onPlayerReconnected(String nickname) {
            throw new RuntimeException("Simulated listener failure");
        }

        @Override
        public void onGameSuspended() {
            throw new RuntimeException("Simulated listener failure");
        }

        @Override
        public void onGameResumed() {
            throw new RuntimeException("Simulated listener failure");
        }

        @Override
        public void onGameStarted() {
            throw new RuntimeException("Simulated listener failure");
        }

        @Override
        public void onStateChanged(GameState newState) {
            throw new RuntimeException("Simulated listener failure");
        }

        @Override
        public void onTotemPlaced(String nickname, char tileId) {
            throw new RuntimeException("Simulated listener failure");
        }

        @Override
        public void onFoodChanged(String nickname, int newAmount, int delta) {
            throw new RuntimeException("Simulated listener failure");
        }

        @Override
        public void onPointsChanged(String nickname, int newAmount, int delta) {
            throw new RuntimeException("Simulated listener failure");
        }

        @Override
        public void onEraChanged(Era newEra) {
            throw new RuntimeException("Simulated listener failure");
        }

        @Override
        public void onCharacterCardTaken(String nickname, String cardId, boolean fromUpperRow) {
            throw new RuntimeException("Simulated listener failure");
        }

        @Override
        public void onBuildingCardTaken(String nickname, String cardId, boolean fromUpperRow) {
            throw new RuntimeException("Simulated listener failure");
        }

        @Override
        public void onRoundEnded(int roundNumber) {
            throw new RuntimeException("Simulated listener failure");
        }

        @Override
        public void onGameEnded(Player winner, Map<Player, Integer> finalScores) {
            throw new RuntimeException("Simulated listener failure");
        }

        @Override
        public void onExtraCardPickAvailable(String nickname, int picks) {
            throw new RuntimeException("Simulated listener failure");
        }
    }
}
