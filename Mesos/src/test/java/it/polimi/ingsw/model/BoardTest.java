package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.*;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.enums.TotemPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {

    private Board board;
    private TurnOrderTile turnOrderTile;
    private List<OfferTile> offerTiles;
    private Totem totem1, totem2, totem3;

    @BeforeEach
    void setUp() {
        totem1 = new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK);
        totem2 = new Totem(TotemColor.BLUE, TotemPosition.OFFER_TRACK);
        totem3 = new Totem(TotemColor.PURPLE, TotemPosition.OFFER_TRACK);

        List<ActionSlot> slotsA = List.of(new ActionSlot("UPPER_ROW", 2));
        List<ActionSlot> slotsC = List.of(new ActionSlot("LOWER_ROW", 3));
        List<ActionSlot> slotsE = List.of(new ActionSlot("FOOD", 1));

        offerTiles = new ArrayList<>();
        offerTiles.add(new OfferTile('C', 2, slotsC));
        offerTiles.add(new OfferTile('A', 2, slotsA));
        offerTiles.add(new OfferTile('E', 3, slotsE));

        List<TurnSlot> turnSlots = new ArrayList<>();
        turnSlots.add(new TurnSlot(1, new GainFoodSlotEffect(2)));
        turnSlots.add(new TurnSlot(2, new NoSlotEffect()));
        turnSlots.add(new TurnSlot(3, new PayFoodSlotEffect(1, 3)));

        turnOrderTile = new TurnOrderTile(3, turnSlots);
        board = new Board(offerTiles, turnOrderTile);
    }

    // =========================================================
    // EDGE CASES FOR OFFER TILE / TURN SLOT
    // =========================================================

    @Test
    void testRemoveTotemFromEmptyOfferTileReturnsNull() {
        OfferTile tile = offerTiles.get(0);

        assertFalse(tile.isOccupied());
        assertNull(tile.removeTotem());
        assertFalse(tile.isOccupied());
    }

    @Test
    void testRemoveTotemFromEmptyTurnSlotReturnsNull() {
        TurnSlot slot = turnOrderTile.getSlots().get(0);

        assertFalse(slot.isOccupied());
        assertNull(slot.removeTotem());
        assertFalse(slot.isOccupied());
    }

    @Test
    void testPlaceTotemTwiceOnOfferTileOverridesPreviousTotem() {
        OfferTile tile = offerTiles.get(0);

        tile.placeTotem(totem1);
        assertEquals(totem1, tile.getTotemSlot());

        tile.placeTotem(totem2);
        assertEquals(totem2, tile.getTotemSlot());
        assertTrue(tile.isOccupied());
    }

    @Test
    void testPlaceTotemTwiceOnTurnSlotOverridesPreviousTotem() {
        TurnSlot slot = turnOrderTile.getSlots().get(0);

        slot.placeTotem(totem1);
        assertEquals(totem1, slot.getOccupiedBy());

        slot.placeTotem(totem2);
        assertEquals(totem2, slot.getOccupiedBy());
        assertTrue(slot.isOccupied());
    }

    // =========================================================
    // EDGE CASES FOR TURN ORDER TILE
    // =========================================================

    @Test
    void testGetNextFreeSlotReturnsNullWhenAllSlotsAreOccupied() {
        for (TurnSlot slot : turnOrderTile.getSlots()) {
            slot.placeTotem(totem1);
        }

        assertTrue(turnOrderTile.isFull());
        assertNull(turnOrderTile.getNextFreeSlot());
    }

    @Test
    void testGetTurnOrderReturnsEmptyListWhenNoTurnSlotIsOccupied() {
        List<Totem> order = turnOrderTile.getTurnOrder();

        assertNotNull(order);
        assertTrue(order.isEmpty());
    }

    @Test
    void testAssignTurnOrderWithNoOccupiedOfferTilesProducesEmptyOrder() {
        turnOrderTile.assignTurnOrder(board);

        List<Totem> order = turnOrderTile.getTurnOrder();
        assertNotNull(order);
        assertTrue(order.isEmpty());
    }

    @Test
    void testAssignTurnOrderWithOnlyOneOccupiedOfferTile() {
        offerTiles.get(0).placeTotem(totem1); // C

        turnOrderTile.assignTurnOrder(board);

        List<Totem> order = turnOrderTile.getTurnOrder();

        assertEquals(1, order.size());
        assertEquals(totem1, order.get(0));
    }

    @Test
    void testAssignTurnOrderIgnoresUnoccupiedOfferTiles() {
        offerTiles.get(1).placeTotem(totem2); // A
        // C ed E restano vuote

        turnOrderTile.assignTurnOrder(board);

        List<Totem> order = turnOrderTile.getTurnOrder();

        assertEquals(1, order.size());
        assertEquals(totem2, order.get(0));
    }

    // =========================================================
    // CRITICAL EDGE CASES
    // =========================================================

    @Test
    void testAssignTurnOrderShouldResetPreviousTurnSlots() {

        turnOrderTile.getSlots().get(0).placeTotem(totem3);
        turnOrderTile.getSlots().get(1).placeTotem(totem1);
        turnOrderTile.getSlots().get(2).placeTotem(totem2);

        // new state on offer tiles

        offerTiles.get(1).placeTotem(totem2); // A
        offerTiles.get(0).placeTotem(totem1); // C


        turnOrderTile.assignTurnOrder(board);

        List<Totem> order = turnOrderTile.getTurnOrder();

        // if assignTurnOrder does NOT reset, you might still find 3 totems here
        assertEquals(2, order.size());
        assertEquals(totem2, order.get(0)); // A -> posizione 1
        assertEquals(totem1, order.get(1)); // C -> posizione 2
    }



    @Test
    void testGetTurnOrderShouldBeSortedByPositionEvenIfSlotsListIsUnordered() {
        TurnSlot slot3 = new TurnSlot(3, new NoSlotEffect());
        TurnSlot slot1 = new TurnSlot(1, new NoSlotEffect());
        TurnSlot slot2 = new TurnSlot(2, new NoSlotEffect());

        slot3.placeTotem(totem3);
        slot1.placeTotem(totem1);
        slot2.placeTotem(totem2);

        List<TurnSlot> unorderedSlots = new ArrayList<>();
        unorderedSlots.add(slot3);
        unorderedSlots.add(slot1);
        unorderedSlots.add(slot2);

        TurnOrderTile unorderedTile = new TurnOrderTile(3, unorderedSlots);

        List<Totem> order = unorderedTile.getTurnOrder();

        assertEquals(3, order.size());
        assertEquals(totem1, order.get(0)); // posizione 1
        assertEquals(totem2, order.get(1)); // posizione 2
        assertEquals(totem3, order.get(2)); // posizione 3
    }

    @Test
    void testAssignTurnOrderShouldRespectAlphabeticalOrderEvenIfOfferTilesListIsUnordered() {
        List<ActionSlot> slotsB = List.of(new ActionSlot("FOOD", 1));
        List<ActionSlot> slotsA = List.of(new ActionSlot("UPPER_ROW", 1));
        List<ActionSlot> slotsD = List.of(new ActionSlot("LOWER_ROW", 1));

        OfferTile tileD = new OfferTile('D', 2, slotsD);
        OfferTile tileB = new OfferTile('B', 2, slotsB);
        OfferTile tileA = new OfferTile('A', 2, slotsA);

        tileD.placeTotem(totem3);
        tileB.placeTotem(totem2);
        tileA.placeTotem(totem1);

        List<OfferTile> unorderedOfferTiles = new ArrayList<>();
        unorderedOfferTiles.add(tileD);
        unorderedOfferTiles.add(tileB);
        unorderedOfferTiles.add(tileA);

        List<TurnSlot> slots = new ArrayList<>();
        // DOPO
        slots.add(new TurnSlot(1, new NoSlotEffect()));
        slots.add(new TurnSlot(2, new NoSlotEffect()));
        slots.add(new TurnSlot(3, new NoSlotEffect()));

        TurnOrderTile localTurnOrderTile = new TurnOrderTile(3, slots);
        Board localBoard = new Board(unorderedOfferTiles, localTurnOrderTile);

        localTurnOrderTile.assignTurnOrder(localBoard);

        List<Totem> order = localTurnOrderTile.getTurnOrder();

        assertEquals(3, order.size());
        assertEquals(totem1, order.get(0)); // A
        assertEquals(totem2, order.get(1)); // B
        assertEquals(totem3, order.get(2)); // D
    }

    @Test
    void testResetOnEmptyTurnOrderTileDoesNotThrowAndLeavesAllSlotsFree() {
        assertDoesNotThrow(() -> turnOrderTile.reset());

        for (TurnSlot slot : turnOrderTile.getSlots()) {
            assertFalse(slot.isOccupied());
        }
    }

    @Test
    void testAssignRandomTurnOrderPlacesAllTotemsInSlots() {
        List<Totem> totems = List.of(totem1, totem2, totem3);

        turnOrderTile.assignRandomTurnOrder(totems);

        List<Totem> order = turnOrderTile.getTurnOrder();

        assertEquals(3, order.size());
        assertTrue(order.contains(totem1));
        assertTrue(order.contains(totem2));
        assertTrue(order.contains(totem3));
    }

    @Test
    void testAssignRandomTurnOrderResetsExistingSlots() {
        turnOrderTile.getSlots().get(0).placeTotem(totem1);
        turnOrderTile.getSlots().get(1).placeTotem(totem2);
        turnOrderTile.getSlots().get(2).placeTotem(totem3);

        // Reassign with only 2 totems — slot 3 must be empty after reset
        List<Totem> totems = List.of(totem1, totem2);
        turnOrderTile.assignRandomTurnOrder(totems);

        List<Totem> order = turnOrderTile.getTurnOrder();

        assertEquals(2, order.size());
        assertTrue(order.contains(totem1));
        assertTrue(order.contains(totem2));
    }

    @Test
    void testAssignRandomTurnOrderWithEmptyListLeavesAllSlotsFree() {
        // assignRandomTurnOrder with empty list must leave all slots empty
        turnOrderTile.assignRandomTurnOrder(new ArrayList<>());

        List<Totem> order = turnOrderTile.getTurnOrder();
        assertTrue(order.isEmpty());
        for (TurnSlot slot : turnOrderTile.getSlots()) {
            assertFalse(slot.isOccupied());
        }
    }

    // =========================================================
// TESTS FOR createForPlayers()
// =========================================================

    @Test
    void testCreateForPlayersReturnsCorrectNumberOfSlotsFor2Players() {
        TurnOrderTile tile = TurnOrderTile.createForPlayers(2);
        assertEquals(2, tile.getSlots().size());
    }

    @Test
    void testCreateForPlayersReturnsCorrectNumberOfSlotsFor3Players() {
        TurnOrderTile tile = TurnOrderTile.createForPlayers(3);
        assertEquals(3, tile.getSlots().size());
    }

    @Test
    void testCreateForPlayersReturnsCorrectNumberOfSlotsFor4Players() {
        TurnOrderTile tile = TurnOrderTile.createForPlayers(4);
        assertEquals(4, tile.getSlots().size());
    }

    @Test
    void testCreateForPlayersReturnsCorrectNumberOfSlotsFor5Players() {
        TurnOrderTile tile = TurnOrderTile.createForPlayers(5);
        assertEquals(5, tile.getSlots().size());
    }

    @Test
    void testCreateForPlayersFirstSlotIsAlwaysGainFood() {
        for (int n = 2; n <= 5; n++) {
            TurnOrderTile tile = TurnOrderTile.createForPlayers(n);
            TurnSlot first = tile.getSlots().stream()
                    .min(Comparator.comparingInt(TurnSlot::getPosition))
                    .orElseThrow();
            assertInstanceOf(GainFoodSlotEffect.class, first.getEffect(),
                    "First slot should be GainFoodSlotEffect for " + n + " players");
        }
    }

    @Test
    void testCreateForPlayersLastSlotIsAlwaysPayFood() {
        for (int n = 2; n <= 5; n++) {
            TurnOrderTile tile = TurnOrderTile.createForPlayers(n);
            TurnSlot last = tile.getSlots().stream()
                    .max(Comparator.comparingInt(TurnSlot::getPosition))
                    .orElseThrow();
            assertInstanceOf(PayFoodSlotEffect.class, last.getEffect(),
                    "Last slot should be PayFoodSlotEffect for " + n + " players");
        }
    }

    @Test
    void testCreateForPlayersAllSlotsHaveUniquePositions() {
        TurnOrderTile tile = TurnOrderTile.createForPlayers(4);
        long distinctPositions = tile.getSlots().stream()
                .map(TurnSlot::getPosition)
                .distinct()
                .count();
        assertEquals(tile.getSlots().size(), distinctPositions);
    }

    @Test
    void testCreateForPlayersThrowsForUnsupportedPlayerCount() {
        assertThrows(IllegalArgumentException.class, () -> TurnOrderTile.createForPlayers(1));
        assertThrows(IllegalArgumentException.class, () -> TurnOrderTile.createForPlayers(6));
    }

    @Test
    void testCreateForPlayersAllSlotsStartUnoccupied() {
        TurnOrderTile tile = TurnOrderTile.createForPlayers(3);
        for (TurnSlot slot : tile.getSlots()) {
            assertFalse(slot.isOccupied());
        }
    }
    // -------------------------------------------------------
// resetOfferTiles
// -------------------------------------------------------

    @Test
    void testResetOfferTilesClearsAllTotems() {
        // place totems on all offer tiles, then reset — all must be free
        offerTiles.get(0).placeTotem(totem1);
        offerTiles.get(1).placeTotem(totem2);

        board.resetOfferTiles();

        for (OfferTile tile : board.getOfferTiles()) {
            assertFalse(tile.isOccupied());
        }
    }

    @Test
    void testResetOfferTilesOnEmptyBoardDoesNotThrow() {
        assertDoesNotThrow(() -> board.resetOfferTiles());
    }

    @Test
    void testResetOfferTilesOnlyOnOccupiedTiles() {
        // only one tile occupied — after reset all must be free
        offerTiles.get(0).placeTotem(totem1);

        board.resetOfferTiles();

        assertFalse(offerTiles.get(0).isOccupied());
        assertFalse(offerTiles.get(1).isOccupied());
    }

// -------------------------------------------------------
// TotemPosition updates via OfferTile and TurnSlot
// -------------------------------------------------------

    @Test
    void testPlaceTotemOnOfferTileSetsPositionToOfferTrack() {
        Totem t = new Totem(TotemColor.RED, TotemPosition.TURN_ORDER);
        offerTiles.get(0).placeTotem(t);
        assertEquals(TotemPosition.OFFER_TRACK, t.getPosition());
    }

    @Test
    void testPlaceTotemOnTurnSlotSetsPositionToTurnOrder() {
        Totem t = new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK);
        turnOrderTile.getSlots().get(0).placeTotem(t);
        assertEquals(TotemPosition.TURN_ORDER, t.getPosition());
    }

    @Test
    void testRemoveTotemFromTurnSlotResetsPositionToOfferTrack() {
        Totem t = new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK);
        turnOrderTile.getSlots().get(0).placeTotem(t);
        turnOrderTile.getSlots().get(0).removeTotem();
        assertEquals(TotemPosition.OFFER_TRACK, t.getPosition());
    }




}