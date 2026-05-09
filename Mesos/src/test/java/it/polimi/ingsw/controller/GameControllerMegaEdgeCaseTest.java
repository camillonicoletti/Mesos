package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.controller.state.ActionResolutionState;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.controller.state.LobbyState;
import it.polimi.ingsw.controller.state.ReadyCheckState;
import it.polimi.ingsw.controller.state.SuspendedState;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.ActionSlot;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.FoodSlot;
import it.polimi.ingsw.model.board.LowerRowSlot;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.board.TurnOrderTile;
import it.polimi.ingsw.model.board.UpperRowSlot;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.effect.EndPPEffect;
import it.polimi.ingsw.model.card.effect.ExtraCardUpperRowEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.RoundPhase;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mega edge-case test suite for the controller package.
 *
 * Scope: controller/state/action/listener behavior only.
 * No network, server, client, VirtualView or socket/RMI classes are used.
 *
 * Some methods are intentionally BUG-HUNTER tests: if they fail, they reveal
 * a controller validation or state-flow issue worth discussing with the group.
 */
public class GameControllerMegaEdgeCaseTest {

    // ---------------------------------------------------------------------
    // BASIC INPUT VALIDATION
    // ---------------------------------------------------------------------

    @Test
    void newControllerStartsCleanInLobby() {
        GameController controller = new GameController();

        assertInstanceOf(LobbyState.class, controller.getCurrentState());
        assertNull(controller.getGame());
        assertEquals(-1, controller.getChosenPlayersCount());
        assertTrue(controller.getLobbyPlayers().isEmpty());
        assertFalse(controller.isSuspended());
        assertEquals(0, controller.getConnectedGamePlayersCount());
    }

    @Test
    void nullBlankNicknameAndNullActionAreRejectedAtEntryPoint() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived(null, new JoinGameAction(createTotem(TotemColor.RED))));
        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("   ", new JoinGameAction(createTotem(TotemColor.RED))));
        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Alice", null));
    }

    @Test
    void nullAndBlankNamesAreSafelyRejectedByHelperMethods() {
        GameController controller = new GameController();

        assertFalse(controller.isNicknameAvailable(null));
        assertFalse(controller.isNicknameAvailable("  "));
        assertFalse(controller.isFirstPlayer(null));
        assertFalse(controller.isFirstPlayer("  "));
        assertFalse(controller.isPlayerReady(null));
        assertFalse(controller.isPlayerReady("  "));
        assertFalse(controller.isPlayerDisconnected(null));
        assertFalse(controller.isPlayerDisconnected("  "));
    }

    // ---------------------------------------------------------------------
    // LOBBY: JOINS, COUNTS, COLORS
    // ---------------------------------------------------------------------

    @Test
    void settingPlayerCountBeforeAnyJoinIsRejected() {
        GameController controller = new GameController();

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new SetPlayerCountAction(2)));

        assertEquals(-1, controller.getChosenPlayersCount());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    @Test
    void invalidPlayerCountValuesAreRejected() {
        GameController controller = new GameController();
        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Alice", new SetPlayerCountAction(-10)));
        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Alice", new SetPlayerCountAction(0)));
        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Alice", new SetPlayerCountAction(1)));
        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Alice", new SetPlayerCountAction(6)));
        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Alice", new SetPlayerCountAction(100)));

        assertEquals(-1, controller.getChosenPlayersCount());
    }

    @Test
    void firstPlayerCanChangePlayerCountBeforeGameStarts() {
        GameController controller = new GameController();
        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        controller.onMessageReceived("Alice", new SetPlayerCountAction(4));

        assertEquals(4, controller.getChosenPlayersCount());

        assertDoesNotThrow(() -> controller.onMessageReceived("Alice", new SetPlayerCountAction(5)));
        assertEquals(5, controller.getChosenPlayersCount());
    }

    @Test
    void nonFirstPlayerCannotChoosePlayerCount() {
        GameController controller = new GameController();
        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Bob", new SetPlayerCountAction(2)));
    }

    @Test
    void duplicateNicknameIsRejectedEvenWithDifferentTotem() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.BLUE))));
        assertEquals(1, controller.getLobbyPlayers().size());
    }

    @Test
    void allFiveTotemColorsCanJoinExactlyOnceAndThenMoveToReadyCheck() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(5));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Charlie", new JoinGameAction(createTotem(TotemColor.PURPLE)));
        controller.onMessageReceived("Dario", new JoinGameAction(createTotem(TotemColor.YELLOW)));
        controller.onMessageReceived("Eve", new JoinGameAction(createTotem(TotemColor.WHITE)));

        assertEquals(5, controller.getLobbyPlayers().size());
        assertInstanceOf(ReadyCheckState.class, controller.getCurrentState());
    }

    @Test
    void sixthPlayerCannotJoinAfterFivePlayerLobbyIsFull() {
        GameController controller = createReadyCheckControllerWithFivePlayers();

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Frank", new JoinGameAction(createTotem(TotemColor.RED))));
        assertEquals(5, controller.getLobbyPlayers().size());
    }

    @Test
    void nullTotemJoinIsRejected() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Alice", new JoinGameAction(null)));
        assertTrue(controller.getLobbyPlayers().isEmpty());
    }

    /** BUG-HUNTER: current code may accept a Totem object with null color. */
    @Test
    void totemWithNullColorShouldBeRejected() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Alice", new JoinGameAction(new Totem(null, "OFFER_TRACK"))));
        assertTrue(controller.getLobbyPlayers().isEmpty());
    }

    /** BUG-HUNTER: current code compares Totem objects, not TotemColor. */
    @Test
    void duplicateTotemColorShouldBeRejectedEvenWhenTotemObjectsAreDifferent() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.RED))));
        assertEquals(1, controller.getLobbyPlayers().size());
    }

    /** BUG-HUNTER: cannot choose 2 players after 3 players are already in lobby. */
    @Test
    void chosenPlayerCountCannotBeLowerThanCurrentLobbySize() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Charlie", new JoinGameAction(createTotem(TotemColor.PURPLE)));

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Alice", new SetPlayerCountAction(2)));
    }

    @Test
    void lobbyPlayersListCannotBeModifiedFromOutside() {
        GameController controller = new GameController();
        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertThrows(UnsupportedOperationException.class,
                () -> controller.getLobbyPlayers().add(new Player("Injected", createTotem(TotemColor.BLUE))));
        assertEquals(1, controller.getLobbyPlayers().size());
    }

    @Test
    void firstPlayerLeavingBeforeChoosingCountAllowsSecondPlayerToBecomeFirst() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onPlayerDisconnection("Alice");

        assertTrue(controller.isFirstPlayer("Bob"));
        assertDoesNotThrow(() -> controller.onMessageReceived("Bob", new SetPlayerCountAction(2)));
        assertEquals(2, controller.getChosenPlayersCount());
    }

    /** BUG-HUNTER / design decision: host leaves after choosing count. */
    @Test
    void promotedFirstPlayerCanChangePreviouslyChosenCount() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(3));

        controller.onPlayerDisconnection("Alice");

        assertEquals("Bob", controller.getLobbyPlayers().get(0).getNickname());
        assertEquals(3, controller.getChosenPlayersCount());
        assertTrue(controller.isFirstPlayer("Bob"));

        assertDoesNotThrow(() -> controller.onMessageReceived("Bob", new SetPlayerCountAction(2)));
        assertEquals(2, controller.getChosenPlayersCount());
    }
    @Test
    void allPlayersLeavingLobbyResetsCountAndReadySet() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));
        controller.onPlayerDisconnection("Alice");

        assertTrue(controller.getLobbyPlayers().isEmpty());
        assertEquals(-1, controller.getChosenPlayersCount());
        assertFalse(controller.isPlayerReady("Alice"));
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    @Test
    void unknownLobbyDisconnectionIsIgnoredAndDoesNotNotify() {
        GameController controller = new GameController();
        RecordingListener listener = new RecordingListener();
        controller.addListener(listener);

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onPlayerDisconnection("Ghost");

        assertEquals(1, controller.getLobbyPlayers().size());
        assertFalse(listener.events.contains("disconnected:Ghost"));
    }

    @Test
    void directRemoveAndDirectDisconnectRejectUnknownPlayers() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () -> controller.removePlayerFromLobby("Ghost"));
        assertThrows(IllegalArgumentException.class, () -> controller.markPlayerDisconnected("Ghost"));
    }

    @Test
    void directAddLobbyPlayerRejectsNull() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () -> controller.addLobbyPlayer(null));
    }

    // ---------------------------------------------------------------------
    // READY CHECK
    // ---------------------------------------------------------------------

    @Test
    void readyTrueMarksPlayerReadyAndReadyFalseUnmarks() {
        GameController controller = createReadyCheckController();

        controller.onMessageReceived("Alice", new ReadyAction(true));
        assertTrue(controller.isPlayerReady("Alice"));

        controller.onMessageReceived("Alice", new ReadyAction(false));
        assertFalse(controller.isPlayerReady("Alice"));
        assertInstanceOf(ReadyCheckState.class, controller.getCurrentState());
    }

    @Test
    void sendingReadyTrueTwiceIsIdempotentUntilEveryoneIsReady() {
        GameController controller = createReadyCheckControllerWithThreePlayers();

        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onMessageReceived("Bob", new ReadyAction(true));

        assertTrue(controller.isPlayerReady("Alice"));
        assertTrue(controller.isPlayerReady("Bob"));
        assertFalse(controller.isPlayerReady("Charlie"));
        assertInstanceOf(ReadyCheckState.class, controller.getCurrentState());
    }

    @Test
    void unknownPlayerCannotSendReadyTrue() {
        GameController controller = createReadyCheckController();

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Ghost", new ReadyAction(true)));
    }

    /** BUG-HUNTER: current ReadyAction(false) may remove from set without validating player existence. */
    @Test
    void unknownPlayerCannotSendReadyFalse() {
        GameController controller = createReadyCheckController();

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Ghost", new ReadyAction(false)));
    }

    @Test
    void readyCheckRejectsGameActionsButAllowsFirstPlayerToChangeCount() {
        GameController controller = createReadyCheckController();

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.PURPLE))));

        assertDoesNotThrow(() -> controller.onMessageReceived("Alice", new SetPlayerCountAction(3)));
        assertEquals(3, controller.getChosenPlayersCount());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new PlaceTotemAction('A')));

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new PickCardAction("1", true)));

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new PickBuildingAction("1", true)));
    }

    @Test
    void disconnectionInReadyCheckReturnsToLobbyAndClearsThatPlayerReadyStatus() {
        GameController controller = createReadyCheckController();

        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onPlayerDisconnection("Bob");

        assertInstanceOf(LobbyState.class, controller.getCurrentState());
        assertEquals(1, controller.getLobbyPlayers().size());
        assertEquals("Alice", controller.getLobbyPlayers().get(0).getNickname());
        assertFalse(controller.isPlayerReady("Bob"));
        assertNull(controller.getGame());
    }

    /** BUG-HUNTER: changeState should not leave controller in a state whose onEnter failed. */
    @Test
    void failedStateTransitionShouldKeepPreviousState() {
        GameController controller = new GameController();

        assertThrows(IllegalStateException.class,
                () -> controller.changeState(new ReadyCheckState()));

        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    @Test
    void changingToNullStateIsRejected() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () -> controller.changeState(null));
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    // ---------------------------------------------------------------------
    // LISTENERS
    // ---------------------------------------------------------------------

    @Test
    void nullListenerCannotBeAddedOrRemoved() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () -> controller.addListener(null));
        assertThrows(IllegalArgumentException.class, () -> controller.removeListener(null));
    }

    @Test
    void removedListenerDoesNotReceiveFutureEvents() {
        GameController controller = new GameController();
        RecordingListener listener = new RecordingListener();

        controller.addListener(listener);
        controller.removeListener(listener);
        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertFalse(listener.events.contains("joined:Alice"));
    }

    @Test
    void listenerExceptionDoesNotPreventFollowingListenersFromReceivingEvent() {
        GameController controller = new GameController();
        RecordingListener listener = new RecordingListener();

        controller.addListener(new ThrowingListener());
        controller.addListener(listener);

        assertDoesNotThrow(() ->
                controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED))));
        assertTrue(listener.events.contains("joined:Alice"));
    }

    @Test
    void stateChangeToReadyCheckIsNotified() {
        GameController controller = new GameController();
        RecordingListener listener = new RecordingListener();
        controller.addListener(listener);

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));

        assertTrue(listener.events.contains("count:2"));
        assertTrue(listener.events.contains("state:ReadyCheckState"));
    }

    // ---------------------------------------------------------------------
    // DISCONNECTIONS / RECONNECTIONS / SUSPENSION
    // ---------------------------------------------------------------------

    @Test
    void disconnectionAndReconnectionRejectNullOrBlankNickname() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () -> controller.onPlayerDisconnection(null));
        assertThrows(IllegalArgumentException.class, () -> controller.onPlayerDisconnection("  "));
        assertThrows(IllegalArgumentException.class, () -> controller.onPlayerReconnection(null));
        assertThrows(IllegalArgumentException.class, () -> controller.onPlayerReconnection("  "));
    }

    @Test
    void reconnectingBeforeGameStartsIsRejected() {
        GameController controller = new GameController();
        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertThrows(IllegalArgumentException.class,
                () -> controller.onPlayerReconnection("Alice"));
    }

    @Test
    void unknownGamePlayerReconnectionIsRejected() {
        GameController controller = createStartedTotemPlacementController(2, List.of(
                new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 1)))
        ));

        assertThrows(IllegalArgumentException.class,
                () -> controller.onPlayerReconnection("Ghost"));
    }

    @Test
    void disconnectionOfOnePlayerInThreePlayerGameDoesNotSuspend() {
        GameController controller = createStartedTotemPlacementController(3, List.of(
                new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('C', 1, List.of(new ActionSlot(new FoodSlot(), 1)))
        ));

        controller.onPlayerDisconnection("Charlie");

        assertTrue(controller.isPlayerDisconnected("Charlie"));
        assertFalse(controller.isSuspended());
        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
    }

    @Test
    void disconnectionOfOnePlayerInTwoPlayerGameSuspendsAndBlocksActions() {
        GameController controller = createStartedTotemPlacementController(2, List.of(
                new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 1)))
        ));

        controller.onPlayerDisconnection("Bob");

        assertTrue(controller.isSuspended());
        assertInstanceOf(SuspendedState.class, controller.getCurrentState());
        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new PlaceTotemAction('A')));
    }

    @Test
    void reconnectionResumesFromSuspendedState() {
        GameController controller = createStartedTotemPlacementController(2, List.of(
                new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 1)))
        ));

        controller.onPlayerDisconnection("Bob");
        controller.onPlayerReconnection("Bob");

        assertFalse(controller.isSuspended());
        assertFalse(controller.isPlayerDisconnected("Bob"));
        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
    }

    @Test
    void suspensionResumeKeepsTotemPlacementProgress() {
        GameController controller = createStartedTotemPlacementController(2, List.of(
                new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 1)))
        ));

        controller.onMessageReceived("Alice", new PlaceTotemAction('A'));
        controller.onPlayerDisconnection("Bob");
        controller.onPlayerReconnection("Bob");

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new PlaceTotemAction('B')));
    }

    /** BUG-HUNTER / design choice: reconnecting an already-connected player may be accepted and notified. */
    @Test
    void reconnectingAlreadyConnectedPlayerShouldBeRejectedOrIgnoredWithoutNotification() {
        GameController controller = createStartedTotemPlacementController(2, List.of(
                new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 1)))
        ));
        RecordingListener listener = new RecordingListener();
        controller.addListener(listener);

        assertThrows(IllegalStateException.class,
                () -> controller.onPlayerReconnection("Alice"));
        assertFalse(listener.events.contains("reconnected:Alice"));
    }

    /** BUG-HUNTER / design choice: duplicate disconnection should not produce duplicate notifications. */
    @Test
    void disconnectingSamePlayerTwiceShouldNotNotifyTwice() {
        GameController controller = createStartedTotemPlacementController(3, List.of(
                new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('C', 1, List.of(new ActionSlot(new FoodSlot(), 1)))
        ));
        RecordingListener listener = new RecordingListener();
        controller.addListener(listener);

        controller.onPlayerDisconnection("Charlie");
        controller.onPlayerDisconnection("Charlie");

        assertEquals(1, listener.countEvents("disconnected:Charlie"));
    }

    @Test
    void invalidSuspensionTimeoutIsRejected() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () -> controller.setSuspensionTimeoutMillis(0));
        assertThrows(IllegalArgumentException.class, () -> controller.setSuspensionTimeoutMillis(-1));
    }

    @Test
    void suspensionTimeoutDoesNothingIfGameIsNotSuspended() {
        GameController controller = createStartedTotemPlacementController(2, List.of(
                new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 1)))
        ));

        assertFalse(controller.checkSuspensionTimeout(System.currentTimeMillis() + 1_000_000));
    }

    @Test
    void suspensionTimeoutBeforeThresholdDoesNotEndGameButAtThresholdDoes() {
        GameController controller = createStartedTotemPlacementController(2, List.of(
                new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 1)))
        ));
        RecordingListener listener = new RecordingListener();
        controller.addListener(listener);
        controller.setSuspensionTimeoutMillis(10L);

        controller.onPlayerDisconnection("Bob");
        long start = controller.getSuspensionStartedAtMillis();

        assertFalse(controller.checkSuspensionTimeout(start + 9L));
        assertTrue(controller.checkSuspensionTimeout(start + 10L));
        assertEquals("Alice", listener.gameEndedWinnerNickname);
        assertTrue(controller.getGame().isGameOver());
    }

    @Test
    void ifAllPlayersAreDisconnectedSuspensionTimeoutDoesNotDeclareWinner() {
        GameController controller = createStartedTotemPlacementController(2, List.of(
                new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 1)))
        ));
        controller.setSuspensionTimeoutMillis(10L);

        controller.onPlayerDisconnection("Bob");
        long start = controller.getSuspensionStartedAtMillis();
        controller.onPlayerDisconnection("Alice");

        assertFalse(controller.checkSuspensionTimeout(start + 10L));
    }

    @Test
    void setGameNullClearsSuspensionData() {
        GameController controller = createStartedTotemPlacementController(2, List.of(
                new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 1)))
        ));

        controller.onPlayerDisconnection("Bob");
        assertTrue(controller.isSuspended());

        controller.setGame(null);

        assertNull(controller.getGame());
        assertFalse(controller.isSuspended());
        assertEquals(-1L, controller.getSuspensionStartedAtMillis());
    }

    // ---------------------------------------------------------------------
    // TOTEM PLACEMENT
    // ---------------------------------------------------------------------

    @Test
    void unknownPlayerCannotPlaceTotem() {
        GameController controller = createStartedTotemPlacementController(2, defaultTwoPlayerTiles());

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Ghost", new PlaceTotemAction('A')));
    }

    @Test
    void unknownOrLowercaseTileIsRejected() {
        GameController controller = createStartedTotemPlacementController(2, defaultTwoPlayerTiles());

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Alice", new PlaceTotemAction('Z')));
        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Alice", new PlaceTotemAction('a')));
    }

    @Test
    void tileUnavailableForCurrentPlayerCountIsRejected() {
        GameController controller = createStartedTotemPlacementController(2, List.of(
                new OfferTile('A', 3, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 1)))
        ));

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new PlaceTotemAction('A')));
    }

    @Test
    void samePlayerCannotPlaceTwiceAndTwoPlayersCannotUseSameTile() {
        GameController controller = createStartedTotemPlacementController(2, defaultTwoPlayerTiles());

        controller.onMessageReceived("Alice", new PlaceTotemAction('A'));

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new PlaceTotemAction('B')));
        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Bob", new PlaceTotemAction('A')));
    }

    @Test
    void disconnectedPlayerCannotPlaceTotem() {
        GameController controller = createStartedTotemPlacementController(3, List.of(
                new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('C', 1, List.of(new ActionSlot(new FoodSlot(), 1)))
        ));

        controller.onPlayerDisconnection("Charlie");

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Charlie", new PlaceTotemAction('A')));
    }

    @Test
    void totemPlacementNotifiesListenerAndMovesForwardWhenAllConnectedPlayersPlaced() {
        GameController controller = createStartedTotemPlacementController(2, defaultTwoPlayerTiles());
        RecordingListener listener = new RecordingListener();
        controller.addListener(listener);

        controller.onMessageReceived("Alice", new PlaceTotemAction('A'));
        controller.onMessageReceived("Bob", new PlaceTotemAction('B'));

        assertTrue(listener.events.contains("placed:Alice:A"));
        assertTrue(listener.events.contains("placed:Bob:B"));
        assertEquals(2, controller.getGame().getCurrentRound().getNumber());
    }

    @Test
    void inThreePlayerPlacementDisconnectedPlayerIsSkippedWhenRemainingPlayersPlaced() {
        GameController controller = createStartedTotemPlacementController(3, List.of(
                new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('C', 1, List.of(new ActionSlot(new FoodSlot(), 1)))
        ));

        controller.onMessageReceived("Alice", new PlaceTotemAction('A'));
        controller.onPlayerDisconnection("Charlie");
        controller.onMessageReceived("Bob", new PlaceTotemAction('B'));

        assertTrue(controller.isPlayerDisconnected("Charlie"));
        assertEquals(2, controller.getGame().getCurrentRound().getNumber());
    }

    /** BUG-HUNTER: rules say an empty required row gives nothing and the turn should continue. */
    @Test
    void emptyRequiredRowsShouldNotLeaveControllerWaitingForImpossiblePick() {
        GameController controller = createStartedTotemPlacementController(2, List.of(
                new OfferTile('A', 1, List.of(new ActionSlot(new LowerRowSlot(), 1))),
                new OfferTile('B', 1, List.of(new ActionSlot(new UpperRowSlot(), 1)))
        ));

        controller.onMessageReceived("Alice", new PlaceTotemAction('A'));
        controller.onMessageReceived("Bob", new PlaceTotemAction('B'));

        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
    }

    // ---------------------------------------------------------------------
    // ACTION RESOLUTION: TURN ORDER, PICKS, BUILDINGS, EXTRA PICKS
    // ---------------------------------------------------------------------

    @Test
    void actionResolutionUsesOfferTileLetterOrderNotPlacementOrder() {
        ActionScenario scenario = createScenarioWhereBobFoodTileResolvesBeforeAliceUpperPick();
        GameController controller = scenario.controller;

        // Bob had tile A, so his automatic food action should have already resolved.
        assertTrue(scenario.bob.getFood() > 0);

        // Alice on tile B should now be the current waiting player.
        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Bob", new PickCardAction("101", true)));
        assertDoesNotThrow(() -> controller.onMessageReceived("Alice", new PickCardAction("101", true)));
    }

    @Test
    void currentPlayerCannotPickFromWrongRow() {
        ActionScenario scenario = createScenarioWhereBobFoodTileResolvesBeforeAliceUpperPick();

        assertThrows(IllegalStateException.class,
                () -> scenario.controller.onMessageReceived("Alice", new PickCardAction("101", false)));
    }

    @Test
    void currentPlayerCannotPickUnknownCardId() {
        ActionScenario scenario = createScenarioWhereBobFoodTileResolvesBeforeAliceUpperPick();

        assertThrows(IllegalArgumentException.class,
                () -> scenario.controller.onMessageReceived("Alice", new PickCardAction("9999", true)));
    }

    @Test
    void currentPlayerCannotUseTotemPlacementActionDuringActionResolution() {
        ActionScenario scenario = createScenarioWhereBobFoodTileResolvesBeforeAliceUpperPick();

        assertThrows(IllegalStateException.class,
                () -> scenario.controller.onMessageReceived("Alice", new PlaceTotemAction('A')));
    }

    @Test
    void validCharacterPickRemovesCardFromRowAndAddsItToTribe() {
        ActionScenario scenario = createScenarioWhereBobFoodTileResolvesBeforeAliceUpperPick();

        scenario.controller.onMessageReceived("Alice", new PickCardAction("101", true));

        assertEquals(1, scenario.alice.getTribe().countCharacterCards());
        assertTrue(scenario.round.getUpperRowTribute().stream().noneMatch(card -> card.getId() == 101));
    }

    @Test
    void tileWithUpperAndLowerPicksAllowsPlayerToChooseOrderButNotEndEarly() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        OfferTile aliceTile = new OfferTile('A', 1, List.of(
                new ActionSlot(new UpperRowSlot(), 1),
                new ActionSlot(new LowerRowSlot(), 1)
        ));
        OfferTile bobTile = new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 0)));
        Round round = createRoundWithRows(
                List.of(new HunterCard(201, Era.I, 1, false)),
                List.of(new HunterCard(202, Era.I, 1, false)),
                new ArrayList<>(),
                new ArrayList<>()
        );
        GameController controller = createActionResolutionController(
                List.of(alice, bob),
                List.of(aliceTile, bobTile),
                round,
                Map.of(alice, aliceTile, bob, bobTile)
        );

        controller.onMessageReceived("Alice", new PickCardAction("201", false));

        assertEquals(1, alice.getTribe().countCharacterCards());
        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Bob", new PickCardAction("202", true)));

        controller.onMessageReceived("Alice", new PickCardAction("202", true));
        assertEquals(2, alice.getTribe().countCharacterCards());
    }

    @Test
    void unaffordableBuildingIsRejectedAndRemainsOnBoard() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        BuildingCard expensive = new BuildingCard(301, Era.I, 10, new EndPPEffect(), 0);
        OfferTile aliceTile = new OfferTile('A', 1, List.of(new ActionSlot(new UpperRowSlot(), 1)));
        OfferTile bobTile = new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 0)));
        Round round = createRoundWithRows(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), List.of(expensive));

        GameController controller = createActionResolutionController(
                List.of(alice, bob),
                List.of(aliceTile, bobTile),
                round,
                Map.of(alice, aliceTile, bob, bobTile)
        );

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new PickBuildingAction("301", true)));
        assertTrue(round.getUpperRowBuildings().contains(expensive));
        assertTrue(alice.getTribe().getBuildingCards().isEmpty());
    }

    @Test
    void affordableBuildingIsTakenAndFoodIsSpent() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));

        alice.addFood(5);

        BuildingCard building = new BuildingCard(302, Era.I, 3, new EndPPEffect(), 0);

        OfferTile aliceTile = new OfferTile('A', 1, List.of(new ActionSlot(new UpperRowSlot(), 1)));
        OfferTile bobTile = new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 0)));

        Round round = createRoundWithRows(
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                List.of(building)
        );

        GameController controller = createActionResolutionController(
                List.of(alice, bob),
                List.of(aliceTile, bobTile),
                round,
                Map.of(alice, aliceTile, bob, bobTile)
        );

        controller.onMessageReceived("Alice", new PickBuildingAction("302", true));

        // Alice starts with 5 food, pays 3 for the building, then receives +1 food
        // when her totem returns to the first turn-order slot.
        assertEquals(3, alice.getFood());

        assertEquals(1, alice.getTribe().getBuildingCards().size());
        assertFalse(round.getUpperRowBuildings().contains(building));
    }

    @Test
    void disconnectedCurrentPlayerIsSkippedInActionResolution() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        OfferTile aliceTile = new OfferTile('A', 1, List.of(new ActionSlot(new UpperRowSlot(), 1)));
        OfferTile bobTile = new OfferTile('B', 1, List.of(new ActionSlot(new UpperRowSlot(), 1)));
        Round round = createRoundWithRows(new ArrayList<>(),
                List.of(new HunterCard(401, Era.I, 1, false), new HunterCard(402, Era.I, 1, false)),
                new ArrayList<>(), new ArrayList<>());

        GameController controller = createBaseGameController(List.of(alice, bob), List.of(aliceTile, bobTile), round);
        Round actualRound = controller.getGame().getCurrentRound();
        OfferTile actualAliceTile = controller.getGame().getBoard().getOfferTiles().get(0);
        OfferTile actualBobTile = controller.getGame().getBoard().getOfferTiles().get(1);
        actualRound.placeTotemOffer(alice, actualAliceTile);
        actualRound.placeTotemOffer(bob, actualBobTile);
        controller.markPlayerDisconnected("Alice");
        controller.changeState(new ActionResolutionState());

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new PickCardAction("401", true)));
        assertDoesNotThrow(() -> controller.onMessageReceived("Bob", new PickCardAction("401", true)));
    }

    @Test
    void extraPickPhaseRejectsLowerRowPick() {
        ExtraPickScenario scenario = createExtraPickScenario();

        assertTrue(scenario.listener.events.contains("extra:Alice:1"));
        assertThrows(IllegalStateException.class,
                () -> scenario.controller.onMessageReceived("Alice", new PickCardAction("501", false)));
    }

    @Test
    void extraPickPhaseRejectsUnknownUpperCard() {
        ExtraPickScenario scenario = createExtraPickScenario();

        assertThrows(IllegalArgumentException.class,
                () -> scenario.controller.onMessageReceived("Alice", new PickCardAction("999", true)));
    }

    @Test
    void validExtraPickConsumesUpperCardAndContinuesRound() {
        ExtraPickScenario scenario = createExtraPickScenario();

        scenario.controller.onMessageReceived("Alice", new PickCardAction("501", true));

        assertEquals(1, scenario.alice.getTribe().countCharacterCards());
        assertTrue(scenario.round.getUpperRowTribute().stream().noneMatch(card -> card.getId() == 501));
    }

    @Test
    void disconnectedExtraPickPlayerIsSkipped() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        alice.addBuildingCard(new BuildingCard(601, Era.I, 0, new ExtraCardUpperRowEffect(), 0));
        OfferTile aliceTile = new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 0)));
        OfferTile bobTile = new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 0)));
        Round round = createRoundWithRows(new ArrayList<>(),
                List.of(new HunterCard(602, Era.I, 1, false)),
                new ArrayList<>(), new ArrayList<>());
        GameController controller = createBaseGameController(List.of(alice, bob), List.of(aliceTile, bobTile), round);
        round.placeTotemOffer(alice, aliceTile);
        round.placeTotemOffer(bob, bobTile);
        controller.markPlayerDisconnected("Alice");

        assertDoesNotThrow(() -> controller.changeState(new ActionResolutionState()));
        assertFalse(controller.getCurrentState() instanceof ActionResolutionState);
    }

    // ---------------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------------

    private GameController createReadyCheckController() {
        GameController controller = new GameController();
        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));
        assertInstanceOf(ReadyCheckState.class, controller.getCurrentState());
        return controller;
    }

    private GameController createReadyCheckControllerWithThreePlayers() {
        GameController controller = new GameController();
        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Charlie", new JoinGameAction(createTotem(TotemColor.PURPLE)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(3));
        assertInstanceOf(ReadyCheckState.class, controller.getCurrentState());
        return controller;
    }

    private GameController createReadyCheckControllerWithFivePlayers() {
        GameController controller = new GameController();
        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(5));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Charlie", new JoinGameAction(createTotem(TotemColor.PURPLE)));
        controller.onMessageReceived("Dario", new JoinGameAction(createTotem(TotemColor.YELLOW)));
        controller.onMessageReceived("Eve", new JoinGameAction(createTotem(TotemColor.WHITE)));
        assertInstanceOf(ReadyCheckState.class, controller.getCurrentState());
        return controller;
    }

    private GameController createStartedTotemPlacementController(int playersCount, List<OfferTile> offerTiles) {
        List<Player> players = createPlayers(playersCount);
        Round round = createRoundWithRows(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        return createBaseGameController(players, offerTiles, round, new TotemPlacementState());
    }

    private GameController createBaseGameController(List<Player> players, List<OfferTile> offerTiles, Round round) {
        return createBaseGameController(players, offerTiles, round, null);
    }

    private GameController createBaseGameController(List<Player> players,
                                                    List<OfferTile> offerTiles,
                                                    Round round,
                                                    GameState stateToEnter) {
        GameController controller = new GameController();
        Board board = new Board(new ArrayList<>(offerTiles), TurnOrderTile.createForPlayers(players.size()));
        Game game = new Game(players, 1, null, Era.I, null, board,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        game.setCurrentRound(round);

        // The Round constructor receives a Game, so rebuild it if the passed round belongs to a temporary game.
        Round actualRound = new Round(round.getNumber(), game, round.getPhase(), round.getEra(),
                new ArrayList<>(round.getLowerRowEvents()),
                new ArrayList<>(round.getUpperRowEvents()),
                new ArrayList<>(round.getLowerRowTribute()),
                new ArrayList<>(round.getUpperRowTribute()),
                new ArrayList<>(round.getLowerRowBuildings()),
                new ArrayList<>(round.getUpperRowBuildings()));
        game.setCurrentRound(actualRound);

        for (Player player : players) {
            player.setGame(game);
            controller.addLobbyPlayer(player);
        }
        controller.setGame(game);
        if (stateToEnter != null) {
            controller.changeState(stateToEnter);
        }
        return controller;
    }

    private GameController createActionResolutionController(List<Player> players,
                                                            List<OfferTile> offerTiles,
                                                            Round round,
                                                            Map<Player, OfferTile> choices) {
        GameController controller = createBaseGameController(players, offerTiles, round);
        Round actualRound = controller.getGame().getCurrentRound();
        for (Map.Entry<Player, OfferTile> entry : choices.entrySet()) {
            OfferTile matchingTile = controller.getGame().getBoard().getOfferTiles().stream()
                    .filter(tile -> tile.getLetter() == entry.getValue().getLetter())
                    .findFirst()
                    .orElseThrow();
            actualRound.placeTotemOffer(entry.getKey(), matchingTile);
        }
        controller.changeState(new ActionResolutionState());
        return controller;
    }

    private ActionScenario createScenarioWhereBobFoodTileResolvesBeforeAliceUpperPick() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        OfferTile aliceTile = new OfferTile('B', 1, List.of(new ActionSlot(new UpperRowSlot(), 1)));
        OfferTile bobTile = new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 3)));
        Round round = createRoundWithRows(new ArrayList<>(),
                List.of(new HunterCard(101, Era.I, 1, false)),
                new ArrayList<>(),
                new ArrayList<>());

        GameController controller = createActionResolutionController(
                List.of(alice, bob),
                List.of(aliceTile, bobTile),
                round,
                Map.of(alice, aliceTile, bob, bobTile)
        );
        return new ActionScenario(controller, alice, bob, controller.getGame().getCurrentRound());
    }

    private ExtraPickScenario createExtraPickScenario() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        alice.addBuildingCard(new BuildingCard(500, Era.I, 0, new ExtraCardUpperRowEffect(), 0));
        OfferTile aliceTile = new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 0)));
        OfferTile bobTile = new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 0)));
        Round round = createRoundWithRows(new ArrayList<>(),
                List.of(new HunterCard(501, Era.I, 1, false)),
                new ArrayList<>(), new ArrayList<>());
        RecordingListener listener = new RecordingListener();
        GameController controller = createBaseGameController(List.of(alice, bob), List.of(aliceTile, bobTile), round);
        controller.addListener(listener);
        controller.getGame().getCurrentRound().placeTotemOffer(alice, controller.getGame().getBoard().getOfferTiles().get(0));
        controller.getGame().getCurrentRound().placeTotemOffer(bob, controller.getGame().getBoard().getOfferTiles().get(1));
        controller.changeState(new ActionResolutionState());
        return new ExtraPickScenario(controller, alice, bob, controller.getGame().getCurrentRound(), listener);
    }

    private Round createRoundWithRows(List<CharacterCard> lowerTribute,
                                      List<CharacterCard> upperTribute,
                                      List<BuildingCard> lowerBuildings,
                                      List<BuildingCard> upperBuildings) {
        Game placeholderGame = new Game(new ArrayList<>(), 1, null, Era.I, null,
                new Board(new ArrayList<>(), TurnOrderTile.createForPlayers(2)),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        return new Round(1, placeholderGame, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(lowerTribute), new ArrayList<>(upperTribute),
                new ArrayList<>(lowerBuildings), new ArrayList<>(upperBuildings));
    }

    private List<OfferTile> defaultTwoPlayerTiles() {
        return List.of(
                new OfferTile('A', 1, List.of(new ActionSlot(new FoodSlot(), 1))),
                new OfferTile('B', 1, List.of(new ActionSlot(new FoodSlot(), 1)))
        );
    }

    private List<Player> createPlayers(int count) {
        List<Player> players = new ArrayList<>();
        TotemColor[] colors = {TotemColor.RED, TotemColor.BLUE, TotemColor.PURPLE, TotemColor.YELLOW, TotemColor.WHITE};
        String[] names = {"Alice", "Bob", "Charlie", "Dario", "Eve"};
        for (int i = 0; i < count; i++) {
            players.add(new Player(names[i], createTotem(colors[i])));
        }
        return players;
    }

    private static Totem createTotem(TotemColor color) {
        return new Totem(color, "OFFER_TRACK");
    }

    private static final class ActionScenario {
        private final GameController controller;
        private final Player alice;
        private final Player bob;
        private final Round round;

        private ActionScenario(GameController controller, Player alice, Player bob, Round round) {
            this.controller = controller;
            this.alice = alice;
            this.bob = bob;
            this.round = round;
        }
    }

    private static final class ExtraPickScenario {
        private final GameController controller;
        private final Player alice;
        private final Player bob;
        private final Round round;
        private final RecordingListener listener;

        private ExtraPickScenario(GameController controller, Player alice, Player bob, Round round, RecordingListener listener) {
            this.controller = controller;
            this.alice = alice;
            this.bob = bob;
            this.round = round;
            this.listener = listener;
        }
    }

    private static final class ThrowingListener implements GameListener {
        @Override public void onPlayerJoined(String nickname) { throw new RuntimeException("Simulated listener failure"); }
        @Override public void onPlayersCountSet(int count) { throw new RuntimeException("Simulated listener failure"); }
        @Override public void onPlayerDisconnected(String nickname) { throw new RuntimeException("Simulated listener failure"); }
        @Override public void onPlayerReconnected(String nickname) { throw new RuntimeException("Simulated listener failure"); }
        @Override public void onGameSuspended() { throw new RuntimeException("Simulated listener failure"); }
        @Override public void onGameResumed() { throw new RuntimeException("Simulated listener failure"); }
        @Override public void onGameStarted() { throw new RuntimeException("Simulated listener failure"); }
        @Override public void onStateChanged(GameState newState) { throw new RuntimeException("Simulated listener failure"); }
        @Override public void onTotemPlaced(String nickname, char tileId) { throw new RuntimeException("Simulated listener failure"); }
        @Override public void onFoodChanged(String nickname, int newAmount, int delta) { throw new RuntimeException("Simulated listener failure"); }
        @Override public void onPointsChanged(String nickname, int newAmount, int delta) { throw new RuntimeException("Simulated listener failure"); }
        @Override public void onEraChanged(Era newEra) { throw new RuntimeException("Simulated listener failure"); }
        @Override public void onCharacterCardTaken(String nickname, String cardId, boolean fromUpperRow) { throw new RuntimeException("Simulated listener failure"); }
        @Override public void onBuildingCardTaken(String nickname, String cardId, boolean fromUpperRow) { throw new RuntimeException("Simulated listener failure"); }
        @Override public void onRoundEnded(int roundNumber) { throw new RuntimeException("Simulated listener failure"); }
        @Override public void onGameEnded(Player winner, Map<Player, Integer> finalScores) { throw new RuntimeException("Simulated listener failure"); }
        @Override public void onExtraCardPickAvailable(String nickname, int picks) { throw new RuntimeException("Simulated listener failure"); }
    }

    private static final class RecordingListener implements GameListener {
        private final List<String> events = new ArrayList<>();
        private String gameEndedWinnerNickname;

        int countEvents(String expected) {
            int count = 0;
            for (String event : events) {
                if (event.equals(expected)) count++;
            }
            return count;
        }

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
        @Override public void onRoundEnded(int roundNumber) { events.add("roundEnded:" + roundNumber); }
        @Override public void onGameEnded(Player winner, Map<Player, Integer> finalScores) { gameEndedWinnerNickname = winner == null ? null : winner.getNickname(); events.add("ended:" + gameEndedWinnerNickname); }
        @Override public void onExtraCardPickAvailable(String nickname, int picks) { events.add("extra:" + nickname + ":" + picks); }
    }
}
