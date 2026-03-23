package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.*;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.effect.TotemSlotFoodBonusEffect;
import it.polimi.ingsw.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RoundTest {

    private Game game;
    private Round round;
    private Player player1, player2;
    private OfferTile tileA, tileB;
    private Board board;

    @BeforeEach
    void setUp() {
        player1 = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player2 = new Player("Bob", new Totem(TotemColor.BLUE, TotemPosition.OFFER_TRACK));

        List<ActionSlot> slotsA = List.of(new ActionSlot(SlotType.UPPER_ROW, 2));
        tileA = new OfferTile('A', 2, slotsA);

        List<ActionSlot> slotsB = List.of(new ActionSlot(SlotType.FOOD, 3));
        tileB = new OfferTile('B', 2, slotsB);

        List<TurnSlot> turnSlots = new ArrayList<>();
        turnSlots.add(new TurnSlot(1, SlotEffect.NONE, 0, 0));
        turnSlots.add(new TurnSlot(2, SlotEffect.NONE, 0, 0));

        board = new Board(List.of(tileA, tileB), new TurnOrderTile(2, turnSlots));

        List<Card> tributeDeck = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            tributeDeck.add(new HunterCard(i, Era.I, 2, false));
        }

        game = new Game(
                List.of(player1, player2),
                1, null,
                Era.I, null,
                board, tributeDeck,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                GameStatus.IN_PROGRESS
        );

        round = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(List.of(
                        new HunterCard(7,  Era.I, 2, false),
                        new HunterCard(8,  Era.I, 2, false),
                        new HunterCard(9,  Era.I, 2, false)
                )),
                new ArrayList<>(List.of(
                        new HunterCard(10, Era.I, 2, false),
                        new HunterCard(11, Era.I, 2, false),
                        new HunterCard(12, Era.I, 2, false),
                        new HunterCard(13, Era.I, 2, false),
                        new HunterCard(14, Era.I, 2, false)
                )),
                new ArrayList<>(), new ArrayList<>()
        );

        game.setCurrentRound(round);
    }

    // -------------------------------------------------------
    // placeTotemOffer
    // -------------------------------------------------------

    @Test
    void testPlaceTotemOfferSucceeds() {
        assertTrue(round.placeTotemOffer(player1, tileA));
        assertTrue(tileA.isOccupied());
    }

    @Test
    void testPlaceTotemOfferFailsIfTileOccupied() {
        round.placeTotemOffer(player1, tileA);
        assertFalse(round.placeTotemOffer(player2, tileA));
    }

    @Test
    void testPlaceTotemOfferFailsIfNotEnoughPlayers() {
        OfferTile tileFor5 = new OfferTile('Z', 5, List.of());
        assertFalse(round.placeTotemOffer(player1, tileFor5));
    }

    // -------------------------------------------------------
    // execute — ACTION_RESOLUTION
    // -------------------------------------------------------

    @Test
    void testExecuteGivesUpperRowCardsToPlayer() {
        // chosenCards is empty → player gets 0 cards
        round.placeTotemOffer(player1, tileA);
        round.execute();
        assertEquals(0, player1.getTribe().getCharacterCards().size());
    }

    @Test
    void testExecuteGivesFoodToPlayer() {
        round.placeTotemOffer(player2, tileB);
        round.execute();
        assertEquals(3, player2.getFood());
    }

    // -------------------------------------------------------
    // execute — ROUND_CLEANUP
    // -------------------------------------------------------

    @Test
    void testDiscardLowerRowClearsIt() {
        round.discardLowerRow();
        assertEquals(0, round.getLowerTributeRowSize());
    }

    @Test
    void testExecuteShiftsUpperToLower() {
        round.placeTotemOffer(player1, tileA);
        round.execute();
        assertEquals(5, round.getLowerTributeRowSize());
    }

    // -------------------------------------------------------
    // checkEraTransition
    // -------------------------------------------------------

    @Test
    void testCheckEraTransitionAdvancesToEraII() {
        round.getUpperRowTribute().add(new HunterCard(99, Era.II, 2, false));
        round.checkEraTransition();
        assertEquals(Era.II, game.getCurrentEra());
    }

    @Test
    void testCheckEraTransitionDoesNotAdvanceIfNoEraIICard() {
        round.checkEraTransition();
        assertEquals(Era.I, game.getCurrentEra());
    }

    @Test
    void testExecuteWithNoPlayerChoicesDoesNotThrow() {
        assertDoesNotThrow(() -> round.execute());
    }

    @Test
    void testPlaceTotemOfferSamePlayerTwiceOverwritesChoice() {
        round.placeTotemOffer(player1, tileA);
        tileA.removeTotem();
        round.placeTotemOffer(player1, tileB);
        round.execute();

        assertEquals(3, player1.getFood());
        assertEquals(0, player1.getTribe().getCharacterCards().size());
    }

    @Test
    void testResolveOfferActionWithEmptyUpperRowDoesNotThrow() {
        Round emptyRound = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        game.setCurrentRound(emptyRound);

        emptyRound.resolveOfferAction(player1, tileA, new ArrayList<>());
        assertEquals(0, player1.getTribe().getCharacterCards().size());
    }

    @Test
    void testCreateNextRoundHasIndependentLists() {
        Round round2 = round.createNextRound(2);

        round.getUpperRowTribute().clear();
        round.getLowerRowTribute().clear();

        assertNotSame(round.getUpperRowTribute(), round2.getUpperRowTribute());
        assertNotSame(round.getLowerRowTribute(), round2.getLowerRowTribute());
    }

    @Test
    void testRefillUpperRowStopsWhenTributeDeckIsEmpty() {
        game.getTributeDeck().clear();
        assertDoesNotThrow(() -> round.refillUpperRow());
    }

    @Test
    void testCheckEraTransitionInEraIIIDoesNothing() {
        game.nextEra();
        game.nextEra(); // now Era III

        Round eraIIIRound = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.III,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        game.setCurrentRound(eraIIIRound);

        assertDoesNotThrow(() -> eraIIIRound.checkEraTransition());
        assertEquals(Era.III, game.getCurrentEra());
    }

    @Test
    void testShiftUpperToLowerRowClearsPreviousLowerRow() {
        int upperSizeBefore = round.getUpperTributeRowSize();

        round.shiftUpperToLowerRow();

        assertEquals(upperSizeBefore, round.getLowerTributeRowSize());
        assertEquals(0, round.getUpperTributeRowSize());
    }

    // -------------------------------------------------------
    // placeTotemOrderForPlayer — slot effects
    // -------------------------------------------------------

    @Test
    void testPlaceTotemOrderNoneEffectDoesNotChangeFood() {
        round.placeTotemOrderForPlayer(player1);
        round.placeTotemOrderForPlayer(player2);

        assertEquals(0, player1.getFood());
        assertEquals(0, player2.getFood());
    }

    @Test
    void testPlaceTotemOrderGainFoodAppliedToFirstPlayer() {
        List<TurnSlot> effectSlots = new ArrayList<>();
        effectSlots.add(new TurnSlot(1, SlotEffect.GAIN_FOOD, 2, 0));
        effectSlots.add(new TurnSlot(2, SlotEffect.NONE, 0, 0));
        board.setTurnOrderTile(new TurnOrderTile(2, effectSlots));

        round.placeTotemOrderForPlayer(player1); // slot 1 → GAIN_FOOD 2

        assertEquals(2, player1.getFood());
    }

    @Test
    void testPlaceTotemOrderPayFoodAppliedToLastPlayer() {
        List<TurnSlot> effectSlots = new ArrayList<>();
        effectSlots.add(new TurnSlot(1, SlotEffect.NONE, 0, 0));
        effectSlots.add(new TurnSlot(2, SlotEffect.PAY_FOOD, 1, 0));
        board.setTurnOrderTile(new TurnOrderTile(2, effectSlots));

        player2.addFood(5);

        round.placeTotemOrderForPlayer(player1); // slot 1 → NONE
        round.placeTotemOrderForPlayer(player2); // slot 2 → PAY_FOOD 1

        assertEquals(4, player2.getFood()); // 5 - 1
    }

    @Test
    void testPlaceTotemOrderBothEffectsAppliedCorrectly() {
        List<TurnSlot> effectSlots = new ArrayList<>();
        effectSlots.add(new TurnSlot(1, SlotEffect.GAIN_FOOD, 3, 0));
        effectSlots.add(new TurnSlot(2, SlotEffect.PAY_FOOD, 2, 0));
        board.setTurnOrderTile(new TurnOrderTile(2, effectSlots));

        player2.addFood(5);

        round.placeTotemOrderForPlayer(player1); // slot 1 → GAIN_FOOD 3
        round.placeTotemOrderForPlayer(player2); // slot 2 → PAY_FOOD 2

        assertEquals(3, player1.getFood()); // 0 + 3
        assertEquals(3, player2.getFood()); // 5 - 2
    }

    @Test
    void testPlaceTotemOrderWithNoOccupiedSlotsDoesNotThrow() {
        assertDoesNotThrow(() -> round.placeTotemOrderForPlayer(player1));
        assertEquals(0, player1.getFood()); // slot 1 NONE → no change
    }

    // -------------------------------------------------------
    // resolveOfferAction — building purchase
    // -------------------------------------------------------

    @Test
    void testResolveOfferActionPurchasesBuildingWhenAffordable() {
        player1.addFood(5);

        BuildingCard building = new BuildingCard(2, Era.I, 2, p -> {}, 0);
        OfferTile buildingTile = new OfferTile('C', 2,
                List.of(new ActionSlot(SlotType.UPPER_ROW, 1)));

        Round buildingRound = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(List.of(building))
        );

        buildingRound.resolveOfferAction(player1, buildingTile, List.of(building));

        assertEquals(1, player1.getTribe().getBuildingCards().size());
        assertEquals(3, player1.getFood()); // 5 - 2
    }

    @Test
    void testResolveOfferActionDoesNotPurchaseBuildingWhenNotAffordable() {
        BuildingCard building = new BuildingCard(3, Era.I, 2, p -> {}, 0);
        OfferTile buildingTile = new OfferTile('C', 2,
                List.of(new ActionSlot(SlotType.UPPER_ROW, 1)));

        Round buildingRound = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(List.of(building))
        );

        buildingRound.resolveOfferAction(player1, buildingTile, List.of(building));

        assertEquals(0, player1.getTribe().getBuildingCards().size());
        assertEquals(0, player1.getFood());
    }

    @Test
    void testPlaceTotemOrderForPlayerWithTotemSlotFoodBonusEffect() {
        List<TurnSlot> effectSlots = new ArrayList<>();
        effectSlots.add(new TurnSlot(1, SlotEffect.GAIN_FOOD, 2, 0));
        effectSlots.add(new TurnSlot(2, SlotEffect.NONE, 0, 0));
        board.setTurnOrderTile(new TurnOrderTile(2, effectSlots));

        player1.addBuildingCard(new BuildingCard(1, Era.I, 0, new TotemSlotFoodBonusEffect(), 0));

        round.placeTotemOrderForPlayer(player1);

        assertEquals(3, player1.getFood()); // 2 dal slot + 1 dal building
    }

    @Test
    void testPlaceTotemOrderForPlayerWithTotemSlotFoodBonusEffectOnNoneSlot() {
        // TotemSlotFoodBonusEffect non si attiva su NONE
        player1.addBuildingCard(new BuildingCard(1, Era.I, 0, new TotemSlotFoodBonusEffect(), 0));

        round.placeTotemOrderForPlayer(player1); // slot 1 → NONE

        assertEquals(0, player1.getFood()); // nessun bonus
    }

    @Test
    void testExecuteResetsOfferTilesAfterRound() {
        round.placeTotemOffer(player1, tileA);
        round.placeTotemOffer(player2, tileB);

        round.execute();

        // dopo execute() le tile devono essere libere per il round successivo
        assertFalse(tileA.isOccupied());
        assertFalse(tileB.isOccupied());
    }
}
