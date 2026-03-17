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

        era1Deck = new ArrayList<>(List.of(new BuildingCard(2, noEffect, Era.I)));
        era2Deck = new ArrayList<>(List.of(
                new BuildingCard(3, noEffect, Era.II),
                new BuildingCard(4, noEffect, Era.II)
        ));
        era3Deck = new ArrayList<>(List.of(
                new BuildingCard(5, noEffect, Era.III),
                new BuildingCard(6, noEffect, Era.III),
                new BuildingCard(7, noEffect, Era.III)
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
    // verifica che tutti i getter restituiscano i valori passati
    // nel costruttore o impostati tramite setter
    // =========================================================

    @Test
    void testGetPlayers() {
        // deve restituire esattamente i 2 giocatori creati nel setup
        assertEquals(2, game.getPlayers().size());
        assertTrue(game.getPlayers().contains(player1));
        assertTrue(game.getPlayers().contains(player2));
    }

    @Test
    void testGetBoard() {
        // deve restituire lo stesso oggetto board passato al costruttore
        assertEquals(board, game.getBoard());
    }

    @Test
    void testGetCurrentEra() {
        // il game parte da Era.I
        assertEquals(Era.I, game.getCurrentEra());
    }

    @Test
    void testGetTributeDeck() {
        // deve restituire lo stesso riferimento al mazzo tribute
        assertEquals(tributeDeck, game.getTributeDeck());
    }

    @Test
    void testGetRemainingTributeDeck() {
        // il mazzo tribute è stato creato con 30 carte
        assertEquals(30, game.getRemainingTributeDeck());
    }

    @Test
    void testGetRemainingBuildingDeck() {
        // era1=1, era2=2, era3=3 → totale 6
        assertEquals(6, game.getRemainingBuildingDeck());
    }

    @Test
    void testGetCurrentRoundAndSetCurrentRound() {
        // getCurrentRound deve restituire il round impostato con setCurrentRound
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
        // ogni era deve restituire il proprio mazzo buildings
        assertEquals(era1Deck, game.getBuildingsForEra(Era.I));
        assertEquals(era2Deck, game.getBuildingsForEra(Era.II));
        assertEquals(era3Deck, game.getBuildingsForEra(Era.III));
    }

    // =========================================================
    // START
    // start() può essere chiamato solo su un game in stato WAITING:
    // imposta era, round e status correttamente
    // =========================================================

    @Test
    void testStartSetsStatusAndEraAndRound() {
        // crea un game fresco in stato WAITING
        Game freshGame = new Game(
                List.of(player1, player2),
                0, null, null, null,
                board, new ArrayList<>(tributeDeck),
                new ArrayList<>(era1Deck), new ArrayList<>(era2Deck), new ArrayList<>(era3Deck),
                GameStatus.WAITING
        );

        freshGame.start();

        // dopo start: era = I, round creato, non è game over
        assertEquals(Era.I, freshGame.getCurrentEra());
        assertNotNull(freshGame.getCurrentRound());
        assertFalse(freshGame.isGameOver());
    }

    @Test
    void testStartThrowsIfNotWaiting() {
        // il game è già IN_PROGRESS → start() deve lanciare eccezione
        assertThrows(IllegalStateException.class, () -> game.start());
    }

    // =========================================================
    // END / IS GAME OVER
    // end() imposta lo stato FINISHED
    // isGameOver() è true se FINISHED oppure round >= MAX_ROUNDS (10)
    // =========================================================

    @Test
    void testIsGameOverFalseAtStart() {
        // al round 1 il game non è finito
        assertFalse(game.isGameOver());
    }

    @Test
    void testEndSetsGameOver() {
        // dopo end() il game risulta terminato
        game.end();
        assertTrue(game.isGameOver());
    }

    @Test
    void testIsGameOverTrueAtMaxRounds() {
        // un game al round 10 (MAX_ROUNDS) è già game over
        Game maxRoundGame = new Game(
                List.of(player1, player2),
                10, null, Era.I, null,
                board, new ArrayList<>(tributeDeck),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                GameStatus.IN_PROGRESS
        );
        assertTrue(maxRoundGame.isGameOver());
    }

    // =========================================================
    // NEXT ERA / END ERA
    // nextEra() avanza l'era corrente (I→II→III, oltre lancia eccezione)
    // endEra() gestisce i buildings e chiama nextEra()
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
        // non si può avanzare oltre Era III
        game.nextEra();
        game.nextEra();
        assertThrows(IllegalStateException.class, () -> game.nextEra());
    }

    @Test
    void testEndEraFromIAdvancesToII() {
        // endEra() sposta i buildings e avanza l'era
        game.endEra();
        assertEquals(Era.II, game.getCurrentEra());
    }

    @Test
    void testEndEraFromIIAdvancesToIII() {
        // forziamo Era II senza toccare buildings, poi endEra
        game.nextEra();
        game.endEra();
        assertEquals(Era.III, game.getCurrentEra());
    }

    @Test
    void testEndEraFromIIIDoesNothing() {
        // da Era III endEra() esce subito senza modificare nulla
        game.nextEra();
        game.nextEra();
        game.endEra();
        assertEquals(Era.III, game.getCurrentEra());
    }

    // =========================================================
    // NEXT ROUND / END ROUND — simulazione multi-round
    // nextRound() crea il round successivo incrementando il numero
    // endRound() esegue il round corrente poi chiama nextRound()
    // =========================================================

    @Test
    void testNextRoundIncrementsNumber() {
        // il numero del round deve aumentare di 1
        int before = game.getCurrentRound().getNumber();
        game.nextRound();
        assertEquals(before + 1, game.getCurrentRound().getNumber());
    }

    @Test
    void testNextRoundCreatesNewRound() {
        // deve essere un oggetto Round diverso da quello precedente
        Round oldRound = game.getCurrentRound();
        game.nextRound();
        assertNotSame(oldRound, game.getCurrentRound());
    }

    @Test
    void testEndRoundThrowsIfNotInProgress() {
        // endRound() non può essere chiamato se il game è finito
        game.end();
        assertThrows(IllegalStateException.class, () -> game.endRound());
    }

    @Test
    void testEndRoundAdvancesToRound2() {
        // dopo endRound il game deve essere al round 2
        round.placeTotemOffer(player1, tileA);
        round.placeTotemOffer(player2, tileB);

        game.endRound();

        assertEquals(2, game.getCurrentRound().getNumber());
    }

    @Test
    void testTwoCompleteRounds() {
        // --- Round 1 ---
        // player1 sceglie tileA → prende 2 carte dalla upper row
        // player2 sceglie tileB → riceve 3 cibo
        round.placeTotemOffer(player1, tileA);
        round.placeTotemOffer(player2, tileB);

        game.endRound();

        assertEquals(2, player1.getTribe().getCharacterCards().size());
        assertEquals(3, player2.getFood());
        assertEquals(2, game.getCurrentRound().getNumber());

        // --- Round 2 ---
        // i ruoli si invertono:
        // player2 sceglie tileA → prende carte
        // player1 sceglie tileB → riceve 3 cibo
        Round round2 = game.getCurrentRound();
        tileA.removeTotem();
        tileB.removeTotem();

        round2.placeTotemOffer(player2, tileA);
        round2.placeTotemOffer(player1, tileB);

        game.endRound();

        // player1 ora ha 3 cibo accumulato dal round 2
        assertEquals(3, player1.getFood());
        // player2 ha almeno 2 carte dal round 2
        assertTrue(player2.getTribe().getCharacterCards().size() >= 2);
        assertEquals(3, game.getCurrentRound().getNumber());
    }

    @Test
    void testEndRoundEndsGameAtMaxRounds() {
        // un game al round 9: endRound lo porta a 10 = MAX_ROUNDS → game over
        Game nearEndGame = new Game(
                List.of(player1, player2),
                9, null, Era.I, null,
                board, new ArrayList<>(tributeDeck),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                GameStatus.IN_PROGRESS
        );

        Round nearEndRound = new Round(9, nearEndGame, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        nearEndGame.setCurrentRound(nearEndRound);

        nearEndGame.endRound();

        assertTrue(nearEndGame.isGameOver());
    }
}
