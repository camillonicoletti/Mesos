package it.polimi.ingsw.controller;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.board.ActionSlot;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.FoodSlot;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.board.TurnOrderTile;
import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.controller.state.LobbyState;
import it.polimi.ingsw.controller.state.ReadyCheckState;
import it.polimi.ingsw.controller.state.StartGameState;
import it.polimi.ingsw.controller.state.SuspendedState;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import it.polimi.ingsw.model.Game;
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
 * Unit tests for GameController disconnection / reconnection flow.
 *
 * <p>Verifies: behaviour when players disconnect during lobby, action resolution and event resolution.
 */
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
    void firstPlayerDisconnectionKeepsChosenPlayersCountAndPromotesNextPlayer() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));

        controller.onPlayerDisconnection("Alice");

        assertTrue(controller.hasChosenPlayersCount());
        assertEquals(2, controller.getChosenPlayersCount());
        assertEquals(1, controller.getLobbyPlayers().size());
        assertEquals("Bob", controller.getLobbyPlayers().get(0).getNickname());
        assertTrue(controller.isFirstPlayer("Bob"));
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
    void reducingPlayerCountToReadyPlayersAfterDisconnectStartsGame() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Charlie", new JoinGameAction(createTotem(TotemColor.PURPLE)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(3));

        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onMessageReceived("Bob", new ReadyAction(true));
        controller.onPlayerDisconnection("Charlie");

        assertInstanceOf(LobbyState.class, controller.getCurrentState());
        assertEquals(3, controller.getChosenPlayersCount());
        assertTrue(controller.isPlayerReady("Alice"));
        assertTrue(controller.isPlayerReady("Bob"));

        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));

        assertNotNull(controller.getGame());
        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
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
        assertInstanceOf(SuspendedState.class, controller.getCurrentState());
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

    @Test
    void disconnectionAndReconnectionNotifyOtherPlayersAndSuspendResumeGame() {
        GameController controller = createStartedGameController();
        RecordingListener listener = new RecordingListener();
        controller.addListener(listener);

        controller.onPlayerDisconnection("Bob");
        controller.onPlayerReconnection("Bob");

        assertEquals(
                List.of("disconnected:Bob", "suspended", "reconnected:Bob", "resumed"),
                listener.events
        );
    }

    @Test
    void repeatedDisconnectionIsIgnored() {
        GameController controller = createStartedGameController();
        RecordingListener listener = new RecordingListener();
        controller.addListener(listener);

        controller.onPlayerDisconnection("Bob");
        controller.onPlayerDisconnection("Bob");

        assertEquals(
                List.of("disconnected:Bob", "suspended"),
                listener.events
        );
    }

    @Test
    void suspensionTimeoutEndsGameAndDeclaresOnlyConnectedPlayerAsWinner() {
        GameController controller = createStartedGameController();
        RecordingListener listener = new RecordingListener();
        controller.addListener(listener);
        controller.setSuspensionTimeoutMillis(10L);

        controller.onPlayerDisconnection("Bob");

        long suspendedAt = controller.getSuspensionStartedAtMillis();
        assertFalse(controller.checkSuspensionTimeout(suspendedAt + 9L));
        assertTrue(controller.isSuspended());
        assertInstanceOf(SuspendedState.class, controller.getCurrentState());

        assertTrue(controller.checkSuspensionTimeout(suspendedAt + 10L));
        assertFalse(controller.isSuspended());
        assertTrue(controller.getGame().isGameOver());
        assertEquals("Alice", listener.gameEndedWinnerNickname);
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

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));

        List<Player> players = new ArrayList<>(controller.getLobbyPlayers());

        Board board = new Board(
                new ArrayList<>(List.of(
                        new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 0))),
                        new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 0)))
                )),
                TurnOrderTile.createForPlayers(players.size())
        );

        Game game = new Game(
                players,
                1,
                null,
                Era.I,
                null,
                board,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );

        Round round = new Round(
                1,
                game,
                Era.I,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );

        game.setCurrentRound(round);

        for (Player player : players) {
            player.setGame(game);
        }

        controller.setGame(game);
        controller.changeState(new TotemPlacementState());

        return controller;
    }

    private Totem createTotem(TotemColor color) {
        return new Totem(color, "OFFER_TRACK");
    }

    private static final class RecordingListener implements GameListener {
        private final List<String> events = new ArrayList<>();
        private String gameEndedWinnerNickname;

        @Override
        public void onPlayerJoined(String nickname) {
        }

        @Override
        public void onPlayersCountSet(int count) {
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
        }

        @Override
        public void onStateChanged(it.polimi.ingsw.controller.state.GameState newState) {
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
            gameEndedWinnerNickname = winner == null ? null : winner.getNickname();
        }

        @Override
        public void onExtraCardPickAvailable(String nickname, int picks) {
        }
    }
}
