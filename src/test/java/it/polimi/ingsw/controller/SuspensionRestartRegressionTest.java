package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.JoinGameAction;
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
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the FA3 (persistence) + FA4 (suspension) interaction.
 *
 * <p>When a server crashes while the match is suspended (only one player left),
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

    /** Mirrors what Persistence.load + LobbyManager do on a server restart. */
    private GameController restartRoundTrip(GameController controller) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(baos)) {
            out.writeObject(controller);
        }
        GameController restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            restored = (GameController) in.readObject();
        }
        if (restored.getGame() != null) {
            restored.getGame().setNotifier(restored::notifyListeners); // Persistence.load does this
        }
        restored.markAllPlayersDisconnected();                          // LobbyManager does this on restore
        return restored;
    }

    private GameController createStartedGameController(int playerCount) {
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
        Game game = new Game(players, 1, null, Era.I, null, board,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        Round round = new Round(1, game, Era.I,
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
}
