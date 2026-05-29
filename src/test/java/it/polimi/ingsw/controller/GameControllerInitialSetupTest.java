package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.controller.state.LobbyState;
import it.polimi.ingsw.controller.state.ReadyCheckState;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameController initial setup.
 *
 * <p>Verifies: game start-up: board, decks, players, totems and first round assembly.
 */
class GameControllerInitialSetupTest {

    /**
     * Verifies that an invalid join action does not mutate the lobby.
     * This protects the lobby from partially accepted players after validation failures.
     */
    @Test
    void invalidJoinWithNullTotemDoesNotMutateLobby() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived("Alice", new JoinGameAction(null))
        );

        assertTrue(controller.getLobbyPlayers().isEmpty());
        assertEquals(-1, controller.getChosenPlayersCount());
        assertNull(controller.getGame());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    /**
     * Verifies that a duplicated nickname is rejected without replacing or modifying
     * the already joined player.
     */
    @Test
    void duplicateNicknameDoesNotReplaceExistingPlayer() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.BLUE)))
        );

        assertEquals(1, controller.getLobbyPlayers().size());
        assertEquals("Alice", controller.getLobbyPlayers().get(0).getNickname());
        assertEquals(TotemColor.RED, controller.getLobbyPlayers().get(0).getTotem().getColor());
        assertNull(controller.getGame());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    /**
     * Verifies that a duplicated totem color is rejected and does not add
     * the second player to the lobby.
     */
    @Test
    void duplicateTotemDoesNotAddSecondPlayer() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.RED)))
        );

        assertEquals(1, controller.getLobbyPlayers().size());
        assertEquals("Alice", controller.getLobbyPlayers().get(0).getNickname());
        assertFalse(controller.containsLobbyPlayer("Bob"));
        assertNull(controller.getGame());
    }

    /**
     * Verifies that the game is not created when the lobby is full but at least
     * one player is not ready.
     */
    @Test
    void fullLobbyDoesNotStartUntilEveryPlayerIsReady() {
        GameController controller = createFullTwoPlayerLobby();

        controller.onMessageReceived("Alice", new ReadyAction(true));

        assertTrue(controller.isPlayerReady("Alice"));
        assertFalse(controller.isPlayerReady("Bob"));
        assertNull(controller.getGame());
        assertInstanceOf(ReadyCheckState.class, controller.getCurrentState());
    }

    /**
     * Verifies that the game starts only after every expected player is ready.
     * It also checks the controller transition into the first real gameplay state.
     */
    @Test
    void allReadyPlayersStartGameAndEnterTotemPlacementState() {
        GameController controller = createStartedTwoPlayerGame();

        assertNotNull(controller.getGame());
        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
        assertEquals(2, controller.getGame().getPlayers().size());
        assertEquals(1, controller.getGame().getCurrentRound().getNumber());
        assertEquals(Era.I, controller.getGame().getCurrentEra());
    }

    /**
     * Verifies that the initial model setup follows the Mesos setup rules:
     * lower row has players + 1 character cards, upper row has players + 4 tribe cards/events,
     * and Era I buildings are revealed.
     */
    @Test
    void startedTwoPlayerGameHasCoherentInitialRowsAndEraOneBuildings() {
        GameController controller = createStartedTwoPlayerGame();
        Game game = controller.getGame();
        Round round = game.getCurrentRound();

        int players = game.getPlayers().size();
        int upperTribeAndEvents =
                round.getUpperRowTribute().size() + round.getUpperRowEvents().size();

        assertEquals(players + 1, round.getLowerRowTribute().size());
        assertEquals(players + 4, upperTribeAndEvents);

        assertTrue(round.getLowerRowEvents().isEmpty(),
                "Initial lower row must contain character cards only; events drawn there must be moved to upper row.");

        assertEquals(1, round.getUpperRowBuildings().size(),
                "In a two-player game exactly one Era I building must be revealed.");

        assertTrue(round.getLowerRowBuildings().isEmpty(),
                "Initial lower building row must be empty before any era transition.");
    }

    /**
     * Verifies that the board and starting resources are coherent after game start.
     * The offer track must contain only tiles available for the player count, the turn-order
     * tile must contain all player totems, and initial food must match turn-order setup rules.
     */
    @Test
    void startedTwoPlayerGameHasCoherentBoardTotemsAndInitialFood() {
        GameController controller = createStartedTwoPlayerGame();
        Game game = controller.getGame();

        assertEquals(2, game.getBoard().getTurnOrderTile().getNumPlayers());
        assertEquals(2, game.getBoard().getTurnOrderTile().getTurnOrder().size());

        for (OfferTile tile : game.getBoard().getOfferTiles()) {
            assertTrue(tile.isAvailableFor(2),
                    "The board must not contain offer tiles unavailable for the current player count.");
            assertFalse(tile.isOccupied(),
                    "No offer tile should be occupied before the first totem placement phase.");
        }

        List<Integer> foodValues = game.getPlayers()
                .stream()
                .map(Player::getFood)
                .sorted()
                .toList();

        assertEquals(List.of(2, 3), foodValues,
                "In a two-player game the first player in turn order receives 2 food and the second receives 3.");
    }

    private GameController createFullTwoPlayerLobby() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));

        assertInstanceOf(ReadyCheckState.class, controller.getCurrentState());
        return controller;
    }

    private GameController createStartedTwoPlayerGame() {
        GameController controller = createFullTwoPlayerLobby();

        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onMessageReceived("Bob", new ReadyAction(true));

        return controller;
    }

    private Totem createTotem(TotemColor color) {
        return new Totem(color, "OFFER_TRACK");
    }
}