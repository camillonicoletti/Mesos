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
    void testCreateBoardWith2PlayersHas4OfferTiles() {
        // B(min2), C(min2), E(min2), F(min2) must be included
        Board board = BoardFactory.createBoard(2);
        assertEquals(4, board.getOfferTiles().size());
    }

    @Test
    void testCreateBoardWith3PlayersHas5OfferTiles() {
        // B(min2), C(min2), D(min3), E(min2), F(min2) must be present
        Board board = BoardFactory.createBoard(3);
        assertEquals(5, board.getOfferTiles().size());
    }

    @Test
    void testCreateBoardWith4PlayersHas6OfferTiles() {
        // B(min2), C(min2), D(min3), E(min2), F(min2), G(min4) must be present
        Board board = BoardFactory.createBoard(4);
        assertEquals(6, board.getOfferTiles().size());
    }

    @Test
    void testCreateBoardTileLettersCorrectFor2Players() {
        // with 2 players: B, C, E, F present; A(min5), D(min3), G(min4) absent
        Board board = BoardFactory.createBoard(2);
        List<Character> letters = board.getOfferTiles().stream()
                .map(OfferTile::getLetter)
                .toList();
        assertTrue(letters.contains('B'));
        assertTrue(letters.contains('C'));
        assertTrue(letters.contains('E'));
        assertTrue(letters.contains('F'));
        assertFalse(letters.contains('A'));
        assertFalse(letters.contains('D'));
        assertFalse(letters.contains('G'));
    }

    @Test
    void testCreateBoardTileLettersCorrectFor3Players() {
        // with 3 players: B, C, D, E, F present; A(min5), G(min4) absent
        Board board = BoardFactory.createBoard(3);
        List<Character> letters = board.getOfferTiles().stream()
                .map(OfferTile::getLetter)
                .toList();
        assertTrue(letters.contains('B'));
        assertTrue(letters.contains('C'));
        assertTrue(letters.contains('D'));
        assertTrue(letters.contains('E'));
        assertTrue(letters.contains('F'));
        assertFalse(letters.contains('A'));
        assertFalse(letters.contains('G'));
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
