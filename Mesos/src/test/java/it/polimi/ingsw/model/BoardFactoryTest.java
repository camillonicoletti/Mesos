package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.enums.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BoardFactory.
 *
 * BoardFactory reads offer_tiles.json and filters tiles based on the number of players.
 * The current JSON contains three tiles:
 *   - A (minPlayers: 2) — always included
 *   - B (minPlayers: 2) — always included
 *   - C (minPlayers: 3) — only included with 3 or more players
 *
 * This test suite verifies:
 *   1. Offer tile filtering: the correct tiles are included/excluded per player count
 *   2. TurnOrderTile creation: the factory produces a tile with the right number of slots
 *   3. Initial tile state: all offer tiles start unoccupied
 */
public class BoardFactoryTest {

    // =========================================================
    // BOARD FACTORY — offer tile filtering by number of players
    // =========================================================

    @Test
    void testCreateBoardWith2PlayersHas2OfferTiles() {
        // A (min2) and B (min2) must be included; C (min3) must be excluded
        Board board = BoardFactory.createBoard(2);
        assertEquals(2, board.getOfferTiles().size());
    }

    @Test
    void testCreateBoardWith3PlayersHas3OfferTiles() {
        // A (min2), B (min2), C (min3) — all three tiles must be present
        Board board = BoardFactory.createBoard(3);
        assertEquals(3, board.getOfferTiles().size());
    }

    @Test
    void testCreateBoardWith4PlayersHas3OfferTiles() {
        // same tiles as 3 players — C is the most restrictive (min3)
        Board board = BoardFactory.createBoard(4);
        assertEquals(3, board.getOfferTiles().size());
    }

    @Test
    void testCreateBoardTileLettersCorrectFor2Players() {
        // with 2 players only A and B must be present; C must be absent
        Board board = BoardFactory.createBoard(2);
        List<Character> letters = board.getOfferTiles().stream()
                .map(OfferTile::getLetter)
                .toList();
        assertTrue(letters.contains('A'));
        assertTrue(letters.contains('B'));
        assertFalse(letters.contains('C'));
    }

    @Test
    void testCreateBoardTileLettersCorrectFor3Players() {
        // with 3 players A, B and C must all be present
        Board board = BoardFactory.createBoard(3);
        List<Character> letters = board.getOfferTiles().stream()
                .map(OfferTile::getLetter)
                .toList();
        assertTrue(letters.contains('A'));
        assertTrue(letters.contains('B'));
        assertTrue(letters.contains('C'));
    }

    @Test
    void testCreateBoardTurnOrderTileHasCorrectSlots() {
        // the TurnOrderTile must have exactly as many slots as there are players
        assertEquals(2, BoardFactory.createBoard(2).getTurnOrderTile().getSlots().size());
        assertEquals(3, BoardFactory.createBoard(3).getTurnOrderTile().getSlots().size());
        assertEquals(4, BoardFactory.createBoard(4).getTurnOrderTile().getSlots().size());
    }

    @Test
    void testCreateBoardAllTilesStartUnoccupied() {
        // no offer tile should be occupied immediately after board creation
        Board board = BoardFactory.createBoard(3);
        for (OfferTile tile : board.getOfferTiles()) {
            assertFalse(tile.isOccupied());
        }
    }
}
