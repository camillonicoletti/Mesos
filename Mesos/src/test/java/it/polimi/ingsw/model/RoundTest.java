package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.*;
import it.polimi.ingsw.model.card.*;
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
        player1 = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player2 = new Player("Bob", new Totem(TotemColor.BLUE, "OFFER_TRACK"));

        List<ActionSlot> slotsA = List.of(new ActionSlot("UPPER_ROW", 2));
        tileA = new OfferTile('A', 2, slotsA);

        List<ActionSlot> slotsB = List.of(new ActionSlot("FOOD", 3));
        tileB = new OfferTile('B', 2, slotsB);

        List<TurnSlot> turnSlots = new ArrayList<>();
        turnSlots.add(new TurnSlot(1, new NoSlotEffect()));
        turnSlots.add(new TurnSlot(2, new NoSlotEffect()));

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
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
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
        round.placeTotemOffer(player1, tileA);
        round.resolveOfferAction(player1, tileA, new ArrayList<>());
        assertEquals(0, player1.getTribe().getCharacterCards().size());
    }
    @Test
    void testExecuteGivesFoodToPlayer() {
        round.placeTotemOffer(player2, tileB);
        round.resolveOfferAction(player2, tileB, new ArrayList<>());
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
        round.discardLowerRow();
        round.shiftUpperToLowerRow();
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
    void testResolveOfferActionWithNoChoicesDoesNotThrow() {
        assertDoesNotThrow(() -> round.resolveOfferAction(player1, tileA, new ArrayList<>()));
    }

    @Test
    void testPlaceTotemOfferSamePlayerTwiceOverwritesChoice() {
        round.placeTotemOffer(player1, tileA);
        tileA.removeTotem();
        round.placeTotemOffer(player1, tileB);
        round.resolveOfferAction(player1, tileB, new ArrayList<>());

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
        effectSlots.add(new TurnSlot(1, new GainFoodSlotEffect(2)));
        effectSlots.add(new TurnSlot(2, new NoSlotEffect()));
        board.setTurnOrderTile(new TurnOrderTile(2, effectSlots));

        round.placeTotemOrderForPlayer(player1); // slot 1 → GAIN_FOOD 2

        assertEquals(2, player1.getFood());
    }

    @Test
    void testPlaceTotemOrderPayFoodAppliedToLastPlayer() {
        List<TurnSlot> effectSlots = new ArrayList<>();
        effectSlots.add(new TurnSlot(1, new NoSlotEffect()));
        effectSlots.add(new TurnSlot(2, new PayFoodSlotEffect(1, 0)));
        board.setTurnOrderTile(new TurnOrderTile(2, effectSlots));

        player2.addFood(5);

        round.placeTotemOrderForPlayer(player1); // slot 1 → NONE
        round.placeTotemOrderForPlayer(player2); // slot 2 → PAY_FOOD 1

        assertEquals(4, player2.getFood()); // 5 - 1
    }

    @Test
    void testPlaceTotemOrderBothEffectsAppliedCorrectly() {
        List<TurnSlot> effectSlots = new ArrayList<>();
        effectSlots.add(new TurnSlot(1, new GainFoodSlotEffect(3)));
        effectSlots.add(new TurnSlot(2, new PayFoodSlotEffect(2, 0)));
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
                List.of(new ActionSlot("UPPER_ROW", 1)));

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
                List.of(new ActionSlot("UPPER_ROW", 1)));

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
        effectSlots.add(new TurnSlot(1, new GainFoodSlotEffect(2)));
        effectSlots.add(new TurnSlot(2, new NoSlotEffect()));
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



    // -------------------------------------------------------
// resolveOfferAction — LOWER_ROW
// -------------------------------------------------------

    @Test
    void testResolveOfferActionPicksCardFromLowerRow() {
        HunterCard card = new HunterCard(99, Era.I, 2, false);

        Round testRound = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(List.of(card)), // lowerRowTribute
                new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );

        OfferTile lowerTile = new OfferTile('D', 2,
                List.of(new ActionSlot("LOWER_ROW", 1)));

        testRound.resolveOfferAction(player1, lowerTile, List.of(card));

        assertEquals(1, player1.getTribe().getCharacterCards().size());
        assertEquals(0, testRound.getLowerRowTribute().size());
    }

// -------------------------------------------------------
// resolveOfferAction — più carte dalla riga superiore
// -------------------------------------------------------

    @Test
    void testResolveOfferActionPicksMultipleCardsFromUpperRow() {
        HunterCard card1 = new HunterCard(99,  Era.I, 2, false);
        HunterCard card2 = new HunterCard(100, Era.I, 2, false);

        Round testRound = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(List.of(card1, card2)), // upperRowTribute
                new ArrayList<>(), new ArrayList<>()
        );

        OfferTile multiTile = new OfferTile('E', 2,
                List.of(new ActionSlot("UPPER_ROW", 2)));

        testRound.resolveOfferAction(player1, multiTile, List.of(card1, card2));

        assertEquals(2, player1.getTribe().getCharacterCards().size());
        assertEquals(0, testRound.getUpperRowTribute().size());
    }

    @Test
    void testResolveOfferActionDoesNotExceedSlotLimit() {
        HunterCard card1 = new HunterCard(99,  Era.I, 2, false);
        HunterCard card2 = new HunterCard(100, Era.I, 2, false);
        HunterCard card3 = new HunterCard(101, Era.I, 2, false);

        Round testRound = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(List.of(card1, card2, card3)),
                new ArrayList<>(), new ArrayList<>()
        );

        // slot permette solo 1 carta
        OfferTile singleTile = new OfferTile('F', 2,
                List.of(new ActionSlot("UPPER_ROW", 1)));

        testRound.resolveOfferAction(player1, singleTile, List.of(card1, card2, card3));

        assertEquals(1, player1.getTribe().getCharacterCards().size());
    }

// -------------------------------------------------------
// resolveOfferAction — sconto costruttori sull'edificio
// -------------------------------------------------------

    @Test
    void testResolveOfferActionAppliesBuildingDiscountFromBuilder() {
        player1.addFood(5);
        player1.addCharacterCard(new BuilderCard(99, Era.I, 2, 0, 2)); // sconto 2

        BuildingCard building = new BuildingCard(50, Era.I, 3, p -> {}, 0); // costo 3
        Round testRound = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(List.of(building))
        );

        OfferTile buildingTile = new OfferTile('G', 2,
                List.of(new ActionSlot("UPPER_ROW", 1)));

        testRound.resolveOfferAction(player1, buildingTile, List.of(building));

        assertEquals(1, player1.getTribe().getBuildingCards().size());
        assertEquals(4, player1.getFood()); // 5 - (3 - 2) = 4
    }

// -------------------------------------------------------
// placeTotemOrderForPlayer — PAY_FOOD senza cibo sufficiente
// -------------------------------------------------------

    @Test
    void testPlaceTotemOrderPayFoodWithInsufficientFoodDeductsPP() {
        List<TurnSlot> effectSlots = new ArrayList<>();
        effectSlots.add(new TurnSlot(1, new PayFoodSlotEffect(1, 2)));
        effectSlots.add(new TurnSlot(2, new NoSlotEffect()));
        board.setTurnOrderTile(new TurnOrderTile(2, effectSlots));

        // player1 ha 0 cibo → non può pagare → perde 2 PP
        round.placeTotemOrderForPlayer(player1);

        assertEquals(0, player1.getFood());
        assertEquals(-2, player1.getPrestigePoints());
    }

// -------------------------------------------------------
// resolveEvent — carte evento nella riga inferiore
// -------------------------------------------------------

    @Test
    void testResolveEventExecutesLowerRowEventCards() {
        player1.addCharacterCard(new HunterCard(50, Era.I, 2, false));
        player1.addCharacterCard(new HunterCard(51, Era.I, 2, false));

        Round eventRound = new Round(1, game, RoundPhase.EVENT_RESOLUTION, Era.I,
                new ArrayList<>(List.of(new HuntEventCard(1, Era.I, false, 2, 3))),
                new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        game.setCurrentRound(eventRound);

        eventRound.resolveEvent();

        assertEquals(4, player1.getFood());           // 2 cacciatori × 2 cibo
        assertEquals(6, player1.getPrestigePoints()); // 2 cacciatori × 3 PP
    }

    @Test
    void testResolveEventDoesNothingWhenNoEventCards() {
        Round emptyEventRound = new Round(1, game, RoundPhase.EVENT_RESOLUTION, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        game.setCurrentRound(emptyEventRound);

        assertDoesNotThrow(() -> emptyEventRound.resolveEvent());
        assertEquals(0, player1.getPrestigePoints());
    }

}
