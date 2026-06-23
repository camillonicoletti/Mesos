package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.controller.state.EndRoundState;
import it.polimi.ingsw.controller.state.EndGameState;
import it.polimi.ingsw.controller.state.EventResolutionState;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.ActionSlot;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.FoodSlot;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.board.TurnOrderTile;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.EventResolvedMessage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the interaction between persistence and the suspension/resilience
 * feature when a server restarts from a saved game.
 *
 * When a server crashes while the match is suspended (only one player left),
 * the lone-player countdown must not be judged by the stale pre-crash timestamp
 * after the restart. With nobody connected the countdown is paused; it restarts
 * fresh from the moment the first player reconnects, and the match resumes once
 * enough players are back instead of being ended by a phantom timeout.
 */
class SuspensionRestartRegressionTest {

    @Test
    void restoreWithNoConnectedPlayersPausesLonePlayerCountdown() throws Exception {
        GameController controller = createStartedGameController(3);
        controller.setSuspensionTimeoutMillis(60_000L);

        // Two players drop; one remains -> suspended, lone-player countdown running.
        controller.onPlayerDisconnection("Bob");
        controller.onPlayerDisconnection("Charlie");
        assertTrue(controller.isSuspended());
        assertEquals(1, controller.getConnectedGamePlayersCount());
        assertTrue(controller.getSuspensionStartedAtMillis() > 0L);

        GameController restored = restartRoundTrip(controller);

        assertTrue(restored.isSuspended(), "still suspended, waiting for reconnects");
        assertEquals(0, restored.getConnectedGamePlayersCount());
        assertFalse(restored.getGame().isGameOver());
        assertTrue(restored.getSuspensionStartedAtMillis() < 0L,
                "With nobody connected after a restart the lone-player countdown must be "
                + "paused; otherwise the stale pre-crash timestamp would end the game the "
                + "instant the first player reconnects.");
    }

    @Test
    void restoreThenAllThreeReconnectResumesGameInsteadOfEndingIt() throws Exception {
        GameController controller = createStartedGameController(3);
        controller.setSuspensionTimeoutMillis(60_000L);
        controller.onPlayerDisconnection("Bob");
        controller.onPlayerDisconnection("Charlie");

        GameController restored = restartRoundTrip(controller);

        // First reconnect: still only one connected -> fresh countdown, no phantom timeout.
        restored.onPlayerReconnection("Alice");
        assertTrue(restored.isSuspended());
        assertEquals(1, restored.getConnectedGamePlayersCount());
        assertTrue(restored.getSuspensionStartedAtMillis() > 0L,
                "countdown must restart from the reconnect moment, not from the stale value");
        assertFalse(restored.getGame().isGameOver(),
                "a fresh countdown must not have elapsed yet");

        // Second reconnect: two connected -> resume to the exact pre-suspension state.
        restored.onPlayerReconnection("Bob");
        assertFalse(restored.isSuspended());
        assertFalse(restored.getGame().isGameOver());
        assertInstanceOf(TotemPlacementState.class, restored.getCurrentState());

        // Third reconnect: a normal mid-game rejoin.
        restored.onPlayerReconnection("Charlie");
        assertFalse(restored.isPlayerDisconnected("Charlie"));
        assertFalse(restored.getGame().isGameOver());
    }

    @Test
    void restoreCapturedDuringEndRoundSleepFinishesNextRoundSetupOnReconnect() throws Exception {
        GameController controller = createStartedGameController(2);
        AtomicReference<GameController> capturedSave = new AtomicReference<>();
        CountDownLatch roundEnded = new CountDownLatch(1);

        controller.addListener(new NoOpGameListener() {
            @Override
            public void onRoundEnded(int roundNumber) {
                try {
                    capturedSave.set(serializationRoundTrip(controller));
                } catch (Exception e) {
                    throw new AssertionError(e);
                } finally {
                    roundEnded.countDown();
                }
            }
        });

        Thread endRoundThread = new Thread(() -> controller.changeState(new EndRoundState()));
        endRoundThread.start();
        assertTrue(roundEnded.await(1, TimeUnit.SECONDS),
                "EndRoundState must emit RoundEnded before its display pause");

        GameController restored = capturedSave.get();
        assertInstanceOf(EndRoundState.class, restored.getCurrentState(),
                "precondition: the saved controller represents the EndRoundState display pause");
        assertEquals(2, restored.getGame().getCurrentRound().getNumber(),
                "precondition: EndRoundState has already prepared the next round before saving");

        restored.getGame().setNotifier(restored::notifyListeners);
        restored.markAllPlayersDisconnected();
        restored.onPlayerReconnection("Alice");
        restored.onPlayerReconnection("Bob");

        assertInstanceOf(TotemPlacementState.class, restored.getCurrentState(),
                "after all players reconnect, the restored end-round pause must complete instead of staying stale");
        assertEquals(2, restored.getGame().getCurrentRound().getNumber(),
                "resuming an already-prepared EndRoundState must not advance another round");
        assertFalse(restored.isPlayerWaitingNextRound("Alice"));
        assertFalse(restored.isPlayerWaitingNextRound("Bob"));

        endRoundThread.interrupt();
        endRoundThread.join(1_000);
    }

    @Test
    void restoreCapturedBeforeEventResolutionEnterContinuesResolutionOnReconnect() throws Exception {
        GameController controller = createStartedGameController(2, 10);
        setCurrentState(controller, new EventResolutionState());

        GameController restored = restartRoundTrip(controller);
        restored.onPlayerReconnection("Alice");
        restored.onPlayerReconnection("Bob");

        assertInstanceOf(EndGameState.class, restored.getCurrentState(),
                "event resolution saved before onEnter must resume instead of staying stale");
        assertTrue(restored.getGame().isGameOver());
    }

    /** Mirrors what Persistence.load + LobbyManager do on a server restart. */
    private GameController restartRoundTrip(GameController controller) throws Exception {
        GameController restored = serializationRoundTrip(controller);
        if (restored.getGame() != null) {
            restored.getGame().setNotifier(restored::notifyListeners); // Persistence.load does this
        }
        restored.markAllPlayersDisconnected();                          // LobbyManager does this on restore
        return restored;
    }

    private GameController serializationRoundTrip(GameController controller) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(baos)) {
            out.writeObject(controller);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            return (GameController) in.readObject();
        }
    }

    private GameController createStartedGameController(int playerCount) {
        return createStartedGameController(playerCount, 1);
    }

    private GameController createStartedGameController(int playerCount, int currentRoundNumber) {
        GameController controller = new GameController();
        controller.onMessageReceived("Alice", new JoinGameAction(new Totem(TotemColor.RED, "OFFER_TRACK")));
        controller.onMessageReceived("Bob", new JoinGameAction(new Totem(TotemColor.BLUE, "OFFER_TRACK")));
        if (playerCount >= 3) {
            controller.onMessageReceived("Charlie", new JoinGameAction(new Totem(TotemColor.PURPLE, "OFFER_TRACK")));
        }

        List<Player> players = new ArrayList<>(controller.getLobbyPlayers());
        List<OfferTile> tiles = new ArrayList<>();
        for (char letter = 'A'; letter < 'A' + Math.max(2, playerCount); letter++) {
            tiles.add(new OfferTile(letter, 1, List.of(new ActionSlot(new FoodSlot(), 0))));
        }
        Board board = new Board(tiles, TurnOrderTile.createForPlayers(players.size()));
        Game game = new Game(players, currentRoundNumber, null, Era.I, null, board,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        Round round = new Round(currentRoundNumber, game, Era.I,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        game.setCurrentRound(round);
        for (Player player : players) {
            player.setGame(game);
        }
        controller.setGame(game);
        controller.changeState(new TotemPlacementState());
        return controller;
    }

    private void setCurrentState(GameController controller, GameState state) throws Exception {
        Field field = GameController.class.getDeclaredField("currentState");
        field.setAccessible(true);
        field.set(controller, state);
    }

    private static class NoOpGameListener implements GameListener {
        @Override public void onPlayerJoined(String nickname) {}
        @Override public void onPlayersCountSet(int count) {}
        @Override public void onPlayerDisconnected(String nickname) {}
        @Override public void onPlayerReconnected(String nickname) {}
        @Override public void onGameSuspended() {}
        @Override public void onGameResumed() {}
        @Override public void onGameStarted() {}
        @Override public void onStateChanged(GameState newState) {}
        @Override public void onTotemPlaced(String nickname, char tileId) {}
        @Override public void onFoodChanged(String nickname, int newAmount, int delta) {}
        @Override public void onPointsChanged(String nickname, int newAmount, int delta) {}
        @Override public void onEraChanged(Era newEra) {}
        @Override public void onCharacterCardTaken(String nickname, String cardId, boolean fromUpperRow) {}
        @Override public void onBuildingCardTaken(String nickname, String cardId, boolean fromUpperRow) {}
        @Override public void onEventResolved(int cardId, String title, String description,
                                              List<EventResolvedMessage.PlayerEffect> effects) {}
        @Override public void onRoundEnded(int roundNumber) {}
        @Override public void onGameEnded(Player winner, Map<Player, Integer> finalScores) {}
        @Override public void onExtraCardPickAvailable(String nickname, int picks) {}
    }
}
