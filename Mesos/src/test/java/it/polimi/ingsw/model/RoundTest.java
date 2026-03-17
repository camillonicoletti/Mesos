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
}
