package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.controller.state.LobbyState;
import it.polimi.ingsw.controller.state.SuspendedState;
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
 * Extra edge cases for the controller lobby/ready/disconnection logic.
 * These tests intentionally avoid network, server, VirtualView and client code.
 */
public class GameControllerMoreLobbyEdgeTest {

    @Test
    void cannotSetPlayerCountBeforeAnyPlayerJoins() {
        GameController controller = new GameController();

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived("Alice", new SetPlayerCountAction(2))
        );

        assertEquals(-1, controller.getChosenPlayersCount());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    @Test
    void firstPlayerCanChangePlayerCountBeforeGameStarts() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(3));

        assertDoesNotThrow(() ->
                controller.onMessageReceived("Alice", new SetPlayerCountAction(4))
        );

        assertEquals(4, controller.getChosenPlayersCount());
    }

    @Test
    void firstPlayerDisconnectsBeforeChoosingCountAndNextPlayerCanBecomeFirst() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));

        controller.onPlayerDisconnection("Alice");

        assertEquals(1, controller.getLobbyPlayers().size());
        assertEquals("Bob", controller.getLobbyPlayers().get(0).getNickname());
        assertDoesNotThrow(() ->
                controller.onMessageReceived("Bob", new SetPlayerCountAction(2))
        );
        assertEquals(2, controller.getChosenPlayersCount());
    }

    @Test
    void whenLobbyBecomesEmptyChosenPlayerCountIsReset() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(3));

        controller.onPlayerDisconnection("Alice");

        assertTrue(controller.getLobbyPlayers().isEmpty());
        assertEquals(-1, controller.getChosenPlayersCount());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    @Test
    void unknownDisconnectionInLobbyDoesNotChangeLobby() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onPlayerDisconnection("Ghost");

        assertEquals(1, controller.getLobbyPlayers().size());
        assertEquals("Alice", controller.getLobbyPlayers().get(0).getNickname());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    @Test
    void reconnectionBeforeGameExistsIsRejected() {
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
                controller.getLobbyPlayers().clear()
        );

        assertEquals(1, controller.getLobbyPlayers().size());
    }

    @Test
    void nullListenerIsRejected() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () -> controller.addListener(null));
        assertThrows(IllegalArgumentException.class, () -> controller.removeListener(null));
    }

    @Test
    void removedListenerDoesNotReceiveFutureNotifications() {
        GameController controller = new GameController();
        RecordingListener listener = new RecordingListener();

        controller.addListener(listener);
        controller.removeListener(listener);

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertTrue(listener.events.isEmpty());
    }

    @Test
    void oneFailingListenerDoesNotBlockTheNextListener() {
        GameController controller = new GameController();
        RecordingListener goodListener = new RecordingListener();

        controller.addListener(new ExplodingListener());
        controller.addListener(goodListener);

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertEquals(List.of("joined:Alice"), goodListener.events);
    }

    @Test
    void suspensionTimeoutMustBePositive() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () -> controller.setSuspensionTimeoutMillis(0));
        assertThrows(IllegalArgumentException.class, () -> controller.setSuspensionTimeoutMillis(-1));
    }

    @Test
    void checkSuspensionTimeoutReturnsFalseWhenGameIsNotSuspended() {
        GameController controller = new GameController();

        assertFalse(controller.checkSuspensionTimeout(123456L));
    }

    @Test
    void suspendedStateRejectsEveryPlayerAction() {
        GameController controller = createStartedGameController();

        controller.onPlayerDisconnection("Bob");

        assertTrue(controller.isSuspended());
        assertInstanceOf(SuspendedState.class, controller.getCurrentState());
        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived("Alice", new ReadyAction(true))
        );
        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived("Alice", new PlaceTotemAction('A'))
        );
    }

    @Test
    void disconnectedPlayerCannotReconnectWithBlankNickname() {
        GameController controller = createStartedGameController();

        assertThrows(IllegalArgumentException.class, () -> controller.onPlayerReconnection(" "));
        assertThrows(IllegalArgumentException.class, () -> controller.onPlayerReconnection(null));
    }

    /**
     * BUG-HUNTER: currently this may fail.
     * If players can join before the first player sets the count, the first player should not be
     * allowed to choose a number lower than the number of players already waiting in lobby.
     */
    @Test
    void cannotChoosePlayerCountLowerThanAlreadyJoinedPlayers() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Charlie", new JoinGameAction(createTotem(TotemColor.PURPLE)));

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived("Alice", new SetPlayerCountAction(2))
        );
    }

    /**
     * BUG-HUNTER: currently this may fail depending on your chosen design.
     * If the player who chose the count disconnects before game start, the lobby may need to reset
     * the chosen count, otherwise the remaining players inherit a decision made by someone who left.
     */
    @Test
    void disconnectingFirstPlayerAfterChoosingCountShouldResetCountForRemainingLobby() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(3));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));

        controller.onPlayerDisconnection("Alice");

        assertEquals(1, controller.getLobbyPlayers().size());
        assertEquals("Bob", controller.getLobbyPlayers().get(0).getNickname());
        assertEquals(3, controller.getChosenPlayersCount());
        assertTrue(controller.isFirstPlayer("Bob"));
    }

    private GameController createStartedGameController() {
        GameController controller = new GameController();
        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.setGame(new Game(
                controller.getLobbyPlayers(),
                1,
                null,
                Era.I,
                null,
                null,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        ));
        return controller;
    }

    private static Totem createTotem(TotemColor color) {
        return new Totem(color, "OFFER_TRACK");
    }

    private static class RecordingListener implements GameListener {
        private final List<String> events = new ArrayList<>();

        @Override public void onPlayerJoined(String nickname) { events.add("joined:" + nickname); }
        @Override public void onPlayersCountSet(int count) { events.add("count:" + count); }
        @Override public void onPlayerDisconnected(String nickname) { events.add("disconnected:" + nickname); }
        @Override public void onPlayerReconnected(String nickname) { events.add("reconnected:" + nickname); }
        @Override public void onGameSuspended() { events.add("suspended"); }
        @Override public void onGameResumed() { events.add("resumed"); }
        @Override public void onGameStarted() { events.add("started"); }
        @Override public void onStateChanged(GameState newState) { events.add("state:" + newState.getClass().getSimpleName()); }
        @Override public void onTotemPlaced(String nickname, char tileId) { events.add("placed:" + nickname + ":" + tileId); }
        @Override public void onFoodChanged(String nickname, int newAmount, int delta) { events.add("food:" + nickname + ":" + newAmount + ":" + delta); }
        @Override public void onPointsChanged(String nickname, int newAmount, int delta) { events.add("points:" + nickname + ":" + newAmount + ":" + delta); }
        @Override public void onEraChanged(Era newEra) { events.add("era:" + newEra); }
        @Override public void onCharacterCardTaken(String nickname, String cardId, boolean fromUpperRow) { events.add("character:" + nickname + ":" + cardId + ":" + fromUpperRow); }
        @Override public void onBuildingCardTaken(String nickname, String cardId, boolean fromUpperRow) { events.add("building:" + nickname + ":" + cardId + ":" + fromUpperRow); }
        @Override public void onRoundEnded(int roundNumber) { events.add("round:" + roundNumber); }
        @Override public void onGameEnded(Player winner, Map<Player, Integer> finalScores) { events.add("ended:" + (winner == null ? "null" : winner.getNickname())); }
        @Override public void onExtraCardPickAvailable(String nickname, int picks) { events.add("extra:" + nickname + ":" + picks); }
    }

    private static final class ExplodingListener extends RecordingListener {
        @Override public void onPlayerJoined(String nickname) {
            throw new RuntimeException("Simulated listener failure");
        }
    }
}
