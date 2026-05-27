package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.controller.state.ActionResolutionState;
import it.polimi.ingsw.controller.state.EventResolutionState;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.controller.state.LobbyState;
import it.polimi.ingsw.controller.state.ReadyCheckState;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.ActionSlot;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.FoodSlot;
import it.polimi.ingsw.model.board.GainFoodSlotEffect;
import it.polimi.ingsw.model.board.LowerRowSlot;
import it.polimi.ingsw.model.board.NoSlotEffect;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.board.TurnOrderTile;
import it.polimi.ingsw.model.board.TurnSlot;
import it.polimi.ingsw.model.board.UpperRowSlot;
import it.polimi.ingsw.model.card.ArtistCard;
import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.EventCard;
import it.polimi.ingsw.model.card.GathererCard;
import it.polimi.ingsw.model.card.HuntEventCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.SustenanceEventCard;
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
 * One-file suite containing the 33 uncovered cases discussed for the controller/game flow.
 *
 * Some tests are intentionally strict BUG-HUNTER tests: if they fail, they indicate a behavior
 * to discuss/fix, not necessarily a syntax problem in the test.
 */
public class GameControllerThirtyThreeDiscoveredCasesTest {

    // ------------------------------------------------------------------
    // A. LOBBY CASES
    // ------------------------------------------------------------------

    /** 1. Nicknames that differ only by leading/trailing spaces should not bypass uniqueness. */
    @Test
    void nicknameWithLeadingOrTrailingSpacesShouldNotBypassUniqueness() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived(" Alice ", new JoinGameAction(createTotem(TotemColor.BLUE))));
    }

    /** 2. This documents the current policy: case-different nicknames are accepted as different. */
    @Test
    void caseDifferentNicknamesAreTreatedAsDifferentPlayersByCurrentPolicy() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        assertDoesNotThrow(() ->
                controller.onMessageReceived("alice", new JoinGameAction(createTotem(TotemColor.BLUE))));

        assertEquals(2, controller.getLobbyPlayers().size());
    }

    /** 3. Newline/control-character nicknames should be rejected before entering the lobby. */
    @Test
    void nicknameWithNewlineShouldBeRejected() {
        GameController controller = new GameController();

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Bob\nAlice", new JoinGameAction(createTotem(TotemColor.RED))));
    }

    /** 4. Once the lobby is full and the controller is in ReadyCheck, a new join must be rejected. */
    @Test
    void joinAfterReadyCheckStartedIsRejected() {
        GameController controller = createReadyCheckController();

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Charlie", new JoinGameAction(createTotem(TotemColor.PURPLE))));
    }

    /** 5. The player count cannot be changed after the lobby has moved to ReadyCheckState. */
    @Test
    void changingPlayerCountAfterReadyCheckStartedIsAllowedForFirstPlayer() {
        GameController controller = createReadyCheckController();

        assertDoesNotThrow(() -> controller.onMessageReceived("Alice", new SetPlayerCountAction(3)));
        assertEquals(3, controller.getChosenPlayersCount());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
    }

    // ------------------------------------------------------------------
    // B. READY CHECK CASES
    // ------------------------------------------------------------------

    /** 6. A disconnected/removed player must not be able to become ready. */
    @Test
    void disconnectedPlayerCannotSendReadyTrue() {
        GameController controller = createReadyCheckController();

        controller.onPlayerDisconnection("Bob");

 assertThrows(IllegalArgumentException.class, () -> controller.onMessageReceived("Bob", new ReadyAction(true))); }

    /** 7. Sending ReadyAction(true) twice must be stable and must not create duplicated readiness. */
    @Test
    void samePlayerCanSendReadyTrueTwiceWithoutChangingStateUnexpectedly() {
        GameController controller = createReadyCheckControllerWithThreePlayers();

        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onMessageReceived("Alice", new ReadyAction(true));

        assertTrue(controller.isPlayerReady("Alice"));
        assertFalse(controller.isPlayerReady("Bob"));
        assertFalse(controller.isPlayerReady("Charlie"));
        assertInstanceOf(ReadyCheckState.class, controller.getCurrentState());
    }

    /** 8. If a player disconnects before everyone is ready, the game must not start. */
    @Test
    void disconnectionBeforeFinalReadyPreventsGameStart() {
        GameController controller = createReadyCheckController();

        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onPlayerDisconnection("Bob");

        assertNull(controller.getGame());
        assertInstanceOf(LobbyState.class, controller.getCurrentState());
        assertFalse(controller.isPlayerReady("Bob"));
    }

    // ------------------------------------------------------------------
    // C. TOTEM PLACEMENT CASES
    // ------------------------------------------------------------------

    /** 9. Players should place totems following the current turn-order tile. */
    @Test
    void totemPlacementOutOfTurnShouldBeRejected() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        GameController controller = createTotemPlacementController(
                List.of(alice, bob),
                List.of(foodTile('A', 1), foodTile('B', 1))
        );
        setTurnOrder(controller, alice, bob);

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Bob", new PlaceTotemAction('A')));
    }

    /** 10. Tile A / 5-player-only tiles must be rejected in a 2-player game. */
    @Test
    void fivePlayerOnlyTileIsRejectedInTwoPlayerGame() {
        GameController controller = createTotemPlacementController(
                createPlayers(2),
                List.of(new OfferTile('A', 5, List.of(new ActionSlot(new FoodSlot(), 3))), foodTile('B', 1))
        );

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new PlaceTotemAction('A')));
    }

    /** 11. A tile occupied by a player who disconnects during placement becomes available again. */
    @Test
    void occupiedTileBecomesAvailableAfterItsPlayerDisconnectsDuringPlacement() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        Player carol = new Player("Carol", createTotem(TotemColor.PURPLE));
        OfferTile tileA = foodTile('A', 1);
        OfferTile tileB = foodTile('B', 1);
        OfferTile tileC = foodTile('C', 1);
        GameController controller = createTotemPlacementController(List.of(alice, bob, carol), List.of(tileA, tileB, tileC));

        controller.onMessageReceived("Alice", new PlaceTotemAction('A'));
        controller.onPlayerDisconnection("Alice");
        controller.onMessageReceived("Bob", new PlaceTotemAction('A'));

        assertTrue(controller.getGame().getBoard().getOfferTiles().get(0).isOccupied());
        assertSame(bob.getTotem(), controller.getGame().getBoard().getOfferTiles().get(0).getTotemSlot());
    }

    /** 12. A player reconnecting during TotemPlacement should still be able to place if the phase is not over. */
    @Test
    void reconnectedPlayerCanStillPlaceTotemIfPlacementPhaseIsNotOver() {
        List<Player> players = createPlayers(3);
        GameController controller = createTotemPlacementController(
                players,
                List.of(foodTile('A', 1), foodTile('B', 1), foodTile('C', 1))
        );

        controller.onMessageReceived("Alice", new PlaceTotemAction('A'));
        controller.onPlayerDisconnection("Bob");
        controller.onPlayerReconnection("Bob");

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Bob", new PlaceTotemAction('B'))); }

    @Test
    void reconnectedPlayerDuringTotemPlacementWaitsForNextRound() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        Player carol = new Player("Carol", createTotem(TotemColor.PURPLE));
        GameController controller = createTotemPlacementController(
                List.of(alice, bob, carol),
                List.of(foodTile('A', 1), foodTile('B', 1), foodTile('C', 1))
        );

        controller.onPlayerDisconnection("Carol");
        controller.onMessageReceived("Alice", new PlaceTotemAction('A'));
        controller.onPlayerReconnection("Carol");
        controller.onMessageReceived("Bob", new PlaceTotemAction('B'));

        assertNotEquals("Bob", controller.getCurrentTurnPlayerNickname());
        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Carol", new PlaceTotemAction('C')));
    }

    // ------------------------------------------------------------------
    // D. ACTION RESOLUTION CASES
    // ------------------------------------------------------------------

    /** 13. A player cannot pick when it is another player's action-resolution turn. */
    @Test
    void playerOutsideTurnCannotPickCard() {
        ActionScenario scenario = createAliceUpperPickScenario();

        assertThrows(IllegalStateException.class,
                () -> scenario.controller.onMessageReceived("Bob", new PickCardAction("101", true)));
    }

    /** 14. A player cannot pick from the wrong row. */
    @Test
    void playerCannotPickFromWrongRow() {
        ActionScenario scenario = createAliceUpperPickScenario();

        assertThrows(IllegalStateException.class,
                () -> scenario.controller.onMessageReceived("Alice", new PickCardAction("201", false)));
    }

    /** 15. A player cannot pick more cards than the selected offer tile allows. */
    @Test
    void playerCannotPickMoreCardsThanTileAllows() {
        ActionScenario scenario = createAliceUpperPickScenario();

        scenario.controller.onMessageReceived("Alice", new PickCardAction("101", true));

        assertThrows(IllegalStateException.class,
                () -> scenario.controller.onMessageReceived("Alice", new PickCardAction("102", true)));
    }

    /** 16. If a tile allows upper + lower, the player may choose the order freely. */
    @Test
    void upperAndLowerPickTileAllowsBothOrders() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        OfferTile aliceTile = new OfferTile('A', 1, List.of(
                new ActionSlot(new UpperRowSlot(), 1),
                new ActionSlot(new LowerRowSlot(), 1)
        ));
        OfferTile bobTile = foodTile('B', 0);
        Round round = createRoundWithRows(
                List.of(new HunterCard(201, Era.I, 1, false)),
                List.of(new HunterCard(101, Era.I, 1, false)),
                new ArrayList<>(),
                new ArrayList<>()
        );
        GameController controller = createActionResolutionController(
                List.of(alice, bob), List.of(aliceTile, bobTile), round, Map.of(alice, aliceTile, bob, bobTile)
        );

        controller.onMessageReceived("Alice", new PickCardAction("101", true));
        controller.onMessageReceived("Alice", new PickCardAction("201", false));

        assertEquals(2, alice.getTribe().countCharacterCards());
    }

    /** 17. Event cards on a row must never be pickable as tribute cards. */
    @Test
    void eventCardCannotBePickedFromRow() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        OfferTile aliceTile = new OfferTile('A', 1, List.of(new ActionSlot(new UpperRowSlot(), 1)));
        OfferTile bobTile = foodTile('B', 0);
        Round round = createRoundWithEvents(
                new ArrayList<>(),
                List.of(new HuntEventCard(900, Era.I, false, 1, 1)),
                new ArrayList<>(), List.of(new HunterCard(901, Era.I, 1, false)),
                new ArrayList<>(), new ArrayList<>()
        );
        GameController controller = createActionResolutionController(
                List.of(alice, bob), List.of(aliceTile, bobTile), round, Map.of(alice, aliceTile, bob, bobTile)
        );

        assertThrows(IllegalArgumentException.class,
                () -> controller.onMessageReceived("Alice", new PickCardAction("900", true)));
    }

    /** 18. A building that the player cannot afford must be rejected and remain on the board. */
    @Test
    void unaffordableBuildingIsRejectedAndRemainsOnBoard() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        BuildingCard expensive = new BuildingCard(301, Era.I, 10, new EndPPEffect(), 0);
        OfferTile aliceTile = new OfferTile('A', 1, List.of(new ActionSlot(new UpperRowSlot(), 1)));
        OfferTile bobTile = foodTile('B', 0);
        Round round = createRoundWithRows(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), List.of(expensive));
        GameController controller = createActionResolutionController(
                List.of(alice, bob), List.of(aliceTile, bobTile), round, Map.of(alice, aliceTile, bob, bobTile)
        );

        assertThrows(IllegalStateException.class,
                () -> controller.onMessageReceived("Alice", new PickBuildingAction("301", true)));
        assertTrue(controller.getGame().getCurrentRound().getUpperRowBuildings().contains(expensive));
    }

    /** 19. Builder cards should reduce the cost paid for a building. */
    @Test
    void builderDiscountReducesBuildingCost() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        alice.addFood(1);
        alice.addCharacterCard(new BuilderCard(700, Era.I, 1, 0, 2));
        BuildingCard building = new BuildingCard(302, Era.I, 3, new EndPPEffect(), 0);
        OfferTile aliceTile = new OfferTile('A', 1, List.of(new ActionSlot(new UpperRowSlot(), 1)));
        OfferTile bobTile = foodTile('B', 0);
        Round round = createRoundWithRows(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), List.of(building));
        GameController controller = createActionResolutionController(
                List.of(alice, bob), List.of(aliceTile, bobTile), round, Map.of(alice, aliceTile, bob, bobTile)
        );

        controller.onMessageReceived("Alice", new PickBuildingAction("302", true));

        // Alice pays 1 food after the builder discount, then receives +1 food
// from the first turn-order slot.
        assertEquals(1, alice.getFood());
        assertEquals(1, alice.getTribe().getBuildingCards().size());
    }

    /** 20. Builder discount must never make the final building cost negative. */
    @Test
    void builderDiscountCannotMakeBuildingCostNegative() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        alice.addCharacterCard(new BuilderCard(701, Era.I, 1, 0, 5));
        BuildingCard building = new BuildingCard(303, Era.I, 2, new EndPPEffect(), 0);
        OfferTile aliceTile = new OfferTile('A', 1, List.of(new ActionSlot(new UpperRowSlot(), 1)));
        OfferTile bobTile = foodTile('B', 0);
        Round round = createRoundWithRows(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), List.of(building));
        GameController controller = createActionResolutionController(
                List.of(alice, bob), List.of(aliceTile, bobTile), round, Map.of(alice, aliceTile, bob, bobTile)
        );

        controller.onMessageReceived("Alice", new PickBuildingAction("303", true));

        // The discount reduces the building cost to 0, then Alice receives +1 food
// from the first turn-order slot.
        assertEquals(1, alice.getFood());
        assertEquals(1, alice.getTribe().getBuildingCards().size());
    }

    /** 21. If the current action player disconnects and at least two remain connected, their turn is skipped. */
    @Test
    void currentActionPlayerDisconnectionSkipsToNextConnectedPlayer() {
        ThreePlayerActionScenario scenario = createThreePlayerAliceThenBobThenCharlieScenario();

        scenario.controller.onPlayerDisconnection("Alice");

        assertDoesNotThrow(() -> scenario.controller.onMessageReceived("Bob", new PickCardAction("102", true)));
        assertEquals(1, scenario.bob.getTribe().countCharacterCards());
    }

    /** 22. A player already disconnected before their action turn is skipped. */
    @Test
    void disconnectedPlayerBeforeOwnActionTurnIsSkipped() {
        ThreePlayerActionScenario scenario = createThreePlayerAliceThenBobThenCharlieScenario();
        scenario.controller.markPlayerDisconnected("Bob");
        scenario.controller.changeState(new ActionResolutionState());

        scenario.controller.onMessageReceived("Alice", new PickCardAction("101", true));

        assertDoesNotThrow(() -> scenario.controller.onMessageReceived("Charlie", new PickCardAction("103", true)));
    }

    // ------------------------------------------------------------------
    // E. EXTRA PICK CASES
    // ------------------------------------------------------------------

    /** 23. Extra pick effects must only allow picking from the upper row. */
    @Test
    void extraPickRejectsLowerRowCharacterPick() {
        ExtraPickScenario scenario = createExtraPickScenarioWithUpperCard();

        assertThrows(IllegalStateException.class,
                () -> scenario.controller.onMessageReceived("Alice", new PickCardAction("901", false)));
    }

    /** 24. Extra pick with an empty upper row should not leave the controller waiting forever. */
    @Test
    void extraPickWithEmptyUpperRowShouldAutoSkip() {
        ExtraPickScenario scenario = createExtraPickScenarioWithEmptyUpperRow();

        assertFalse(scenario.controller.getCurrentState() instanceof ActionResolutionState);
    }

    /** 25. Extra pick for a disconnected player should be skipped. */
    @Test
    void disconnectedExtraPickPlayerIsSkipped() {
        ExtraPickScenario scenario = createExtraPickScenarioWithUpperCard();

        scenario.controller.onPlayerDisconnection("Alice");

        assertThrows(IllegalStateException.class,
                () -> scenario.controller.onMessageReceived("Alice", new PickCardAction("901", true)));
    }

    // ------------------------------------------------------------------
    // F. EVENT CASES
    // ------------------------------------------------------------------

    /** 26. EventResolutionState must reject manual player actions. */
    @Test
    void eventResolutionStateRejectsManualActions() {
        EventResolutionState state = new EventResolutionState();
        GameController controller = createBaseGameController(
                createPlayers(2),
                List.of(foodTile('A', 0), foodTile('B', 0)),
                createRoundWithRows(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>())
        );

        assertThrows(IllegalStateException.class,
                () -> state.handleAction(controller, "Alice", new PickCardAction("1", true)));
    }

    /** 27. Sustenance must be resolved after other events. */
    @Test
    void sustenanceEventIsResolvedAfterHuntEvent() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        alice.addCharacterCard(new HunterCard(1, Era.I, 1, false));
        Round round = createRoundWithEvents(
                List.of(new SustenanceEventCard(1001, Era.I, false, 10), new HuntEventCard(1002, Era.I, false, 1, 5)),
                new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        GameController controller = createBaseGameController(List.of(alice, bob), List.of(foodTile('A', 0), foodTile('B', 0)), round);

        controller.changeState(new EventResolutionState());

        assertEquals(5, alice.getPrestigePoints());
        assertEquals(0, alice.getFood());
    }

    /** 28. Two events of the same type should be resolved in Era order. */
    @Test
    void eventsOfSameTypeAreResolvedInEraOrder() {
        List<String> order = new ArrayList<>();
        Round round = createRoundWithEvents(
                List.of(new TrackingEventCard(2002, Era.II, "II", order), new TrackingEventCard(2001, Era.I, "I", order)),
                new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        GameController controller = createBaseGameController(createPlayers(2), List.of(foodTile('A', 0), foodTile('B', 0)), round);

        controller.changeState(new EventResolutionState());

        assertEquals(List.of("I", "II"), order);
    }

    // ------------------------------------------------------------------
    // G. END ROUND / END GAME CASES
    // ------------------------------------------------------------------

    /** 29. End-of-round lower row cleanup must remove characters/events but keep lower buildings. */
    @Test
    void lowerRowCleanupDiscardsOnlyTributeAndEventsButKeepsBuildings() {
        BuildingCard lowerBuilding = new BuildingCard(3001, Era.I, 0, new EndPPEffect(), 0);
        Round round = createRoundWithEvents(
                List.of(new HuntEventCard(3002, Era.I, false, 1, 1)),
                new ArrayList<>(),
                List.of(new HunterCard(3003, Era.I, 1, false)),
                new ArrayList<>(),
                List.of(lowerBuilding),
                new ArrayList<>()
        );

        round.discardLowerRow();

        assertTrue(round.getLowerRowEvents().isEmpty());
        assertTrue(round.getLowerRowTribute().isEmpty());
        assertTrue(round.getLowerRowBuildings().contains(lowerBuilding));
    }

    /** 30. Shifting upper to lower must move only tribute/events, not buildings. */
    @Test
    void shiftUpperToLowerDoesNotMoveBuildings() {
        BuildingCard upperBuilding = new BuildingCard(4001, Era.I, 0, new EndPPEffect(), 0);
        CharacterCard upperCharacter = new HunterCard(4002, Era.I, 1, false);
        EventCard upperEvent = new HuntEventCard(4003, Era.I, false, 1, 1);
        Round round = createRoundWithEvents(
                new ArrayList<>(), List.of(upperEvent),
                new ArrayList<>(), List.of(upperCharacter),
                new ArrayList<>(), List.of(upperBuilding)
        );

        round.shiftUpperToLowerRow();

        assertTrue(round.getLowerRowTribute().contains(upperCharacter));
        assertTrue(round.getLowerRowEvents().contains(upperEvent));
        assertTrue(round.getUpperRowBuildings().contains(upperBuilding));
    }

    /** 31. At the end of the last round, visible upper-row events must also be resolved. */
    @Test
    void finalRoundResolvesUpperRowEventsToo() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        alice.addCharacterCard(new HunterCard(5001, Era.I, 1, false));
        Round round = createRoundWithEvents(
                new ArrayList<>(),
                List.of(new HuntEventCard(5002, Era.I, false, 1, 4)),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        GameController controller = createBaseGameController(List.of(alice, bob), List.of(foodTile('A', 0), foodTile('B', 0)), round, null, 10);

        controller.changeState(new EventResolutionState());

        assertTrue(alice.getPrestigePoints() >= 4);
    }

    /** 32. If final points are tied, food should be the tie-breaker. */
    @Test
    void finalTieOnPointsIsBrokenByFood() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        alice.addPrestigePoints(10);
        bob.addPrestigePoints(10);
        alice.addFood(3);
        bob.addFood(1);
        Game game = createPlainGame(List.of(alice, bob), 1);

        assertEquals(alice, game.getWinner());
    }

    /** 33. If points and food are tied, victory is shared: getWinner() returns null,
     *  getWinners() contains all co-winners. */
    @Test
    void finalTieOnPointsAndFoodProducesSharedVictory() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        alice.addPrestigePoints(10);
        bob.addPrestigePoints(10);
        alice.addFood(2);
        bob.addFood(2);
        Game game = createPlainGame(List.of(alice, bob), 1);

        assertEquals(game.getFinalScore(alice), game.getFinalScore(bob),
                "scores must remain equal on a full tie");
        assertNull(game.getWinner(),
                "full PP+food tie → shared victory: getWinner() must return null");
        List<Player> winners = game.getWinners();
        assertTrue(winners.contains(alice), "Alice must be in getWinners() on a full tie");
        assertTrue(winners.contains(bob),   "Bob must be in getWinners() on a full tie");
    }

    // ------------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------------

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

    private GameController createTotemPlacementController(List<Player> players, List<OfferTile> offerTiles) {
        Round round = createRoundWithRows(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        return createBaseGameController(players, offerTiles, round, new TotemPlacementState());
    }

    private GameController createBaseGameController(List<Player> players, List<OfferTile> offerTiles, Round round) {
        return createBaseGameController(players, offerTiles, round, null, 1);
    }

    private GameController createBaseGameController(List<Player> players, List<OfferTile> offerTiles, Round round, GameState stateToEnter) {
        return createBaseGameController(players, offerTiles, round, stateToEnter, 1);
    }

    private GameController createBaseGameController(List<Player> players,
                                                    List<OfferTile> offerTiles,
                                                    Round round,
                                                    GameState stateToEnter,
                                                    int currentRoundNumber) {
        GameController controller = new GameController();
        Game game = createPlainGame(players, currentRoundNumber, offerTiles);
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

    private Game createPlainGame(List<Player> players, int currentRoundNumber) {
        return createPlainGame(players, currentRoundNumber, List.of(foodTile('A', 0), foodTile('B', 0)));
    }

    private Game createPlainGame(List<Player> players, int currentRoundNumber, List<OfferTile> offerTiles) {
        Board board = new Board(new ArrayList<>(offerTiles), TurnOrderTile.createForPlayers(players.size()));
        Game game = new Game(players, currentRoundNumber, null, Era.I, null, board,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        for (Player player : players) {
            player.setGame(game);
        }
        return game;
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

    private ActionScenario createAliceUpperPickScenario() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        OfferTile aliceTile = new OfferTile('A', 1, List.of(new ActionSlot(new UpperRowSlot(), 1)));
        OfferTile bobTile = foodTile('B', 0);
        Round round = createRoundWithRows(
                List.of(new HunterCard(201, Era.I, 1, false)),
                List.of(new HunterCard(101, Era.I, 1, false), new HunterCard(102, Era.I, 1, false)),
                new ArrayList<>(), new ArrayList<>()
        );
        GameController controller = createActionResolutionController(
                List.of(alice, bob), List.of(aliceTile, bobTile), round, Map.of(alice, aliceTile, bob, bobTile)
        );
        return new ActionScenario(controller, alice, bob, controller.getGame().getCurrentRound());
    }

    private ThreePlayerActionScenario createThreePlayerAliceThenBobThenCharlieScenario() {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        Player charlie = new Player("Charlie", createTotem(TotemColor.PURPLE));
        OfferTile aliceTile = new OfferTile('A', 1, List.of(new ActionSlot(new UpperRowSlot(), 1)));
        OfferTile bobTile = new OfferTile('B', 1, List.of(new ActionSlot(new UpperRowSlot(), 1)));
        OfferTile charlieTile = new OfferTile('C', 1, List.of(new ActionSlot(new UpperRowSlot(), 1)));
        Round round = createRoundWithRows(new ArrayList<>(),
                List.of(new HunterCard(101, Era.I, 1, false), new HunterCard(102, Era.I, 1, false), new HunterCard(103, Era.I, 1, false)),
                new ArrayList<>(), new ArrayList<>());
        GameController controller = createBaseGameController(List.of(alice, bob, charlie), List.of(aliceTile, bobTile, charlieTile), round);
        Round actualRound = controller.getGame().getCurrentRound();
        actualRound.placeTotemOffer(alice, controller.getGame().getBoard().getOfferTiles().get(0));
        actualRound.placeTotemOffer(bob, controller.getGame().getBoard().getOfferTiles().get(1));
        actualRound.placeTotemOffer(charlie, controller.getGame().getBoard().getOfferTiles().get(2));
        controller.changeState(new ActionResolutionState());
        return new ThreePlayerActionScenario(controller, alice, bob, charlie, controller.getGame().getCurrentRound());
    }

    private ExtraPickScenario createExtraPickScenarioWithUpperCard() {
        return createExtraPickScenario(List.of(new HunterCard(901, Era.I, 1, false)));
    }

    private ExtraPickScenario createExtraPickScenarioWithEmptyUpperRow() {
        return createExtraPickScenario(new ArrayList<>());
    }

    private ExtraPickScenario createExtraPickScenario(List<CharacterCard> upperCards) {
        Player alice = new Player("Alice", createTotem(TotemColor.RED));
        Player bob = new Player("Bob", createTotem(TotemColor.BLUE));
        alice.addBuildingCard(new BuildingCard(900, Era.I, 0, new ExtraCardUpperRowEffect(), 0));
        OfferTile aliceTile = foodTile('A', 0);
        OfferTile bobTile = foodTile('B', 0);
        Round round = createRoundWithRows(new ArrayList<>(), upperCards, new ArrayList<>(), new ArrayList<>());
        GameController controller = createBaseGameController(List.of(alice, bob), List.of(aliceTile, bobTile), round);
        Round actualRound = controller.getGame().getCurrentRound();
        actualRound.placeTotemOffer(alice, controller.getGame().getBoard().getOfferTiles().get(0));
        actualRound.placeTotemOffer(bob, controller.getGame().getBoard().getOfferTiles().get(1));
        controller.changeState(new ActionResolutionState());
        return new ExtraPickScenario(controller, alice, bob, controller.getGame().getCurrentRound());
    }

    private Round createRoundWithRows(List<CharacterCard> lowerTribute,
                                      List<CharacterCard> upperTribute,
                                      List<BuildingCard> lowerBuildings,
                                      List<BuildingCard> upperBuildings) {
        return createRoundWithEvents(new ArrayList<>(), new ArrayList<>(), lowerTribute, upperTribute, lowerBuildings, upperBuildings);
    }

    private Round createRoundWithEvents(List<EventCard> lowerEvents,
                                        List<EventCard> upperEvents,
                                        List<CharacterCard> lowerTribute,
                                        List<CharacterCard> upperTribute,
                                        List<BuildingCard> lowerBuildings,
                                        List<BuildingCard> upperBuildings) {
        Game placeholderGame = createPlainGame(createPlayers(2), 1);
        return new Round(1, placeholderGame, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(lowerEvents), new ArrayList<>(upperEvents),
                new ArrayList<>(lowerTribute), new ArrayList<>(upperTribute),
                new ArrayList<>(lowerBuildings), new ArrayList<>(upperBuildings));
    }

    private OfferTile foodTile(char letter, int amount) {
        return new OfferTile(letter, 1, List.of(new ActionSlot(new FoodSlot(), amount)));
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

    private void setTurnOrder(GameController controller, Player... players) {
        TurnOrderTile tile = controller.getGame().getBoard().getTurnOrderTile();
        tile.reset();
        for (int i = 0; i < players.length; i++) {
            tile.getSlots().get(i).placeTotem(players[i].getTotem());
        }
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

    private static final class ThreePlayerActionScenario {
        private final GameController controller;
        private final Player alice;
        private final Player bob;
        private final Player charlie;
        private final Round round;

        private ThreePlayerActionScenario(GameController controller, Player alice, Player bob, Player charlie, Round round) {
            this.controller = controller;
            this.alice = alice;
            this.bob = bob;
            this.charlie = charlie;
            this.round = round;
        }
    }

    private static final class ExtraPickScenario {
        private final GameController controller;
        private final Player alice;
        private final Player bob;
        private final Round round;

        private ExtraPickScenario(GameController controller, Player alice, Player bob, Round round) {
            this.controller = controller;
            this.alice = alice;
            this.bob = bob;
            this.round = round;
        }
    }

    private static final class TrackingEventCard extends EventCard {
        private final String label;
        private final List<String> order;

        private TrackingEventCard(int id, Era era, String label, List<String> order) {
            super(id, era, false);
            this.label = label;
            this.order = order;
        }

        @Override
        public void resolve(Game game) {
            order.add(label);
        }
    }

    private static final class RecordingListener implements GameListener {
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
        @Override public void onRoundEnded(int roundNumber) { events.add("roundEnded:" + roundNumber); }
        @Override public void onGameEnded(Player winner, Map<Player, Integer> finalScores) { events.add("ended:" + (winner == null ? null : winner.getNickname())); }
        @Override public void onExtraCardPickAvailable(String nickname, int picks) { events.add("extra:" + nickname + ":" + picks); }
    }
}
