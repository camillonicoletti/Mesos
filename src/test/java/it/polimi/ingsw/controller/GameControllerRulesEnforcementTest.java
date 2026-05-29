package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.controller.state.ActionResolutionState;
import it.polimi.ingsw.controller.state.EventResolutionState;
import it.polimi.ingsw.controller.state.ReadyCheckState;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.ActionSlot;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.LowerRowSlot;
import it.polimi.ingsw.model.board.NoSlotEffect;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.board.TurnOrderTile;
import it.polimi.ingsw.model.board.TurnSlot;
import it.polimi.ingsw.model.board.UpperRowSlot;
import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.HuntEventCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.effect.EndPPEffect;
import it.polimi.ingsw.model.card.effect.ExtraCardUpperRowEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.RoundPhase;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for rule-based controller/model behaviors discovered by the full legacy test suite.
 */
public class GameControllerRulesEnforcementTest {

    /** Starting a game through the real lobby flow must create a valid board and enter totem placement. */
    @Test
    void realLobbyStartCreatesBoardAndEntersTotemPlacementWithoutNullBoard() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));

        assertInstanceOf(ReadyCheckState.class, controller.getCurrentState());

        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onMessageReceived("Bob", new ReadyAction(true));

        assertNotNull(controller.getGame());
        assertNotNull(controller.getGame().getBoard());
        assertNotNull(controller.getGame().getBoard().getTurnOrderTile());
        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());
    }

    /** Spending more food than available must fail without silently removing prestige points. */
    @Test
    void spendFoodRejectsInsufficientFoodWithoutChangingPrestige() {
        Player player = new Player("Alice", createTotem(TotemColor.RED));
        player.addFood(1);
        player.addPrestigePoints(5);

        assertThrows(IllegalStateException.class, () -> player.spendFood(2));

        assertEquals(1, player.getFood());
        assertEquals(5, player.getPrestigePoints());
    }

    /** Event cards are never pickable cards during action resolution. */
    @Test
    void eventCardCannotBePickedDuringActionResolution() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));

        OfferTile aliceTile = lowerPickTile('A');
        OfferTile bobTile = noPickTile('B');

        HuntEventCard event = new HuntEventCard(900, Era.I, false, 1, 2);
        HunterCard validCharacter = new HunterCard(101, Era.I, 1, false);

        GameController controller = createActionResolutionController(
                List.of(alice, bob),
                List.of(aliceTile, bobTile),
                new ArrayList<>(List.of(validCharacter)),
                new ArrayList<>(),
                new ArrayList<>(List.of(event)),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );

        assertInstanceOf(ActionResolutionState.class, controller.getCurrentState());

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Alice", new PickCardAction("900", false)));

        assertTrue(controller.getGame().getCurrentRound().getLowerRowEvents().contains(event));
        assertEquals(0, alice.getTribe().countCharacterCards());
        assertEquals("Alice", controller.getCurrentActionPlayerNickname());
    }
    //le building must be taken and the exact discounted cost must be paid. */
    @Test
    void affordableBuildingIsTakenAndExactDiscountedCostIsPaid() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));

        alice.addFood(5);
        alice.addCharacterCard(new BuilderCard(100, Era.I, 1, 0, 1));

        BuildingCard building = new BuildingCard(301, Era.I, 3, new EndPPEffect(), 0);

        GameController controller = createActionResolutionController(
                List.of(alice, bob),
                List.of(upperPickTile('A'), upperPickTile('B')),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                List.of(building)
        );

        controller.onMessageReceived("Alice", new PickBuildingAction("301", true));

        assertEquals(3, alice.getFood());
        assertEquals(1, alice.getTribe().getBuildingCards().size());
        assertFalse(controller.getGame().getCurrentRound().getUpperRowBuildings().contains(building));
    }

    /** Builder discounts must be capped at zero and must never create food. */
    @Test
    void builderDiscountCannotMakeBuildingCostNegativeOrIncreaseFood() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));

        alice.addFood(1);
        alice.addCharacterCard(new BuilderCard(100, Era.I, 1, 0, 5));

        BuildingCard cheapBuilding = new BuildingCard(302, Era.I, 3, new EndPPEffect(), 0);

        GameController controller = createActionResolutionController(
                List.of(alice, bob),
                List.of(upperPickTile('A'), upperPickTile('B')),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                List.of(cheapBuilding)
        );

        controller.onMessageReceived("Alice", new PickBuildingAction("302", true));

        assertEquals(1, alice.getFood());
        assertEquals(1, alice.getTribe().getBuildingCards().size());
        assertFalse(controller.getGame().getCurrentRound().getUpperRowBuildings().contains(cheapBuilding));
    }

    /** Extra upper-row pick must not block the controller when the upper row is empty. */
    @Test
    void extraUpperRowPickWithEmptyUpperRowAutoSkipsToEventResolution() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));

        alice.addBuildingCard(new BuildingCard(400, Era.I, 0, new ExtraCardUpperRowEffect(), 0));

        GameController controller = createActionResolutionController(
                List.of(alice, bob),
                List.of(noPickTile('A'), noPickTile('B')),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );

        assertFalse(controller.getCurrentState() instanceof ActionResolutionState);
        assertInstanceOf(TotemPlacementState.class, controller.getCurrentState());    }

    /** Once ReadyCheckState has started, the chosen player count must not be changed. */
    @Test
    void playerCountCanBeChangedAfterReadyCheckStarted() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));

        assertInstanceOf(ReadyCheckState.class, controller.getCurrentState());
        assertEquals(2, controller.getChosenPlayersCount());

        assertDoesNotThrow(() -> controller.onMessageReceived("Alice", new SetPlayerCountAction(3)));

        assertEquals(3, controller.getChosenPlayersCount());
        assertInstanceOf(it.polimi.ingsw.controller.state.LobbyState.class, controller.getCurrentState());
    }

    private static GameController createActionResolutionController(
            List<Player> players,
            List<OfferTile> offerTiles,
            List<HunterCard> lowerCharacters,
            List<HunterCard> upperCharacters,
            List<HuntEventCard> lowerEvents,
            List<HuntEventCard> upperEvents,
            List<BuildingCard> lowerBuildings,
            List<BuildingCard> upperBuildings
    ) {
        GameController controller = new GameController();

        Board board = new Board(new ArrayList<>(offerTiles), noEffectTurnOrder(players.size()));

        Game game = new Game(
                new ArrayList<>(players),
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
                RoundPhase.ACTION_RESOLUTION,
                Era.I,
                new ArrayList<>(lowerEvents),
                new ArrayList<>(upperEvents),
                new ArrayList<>(lowerCharacters),
                new ArrayList<>(upperCharacters),
                new ArrayList<>(lowerBuildings),
                new ArrayList<>(upperBuildings)
        );

        game.setCurrentRound(round);

        for (Player player : players) {
            player.setGame(game);
            controller.addLobbyPlayer(player);
        }

        controller.setGame(game);

        for (int i = 0; i < players.size(); i++) {
            round.placeTotemOffer(players.get(i), board.getOfferTiles().get(i));
        }

        controller.changeState(new ActionResolutionState());
        return controller;
    }

    private static TurnOrderTile noEffectTurnOrder(int playersCount) {
        List<TurnSlot> slots = new ArrayList<>();
        for (int i = 1; i <= playersCount; i++) {
            slots.add(new TurnSlot(i, new NoSlotEffect()));
        }
        return new TurnOrderTile(playersCount, slots);
    }

    private static OfferTile upperPickTile(char letter) {
        return new OfferTile(letter, 1, List.of(new ActionSlot(new UpperRowSlot(), 1)));
    }

    private static OfferTile lowerPickTile(char letter) {
        return new OfferTile(letter, 1, List.of(new ActionSlot(new LowerRowSlot(), 1)));
    }

    private static OfferTile noPickTile(char letter) {
        return new OfferTile(letter, 1, new ArrayList<>());
    }

    private static Totem createTotem(TotemColor color) {
        return new Totem(color, "OFFER_TRACK");
    }
}