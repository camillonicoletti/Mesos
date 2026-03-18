package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.*;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Round.java
 *
 * Covers 3 areas:
 *
 * 1. placeTotemOffer
 *    - successful placement occupies the tile and saves the choice in playerChoices
 *    - fails if the tile is already occupied
 *    - fails if the tile requires more players than are in the game
 *
 * 2. execute — ACTION_RESOLUTION
 *    - player who chose an UPPER_ROW tile receives the correct number of cards
 *    - player who chose a FOOD tile receives the correct amount of food
 *
 * 3. execute — ROUND_CLEANUP
 *    - lower row is cleared when discardLowerRow() is called directly
 *    - upper row shifts to lower row (minus cards taken by players)
 *
 * 4. checkEraTransition
 *    - era advances to II if an Era II card appears in the upper row
 *    - era stays at I if no Era II card is present
 */
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
        for (int i = 0; i < 6; i++) {
            tributeDeck.add(new HunterCard(false, Era.I));
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
                        new HunterCard(false, Era.I),
                        new HunterCard(false, Era.I),
                        new HunterCard(false, Era.I)
                )),
                new ArrayList<>(List.of(
                        new HunterCard(false, Era.I),
                        new HunterCard(false, Era.I),
                        new HunterCard(false, Era.I),
                        new HunterCard(false, Era.I),
                        new HunterCard(false, Era.I)
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
        round.execute();
        assertEquals(2, player1.getTribe().getCharacterCards().size());
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
        int upperSizeBefore = round.getUpperTributeRowSize();
        round.placeTotemOffer(player1, tileA);
        round.execute();
        assertEquals(upperSizeBefore - 2, round.getLowerTributeRowSize());
    }

    // -------------------------------------------------------
    // checkEraTransition
    // -------------------------------------------------------

    @Test
    void testCheckEraTransitionAdvancesToEraII() {
        round.getUpperRowTribute().add(new HunterCard(false, Era.II));
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
        // execute() with no placements must run all phases without crashing
        assertDoesNotThrow(() -> round.execute());
    }

    @Test
    void testPlaceTotemOfferSamePlayerTwiceOverwritesChoice() {
        // if the same player places totem twice (different tiles), the second choice wins
        round.placeTotemOffer(player1, tileA);
        tileA.removeTotem(); // free tileA to avoid "occupied" block
        round.placeTotemOffer(player1, tileB);
        round.execute();

        // player1 chose tileB (FOOD 3), must have 3 food and 0 cards
        assertEquals(3, player1.getFood());
        assertEquals(0, player1.getTribe().getCharacterCards().size());
    }

    @Test
    void testResolveOfferActionWithEmptyUpperRowDoesNotThrow() {
        // resolveOfferAction on an UPPER_ROW tile with empty row must not throw
        Round emptyRound = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), // upper tribute empty
                new ArrayList<>(), new ArrayList<>()
        );
        game.setCurrentRound(emptyRound);

        assertDoesNotThrow(() -> emptyRound.resolveOfferAction(player1, tileA));
        assertEquals(0, player1.getTribe().getCharacterCards().size());
    }

    @Test
    void testCreateNextRoundHasIndependentLists() {
        // modifying round1's lists must not affect round2
        Round round2 = round.createNextRound(2);

        round.getUpperRowTribute().clear();
        round.getLowerRowTribute().clear();

        // round2 must still have its own copies
        assertNotSame(round.getUpperRowTribute(), round2.getUpperRowTribute());
        assertNotSame(round.getLowerRowTribute(), round2.getLowerRowTribute());
    }

    @Test
    void testRefillUpperRowStopsWhenTributeDeckIsEmpty() {
        // empty the tribute deck: refillUpperRow must not throw
        game.getTributeDeck().clear();

        assertDoesNotThrow(() -> round.refillUpperRow());
    }

    @Test
    void testCheckEraTransitionInEraIIIDoesNothing() {
        // in Era III checkEraTransition must not call endEra or throw
        game.nextEra();
        game.nextEra(); // now Era III

        Round eraIIIRound = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.III,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        game.setCurrentRound(eraIIIRound);

        assertDoesNotThrow(() -> eraIIIRound.checkEraTransition());
        assertEquals(Era.III, game.getCurrentEra()); // era must stay III
    }

    @Test
    void testShiftUpperToLowerRowClearsPreviousLowerRow() {
        // after shiftUpperToLowerRow, the old lower row content is gone
        int upperSizeBefore = round.getUpperTributeRowSize();

        round.shiftUpperToLowerRow();

        // new lower row = old upper row
        assertEquals(upperSizeBefore, round.getLowerTributeRowSize());
        // upper row is now empty
        assertEquals(0, round.getUpperTributeRowSize());
    }

}
