package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.controller.state.ActionResolutionState;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.ActionSlot;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.LowerRowSlot;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.board.TurnOrderTile;
import it.polimi.ingsw.model.board.UpperRowSlot;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.effect.EndPPEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Strong controller tests for invalid actions and model immutability after rejected inputs.
 */
public class GameControllerInvalidActionsTest {

    /** Unknown players must not be able to act or mutate the game during action resolution. */
    @Test
    void unknownPlayerActionDuringActionResolutionIsRejectedAndDoesNotMutateModel() {
        GameController controller = createActionResolutionControllerWithPendingLowerPick();
        String before = modelSignature(controller);

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Mallory", new PickCardAction("101", false)));

        assertEquals(before, modelSignature(controller));
        assertEquals("Alice", controller.getCurrentActionPlayerNickname());
    }

    /** Null, blank and control-character nicknames must be rejected before reaching any state logic. */
    @Test
    void invalidNicknameIsRejectedBeforeDelegatingToCurrentState() {
        GameController controller = createTotemPlacementController();
        String before = modelSignature(controller);

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived(null, new PlaceTotemAction('A')));

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("   ", new PlaceTotemAction('A')));

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Ali\nce", new PlaceTotemAction('A')));
        assertEquals(before, modelSignature(controller));
        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
    }

    /** Null actions must be rejected explicitly and must not modify lobby or game data. */
    @Test
    void nullActionIsRejectedWithoutChangingControllerData() {
        GameController controller = createTotemPlacementController();
        String before = modelSignature(controller);

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Alice", null));

        assertEquals(before, modelSignature(controller));
        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
    }

    /** ReadyAction is only meaningful in lobby/ready-check states and must be rejected during the game. */
    @Test
    void readyActionDuringTotemPlacementIsRejectedAndDoesNotChangeModel() {
        GameController controller = createTotemPlacementController();
        String before = modelSignature(controller);

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new ReadyAction(true)));

        assertEquals(before, modelSignature(controller));
        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
    }

    /** The expected player count must not be changed after the lobby has already produced a game. */
    @Test
    void setPlayerCountAfterGameStartedIsRejectedAndDoesNotChangeModel() {
        GameController controller = createStartedControllerThroughLobbyFlow();
        String before = modelSignature(controller);
        int chosenPlayersCount = controller.getChosenPlayersCount();

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new SetPlayerCountAction(3)));

        assertEquals(chosenPlayersCount, controller.getChosenPlayersCount());
        assertEquals(before, modelSignature(controller));
        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
    }

    /** Picking a building before all totems are placed must be rejected and must not create player choices. */
    @Test
    void pickBuildingDuringTotemPlacementIsRejectedAndDoesNotCreateChoices() {
        GameController controller = createTotemPlacementController();
        String before = modelSignature(controller);

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new PickBuildingAction("201", true)));

        assertEquals(before, modelSignature(controller));
        assertTrue(controller.getGame().getCurrentRound().getPlayerChoices().isEmpty());
        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
    }

    /** Placing a totem during action resolution must be rejected and must not resolve or skip the pending pick. */
    @Test
    void placeTotemDuringActionResolutionIsRejectedAndDoesNotAdvanceResolution() {
        GameController controller = createActionResolutionControllerWithPendingLowerPick();
        String before = modelSignature(controller);

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new PlaceTotemAction('B')));

        assertEquals(before, modelSignature(controller));
        assertEquals("Alice", controller.getCurrentActionPlayerNickname());
        assertInstanceOf(ActionResolutionState.class, controller.getCurrentState());
    }

    private static GameController createStartedControllerThroughLobbyFlow() {
        GameController controller = new GameController();

        controller.onMessageReceived(
                "Alice",
                new it.polimi.ingsw.controller.action.JoinGameAction(createTotem(TotemColor.RED))
        );

        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));

        controller.onMessageReceived(
                "Bob",
                new it.polimi.ingsw.controller.action.JoinGameAction(createTotem(TotemColor.BLUE))
        );

        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onMessageReceived("Bob", new ReadyAction(true));

        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
        return controller;
    }

    private static GameController createTotemPlacementController() {
        GameController controller = new GameController();

        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        List<Player> players = new ArrayList<>(List.of(alice, bob));

        Board board = new Board(
                new ArrayList<>(List.of(
                        new OfferTile('A', 1, List.of(new ActionSlot(new LowerRowSlot(), 1))),
                        new OfferTile('B', 1, List.of(new ActionSlot(new UpperRowSlot(), 1)))
                )),
                TurnOrderTile.createForPlayers(2)
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
                new ArrayList<>(List.of(new HunterCard(101, Era.I, 1, false))),
                new ArrayList<>(List.of(new HunterCard(102, Era.I, 1, false))),
                new ArrayList<>(),
                new ArrayList<>(List.of(new BuildingCard(201, Era.I, 0, new EndPPEffect(), 1)))
        );

        game.setCurrentRound(round);
        alice.setGame(game);
        bob.setGame(game);

        controller.addLobbyPlayer(alice);
        controller.addLobbyPlayer(bob);
        controller.setGame(game);
        controller.changeState(new TotemPlacementState());

        return controller;
    }

    private static GameController createActionResolutionControllerWithPendingLowerPick() {
        GameController controller = createTotemPlacementController();

        controller.onMessageReceived("Alice", new PlaceTotemAction('A'));
        controller.onMessageReceived("Bob", new PlaceTotemAction('B'));

        assertInstanceOf(ActionResolutionState.class, controller.getCurrentState());
        assertEquals("Alice", controller.getCurrentActionPlayerNickname());

        return controller;
    }

    private static Totem createTotem(TotemColor color) {
        return new Totem(color, "OFFER_TRACK");
    }

    private static String modelSignature(GameController controller) {
        Game game = controller.getGame();

        if (game == null) {
            return "no-game|lobby=" + controller.getLobbyPlayers().size()
                    + "|chosen=" + controller.getChosenPlayersCount()
                    + "|ready=" + controller.getReadyPlayers().size();
        }

        Round round = game.getCurrentRound();
        StringBuilder signature = new StringBuilder();

        signature.append("state=").append(controller.getCurrentState().getClass().getSimpleName());
        signature.append("|round=").append(round.getNumber());
        signature.append("|choices=").append(round.getPlayerChoices().size());
        signature.append("|lowerTribute=").append(round.getLowerRowTribute().size());
        signature.append("|upperTribute=").append(round.getUpperRowTribute().size());
        signature.append("|lowerBuildings=").append(round.getLowerRowBuildings().size());
        signature.append("|upperBuildings=").append(round.getUpperRowBuildings().size());

        for (OfferTile tile : game.getBoard().getOfferTiles()) {
            signature.append("|tile").append(tile.getLetter()).append('=').append(tile.isOccupied());
        }

        for (Player player : game.getPlayers()) {
            signature.append("|player=").append(player.getNickname())
                    .append(",food=").append(player.getFood())
                    .append(",pp=").append(player.getPrestigePoints())
                    .append(",chars=").append(player.getTribe().getCharacterCards().size())
                    .append(",buildings=").append(player.getTribe().getBuildingCards().size())
                    .append(",totem=").append(player.getTotem().getPosition());
        }

        return signature.toString();
    }
    /**
     * Replaying the same card-pick action must not advance the turn or mutate the model twice.
     */
    @Test
    void replayedPickCardActionIsRejectedWithoutAdvancingTurn() {
        GameController controller = createActionResolutionControllerWithPendingLowerPick();

        String currentPlayer = controller.getCurrentActionPlayerNickname();
        Round round = controller.getGame().getCurrentRound();

        String cardId = String.valueOf(round.getLowerRowTribute().get(0).getId());

        controller.onMessageReceived(currentPlayer, new PickCardAction(cardId, false));

        String afterFirstPickSignature = modelSignature(controller);
        String stillCurrentPlayer = controller.getCurrentActionPlayerNickname();

        assertThrows(RuntimeException.class,
                () -> controller.onMessageReceived(currentPlayer, new PickCardAction(cardId, false)));
        assertEquals(afterFirstPickSignature, modelSignature(controller));
        assertEquals(stillCurrentPlayer, controller.getCurrentActionPlayerNickname());
    }
}