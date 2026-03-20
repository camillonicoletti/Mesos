package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.*;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Game.java — covers all public methods:
 *
 * Getters: getPlayers, getBoard, getCurrentEra, getTributeDeck,
 *          getRemainingTributeDeck, getRemainingBuildingDeck,
 *          getCurrentRound, setCurrentRound, getBuildingsForEra
 *
 * Lifecycle: start, end, isGameOver
 *
 * Era management: nextEra, endEra
 *
 * Round management: nextRound, endRound (multi-round simulation)
 */
public class GameTest {

    private Game game;
    private Round round;
    private Player player1, player2;
    private OfferTile tileA, tileB;
    private Board board;
    private List<Card> tributeDeck;
    private List<BuildingCard> era1Deck, era2Deck, era3Deck;

    private final Effect noEffect = player -> {};

    @BeforeEach
    void setUp() {
        player1 = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player2 = new Player("Bob", new Totem(TotemColor.BLUE, TotemPosition.OFFER_TRACK));

        tileA = new OfferTile('A', 2, List.of(new ActionSlot(SlotType.UPPER_ROW, 2)));
        tileB = new OfferTile('B', 2, List.of(new ActionSlot(SlotType.FOOD, 3)));

        List<TurnSlot> turnSlots = new ArrayList<>();
        turnSlots.add(new TurnSlot(1, SlotEffect.NONE, 0, 0));
        turnSlots.add(new TurnSlot(2, SlotEffect.NONE, 0, 0));

        board = new Board(List.of(tileA, tileB), new TurnOrderTile(2, turnSlots));

        tributeDeck = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            tributeDeck.add(new HunterCard(false, Era.I));
        }

        era1Deck = new ArrayList<>(List.of(new BuildingCard(2, noEffect, Era.I, 1)));
        era2Deck = new ArrayList<>(List.of(
                new BuildingCard(3, noEffect, Era.II, 2),
                new BuildingCard(4, noEffect, Era.II, 3)
        ));
        era3Deck = new ArrayList<>(List.of(
                new BuildingCard(5, noEffect, Era.III, 1),
                new BuildingCard(6, noEffect, Era.III, 1),
                new BuildingCard(7, noEffect, Era.III, 1)
        ));

        game = new Game(
                List.of(player1, player2),
                1, null, Era.I, null,
                board, tributeDeck,
                era1Deck, era2Deck, era3Deck,
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

    // =========================================================
    // GETTERS
    // Verifies that all getters return the values passed to the
    // constructor or set via setter
    // =========================================================

    @Test
    void testGetPlayers() {
        // must return exactly the 2 players created in setUp
        assertEquals(2, game.getPlayers().size());
        assertTrue(game.getPlayers().contains(player1));
        assertTrue(game.getPlayers().contains(player2));
    }

    @Test
    void testGetBoard() {
        // must return the same board object passed to the constructor
        assertEquals(board, game.getBoard());
    }

    @Test
    void testGetCurrentEra() {
        // game starts at Era.I
        assertEquals(Era.I, game.getCurrentEra());
    }

    @Test
    void testGetTributeDeck() {
        // must return the same reference to the tribute deck
        assertEquals(tributeDeck, game.getTributeDeck());
    }

    @Test
    void testGetRemainingTributeDeck() {
        // tribute deck was created with 30 cards
        assertEquals(30, game.getRemainingTributeDeck());
    }

    @Test
    void testGetRemainingBuildingDeck() {
        // era1=1, era2=2, era3=3 → total 6
        assertEquals(6, game.getRemainingBuildingDeck());
    }

    @Test
    void testGetCurrentRoundAndSetCurrentRound() {
        // getCurrentRound must return the round set via setCurrentRound
        assertEquals(round, game.getCurrentRound());

        Round newRound = new Round(2, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        game.setCurrentRound(newRound);
        assertEquals(newRound, game.getCurrentRound());
    }

    @Test
    void testGetBuildingsForEra() {
        // each era must return its own building deck
        assertEquals(era1Deck, game.getBuildingsForEra(Era.I));
        assertEquals(era2Deck, game.getBuildingsForEra(Era.II));
        assertEquals(era3Deck, game.getBuildingsForEra(Era.III));
    }

    // =========================================================
    // START
    // start() can only be called on a game in WAITING status:
    // correctly sets era, round and status
    // =========================================================

    @Test
    void testStartSetsStatusAndEraAndRound() {
        // create a fresh game in WAITING status
        Game freshGame = new Game(
                List.of(player1, player2),
                0, null, null, null,
                board, new ArrayList<>(tributeDeck),
                new ArrayList<>(era1Deck), new ArrayList<>(era2Deck), new ArrayList<>(era3Deck),
                GameStatus.WAITING
        );

        freshGame.start();

        // after start: era = I, round created, game is not over
        assertEquals(Era.I, freshGame.getCurrentEra());
        assertNotNull(freshGame.getCurrentRound());
        assertFalse(freshGame.isGameOver());
    }

    @Test
    void testStartThrowsIfNotWaiting() {
        // game is already IN_PROGRESS → start() must throw
        assertThrows(IllegalStateException.class, () -> game.start());
    }

    // =========================================================
    // END / IS GAME OVER
    // end() sets status to FINISHED
    // isGameOver() is true if FINISHED or round > MAX_ROUNDS (10)
    // =========================================================

    @Test
    void testIsGameOverFalseAtStart() {
        // at round 1 the game is not over
        assertFalse(game.isGameOver());
    }

    @Test
    void testEndSetsGameOver() {
        // after end() the game must be over
        game.end();
        assertTrue(game.isGameOver());
    }

    @Test
    void testIsGameOverTrueAtMaxRounds() {
        // isGameOver() checks currentRoundNumber > MAX_ROUNDS(10), so 11 triggers it
        Game maxRoundGame = new Game(
                List.of(player1, player2),
                11, null, Era.I, null,
                board, new ArrayList<>(tributeDeck),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                GameStatus.IN_PROGRESS
        );
        assertTrue(maxRoundGame.isGameOver());
    }

    // =========================================================
    // NEXT ERA / END ERA
    // nextEra() advances the current era (I→II→III, throws beyond that)
    // endEra() manages buildings and calls nextEra()
    // =========================================================

    @Test
    void testNextEraFromIToII() {
        game.nextEra();
        assertEquals(Era.II, game.getCurrentEra());
    }

    @Test
    void testNextEraFromIIToIII() {
        game.nextEra();
        game.nextEra();
        assertEquals(Era.III, game.getCurrentEra());
    }

    @Test
    void testNextEraFromIIIThrows() {
        // cannot advance beyond Era III
        game.nextEra();
        game.nextEra();
        assertThrows(IllegalStateException.class, () -> game.nextEra());
    }

    @Test
    void testEndEraFromIAdvancesToII() {
        // endEra() moves buildings and advances the era
        game.endEra();
        assertEquals(Era.II, game.getCurrentEra());
    }

    @Test
    void testEndEraFromIIAdvancesToIII() {
        // force Era II without touching buildings, then endEra
        game.nextEra();
        game.endEra();
        assertEquals(Era.III, game.getCurrentEra());
    }

    @Test
    void testEndEraFromIIIDoesNothing() {
        // from Era III endEra() returns immediately without any changes
        game.nextEra();
        game.nextEra();
        game.endEra();
        assertEquals(Era.III, game.getCurrentEra());
    }

    // =========================================================
    // NEXT ROUND / END ROUND — multi-round simulation
    // nextRound() creates the next round incrementing the number
    // endRound() executes the current round then calls nextRound()
    // =========================================================

    @Test
    void testNextRoundIncrementsNumber() {
        // round number must increase by 1
        int before = game.getCurrentRound().getNumber();
        game.nextRound();
        assertEquals(before + 1, game.getCurrentRound().getNumber());
    }

    @Test
    void testNextRoundCreatesNewRound() {
        // must be a different Round object from the previous one
        Round oldRound = game.getCurrentRound();
        game.nextRound();
        assertNotSame(oldRound, game.getCurrentRound());
    }

    @Test
    void testEndRoundThrowsIfNotInProgress() {
        // endRound() cannot be called if the game is over
        game.end();
        assertThrows(IllegalStateException.class, () -> game.endRound());
    }

    @Test
    void testEndRoundAdvancesToRound2() {
        // after endRound the game must be at round 2
        round.placeTotemOffer(player1, tileA);
        round.placeTotemOffer(player2, tileB);

        game.endRound();

        assertEquals(2, game.getCurrentRound().getNumber());
    }

    @Test
    void testTwoCompleteRounds() {
        // --- Round 1 ---
        // player1 picks tileA (UPPER_ROW) — no cards taken since chosenCards is empty (no controller yet)
        // player2 picks tileB (FOOD 3) — receives 3 food
        round.placeTotemOffer(player1, tileA);
        round.placeTotemOffer(player2, tileB);

        game.endRound();

        assertEquals(0, player1.getTribe().getCharacterCards().size()); // empty chosenCards
        assertEquals(3, player2.getFood());
        assertEquals(2, game.getCurrentRound().getNumber());

        // --- Round 2 ---
        // roles are swapped:
        // player2 picks tileA — no cards taken (no controller)
        // player1 picks tileB — receives 3 food
        Round round2 = game.getCurrentRound();

        round2.placeTotemOffer(player2, tileA);
        round2.placeTotemOffer(player1, tileB);

        game.endRound();

        assertEquals(3, player1.getFood());
        assertEquals(0, player2.getTribe().getCharacterCards().size()); // empty chosenCards
        assertEquals(3, game.getCurrentRound().getNumber());
    }

    @Test
    void testEndRoundEndsGameAtMaxRounds() {
        // game at round 10: endRound → nextRound sees 10 >= MAX_ROUNDS → calls end()
        Game nearEndGame = new Game(
                List.of(player1, player2),
                10, null, Era.I, null,
                board, new ArrayList<>(tributeDeck),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                GameStatus.IN_PROGRESS
        );

        Round nearEndRound = new Round(10, nearEndGame, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        nearEndGame.setCurrentRound(nearEndRound);

        nearEndGame.endRound();

        assertTrue(nearEndGame.isGameOver());
    }

    @Test
    void testNextRoundAtMaxRoundsEndsGame() {
        // nextRound() called directly when already at round 10 must call end()
        Game maxGame = new Game(
                List.of(player1, player2),
                10, null, Era.I, null,
                board, new ArrayList<>(tributeDeck),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                GameStatus.IN_PROGRESS
        );
        Round maxRound = new Round(10, maxGame, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        maxGame.setCurrentRound(maxRound);

        maxGame.nextRound();

        assertTrue(maxGame.isGameOver());
    }

    @Test
    void testEndEraFromIIDiscardsLowerAndMovesUpperBuildings() {
        // from Era II: endEra must discard lower buildings and move upper to lower
        game.nextEra(); // now Era II

        List<BuildingCard> lower = new ArrayList<>(List.of(new BuildingCard(1, noEffect, Era.I, 2)));
        List<BuildingCard> upper = new ArrayList<>(List.of(new BuildingCard(2, noEffect, Era.II, 3)));

        Round eraIIRound = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.II,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                lower, upper
        );
        game.setCurrentRound(eraIIRound);

        game.endEra(); // Era II → discards lower, moves upper to lower, advances to III

        assertEquals(Era.III, game.getCurrentEra());
        // after endEra from II: old lower is gone, upper has moved to lower
        assertEquals(1, eraIIRound.getLowerRowBuildings().size());
    }

    @Test
    void testGetBuildingsForEraReturnsEmptyListNotNull() {
        // even with empty decks, getBuildingsForEra must return empty list (not null)
        Game emptyGame = new Game(
                List.of(player1, player2),
                1, null, Era.I, null,
                board, new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                GameStatus.IN_PROGRESS
        );

        assertNotNull(emptyGame.getBuildingsForEra(Era.I));
        assertNotNull(emptyGame.getBuildingsForEra(Era.II));
        assertNotNull(emptyGame.getBuildingsForEra(Era.III));
        assertTrue(emptyGame.getBuildingsForEra(Era.I).isEmpty());
    }

    @Test
    void testIsGameOverTrueWhenFinishedButRoundLow() {
        // isGameOver must be true if status = FINISHED even if round < MAX_ROUNDS
        game.end();
        assertTrue(game.isGameOver());
    }

    @Test
    void testEndCalledTwiceDoesNotThrow() {
        // calling end() twice must not throw
        game.end();
        assertDoesNotThrow(() -> game.end());
        assertTrue(game.isGameOver());
    }
}
