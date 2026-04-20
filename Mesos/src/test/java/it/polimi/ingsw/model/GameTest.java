package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.*;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.enums.Era;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

        tileA = new OfferTile('A', 2, List.of(new ActionSlot("UPPER_ROW", 2)));
        tileB = new OfferTile('B', 2, List.of(new ActionSlot("FOOD", 3)));

        List<TurnSlot> turnSlots = new ArrayList<>();
        turnSlots.add(new TurnSlot(1, new NoSlotEffect()));
        turnSlots.add(new TurnSlot(2, new NoSlotEffect()));

        board = new Board(List.of(tileA, tileB), new TurnOrderTile(2, turnSlots));

        tributeDeck = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            tributeDeck.add(new HunterCard(i, Era.I, 2, false));
        }

        era1Deck = new ArrayList<>(List.of(
                new BuildingCard(2, Era.I, 0, noEffect, 0)
        ));
        era2Deck = new ArrayList<>(List.of(
                new BuildingCard(3, Era.II, 0, noEffect, 0),
                new BuildingCard(4, Era.II, 0, noEffect, 0)
        ));
        era3Deck = new ArrayList<>(List.of(
                new BuildingCard(5, Era.III, 0, noEffect, 0),
                new BuildingCard(6, Era.III, 0, noEffect, 0),
                new BuildingCard(7, Era.III, 0, noEffect, 0)
        ));

        game = new Game(
                List.of(player1, player2),
                1, null, Era.I, null,
                board, tributeDeck,
                era1Deck, era2Deck, era3Deck
        );

        round = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(List.of(
                        new HunterCard(30, Era.I, 2, false),
                        new HunterCard(31, Era.I, 2, false),
                        new HunterCard(32, Era.I, 2, false)
                )),
                new ArrayList<>(List.of(
                        new HunterCard(33, Era.I, 2, false),
                        new HunterCard(34, Era.I, 2, false),
                        new HunterCard(35, Era.I, 2, false),
                        new HunterCard(36, Era.I, 2, false),
                        new HunterCard(37, Era.I, 2, false)
                )),
                new ArrayList<>(), new ArrayList<>()
        );

        game.setCurrentRound(round);
    }

    // =========================================================
    // GETTERS
    // =========================================================

    @Test
    void testGetPlayers() {
        assertEquals(2, game.getPlayers().size());
        assertTrue(game.getPlayers().contains(player1));
        assertTrue(game.getPlayers().contains(player2));
    }

    @Test
    void testGetBoard() {
        assertEquals(board, game.getBoard());
    }

    @Test
    void testGetCurrentEra() {
        assertEquals(Era.I, game.getCurrentEra());
    }

    @Test
    void testGetTributeDeck() {
        assertEquals(tributeDeck, game.getTributeDeck());
    }

    @Test
    void testGetRemainingTributeDeck() {
        assertEquals(30, game.getRemainingTributeDeck());
    }

    @Test
    void testGetRemainingBuildingDeck() {
        assertEquals(6, game.getRemainingBuildingDeck());
    }

    @Test
    void testGetCurrentRoundAndSetCurrentRound() {
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
        assertEquals(era1Deck, game.getBuildingsForEra(Era.I));
        assertEquals(era2Deck, game.getBuildingsForEra(Era.II));
        assertEquals(era3Deck, game.getBuildingsForEra(Era.III));
    }

    // =========================================================
    // START
    // =========================================================

    @Test
    void testStartSetsStatusAndEraAndRound() {
        Game freshGame = new Game(
                List.of(player1, player2),
                0, null, null, null,
                board, new ArrayList<>(tributeDeck),
                new ArrayList<>(era1Deck), new ArrayList<>(era2Deck), new ArrayList<>(era3Deck)
        );

        freshGame.start();

        assertEquals(Era.I, freshGame.getCurrentEra());
        assertNotNull(freshGame.getCurrentRound());
        assertFalse(freshGame.isGameOver());
    }

    // =========================================================
    // END / IS GAME OVER
    // =========================================================

    @Test
    void testIsGameOverFalseAtStart() {
        assertFalse(game.isGameOver());
    }

    @Test
    void testEndSetsGameOver() {
        game.end();
        assertTrue(game.isGameOver());
    }

    @Test
    void testIsGameOverTrueAtMaxRounds() {
        Game maxRoundGame = new Game(
                List.of(player1, player2),
                10, null, Era.I, null,
                board, new ArrayList<>(tributeDeck),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
        );
        Round maxRound = new Round(10, maxRoundGame, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        maxRoundGame.setCurrentRound(maxRound);
        maxRoundGame.nextRound(); // a round 10 (MAX_ROUNDS), nextRound() chiama end()
        assertTrue(maxRoundGame.isGameOver());
    }

    // =========================================================
    // NEXT ERA / END ERA
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
        game.nextEra();
        game.nextEra();
        assertThrows(IllegalStateException.class, () -> game.nextEra());
    }

    @Test
    void testEndEraFromIAdvancesToII() {
        game.endEra();
        assertEquals(Era.II, game.getCurrentEra());
    }

    @Test
    void testEndEraFromIIAdvancesToIII() {
        game.nextEra();
        game.endEra();
        assertEquals(Era.III, game.getCurrentEra());
    }

    @Test
    void testEndEraFromIIIDoesNothing() {
        game.nextEra();
        game.nextEra();
        game.endEra();
        assertEquals(Era.III, game.getCurrentEra());
    }

    // =========================================================
    // NEXT ROUND
    // =========================================================

    @Test
    void testNextRoundIncrementsNumber() {
        int before = game.getCurrentRound().getNumber();
        game.nextRound();
        assertEquals(before + 1, game.getCurrentRound().getNumber());
    }

    @Test
    void testNextRoundCreatesNewRound() {
        Round oldRound = game.getCurrentRound();
        game.nextRound();
        assertNotSame(oldRound, game.getCurrentRound());
    }

    @Test
    void testNextRoundAtMaxRoundsEndsGame() {
        Game maxGame = new Game(
                List.of(player1, player2),
                10, null, Era.I, null,
                board, new ArrayList<>(tributeDeck),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
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
        game.nextEra();

        List<BuildingCard> lower = new ArrayList<>(List.of(
                new BuildingCard(1, Era.I, 0, noEffect, 0)
        ));
        List<BuildingCard> upper = new ArrayList<>(List.of(
                new BuildingCard(2, Era.II, 0, noEffect, 0)
        ));

        Round eraIIRound = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.II,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                lower, upper
        );
        game.setCurrentRound(eraIIRound);

        game.endEra();

        assertEquals(Era.III, game.getCurrentEra());
        assertEquals(1, eraIIRound.getLowerRowBuildings().size());
    }

    @Test
    void testGetBuildingsForEraReturnsEmptyListNotNull() {
        Game emptyGame = new Game(
                List.of(player1, player2),
                1, null, Era.I, null,
                board, new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
        );

        assertNotNull(emptyGame.getBuildingsForEra(Era.I));
        assertNotNull(emptyGame.getBuildingsForEra(Era.II));
        assertNotNull(emptyGame.getBuildingsForEra(Era.III));
        assertTrue(emptyGame.getBuildingsForEra(Era.I).isEmpty());
    }

    @Test
    void testIsGameOverTrueWhenFinishedButRoundLow() {
        game.end();
        assertTrue(game.isGameOver());
    }

    @Test
    void testEndCalledTwiceDoesNotThrow() {
        game.end();
        assertDoesNotThrow(() -> game.end());
        assertTrue(game.isGameOver());
    }

    // =========================================================
    // TAKE CHARACTER CARD
    // =========================================================

    @Test
    void testTakeCharacterCardAddsCardToPlayerTribe() {
        HunterCard card = new HunterCard(99, Era.I, 2, false);
        List<CharacterCard> row = new ArrayList<>(List.of(card));

        game.takeCharacterCard(player1, row, card);

        assertEquals(1, player1.getTribe().getCharacterCards().size());
        assertTrue(player1.getTribe().getCharacterCards().contains(card));
    }

    @Test
    void testTakeCharacterCardRemovesCardFromRow() {
        HunterCard card = new HunterCard(99, Era.I, 2, false);
        List<CharacterCard> row = new ArrayList<>(List.of(card));

        game.takeCharacterCard(player1, row, card);

        assertTrue(row.isEmpty());
    }

    @Test
    void testTakeCharacterCardThrowsWhenCardNotInRow() {
        HunterCard card = new HunterCard(99, Era.I, 2, false);
        List<CharacterCard> emptyRow = new ArrayList<>();

        assertThrows(IllegalArgumentException.class,
                () -> game.takeCharacterCard(player1, emptyRow, card));
    }

    @Test
    void testTakeCharacterCardHunterWithFoodIconGivesFoodEqualToHunterCount() {
        HunterCard hunter = new HunterCard(99, Era.I, 2, true);
        List<CharacterCard> row = new ArrayList<>(List.of(hunter));

        game.takeCharacterCard(player1, row, hunter);

        assertEquals(1, player1.getFood()); // 1 cacciatore in tribù → +1 cibo
    }

    @Test
    void testTakeCharacterCardHunterWithFoodIconCountsAllHuntersInTribe() {
        player1.addCharacterCard(new HunterCard(10, Era.I, 2, false));
        player1.addCharacterCard(new HunterCard(11, Era.I, 2, false));

        HunterCard newHunter = new HunterCard(99, Era.I, 2, true);
        List<CharacterCard> row = new ArrayList<>(List.of(newHunter));

        game.takeCharacterCard(player1, row, newHunter);

        assertEquals(3, player1.getFood()); // 3 cacciatori in tribù → +3 cibo
    }

    @Test
    void testTakeCharacterCardHunterWithoutFoodIconGivesNoFood() {
        HunterCard hunter = new HunterCard(99, Era.I, 2, false);
        List<CharacterCard> row = new ArrayList<>(List.of(hunter));

        game.takeCharacterCard(player1, row, hunter);

        assertEquals(0, player1.getFood());
    }

    // =========================================================
    // TAKE BUILDING CARD
    // =========================================================

    @Test
    void testTakeBuildingCardSpendsCorrectFood() {
        player1.addFood(5);
        BuildingCard building = new BuildingCard(99, Era.I, 3, p -> {}, 0);
        List<BuildingCard> row = new ArrayList<>(List.of(building));

        game.takeBuildingCard(player1, row, building);

        assertEquals(1, player1.getTribe().getBuildingCards().size());
        assertEquals(2, player1.getFood()); // 5 - 3
    }

    @Test
    void testTakeBuildingCardRemovesCardFromRow() {
        player1.addFood(5);
        BuildingCard building = new BuildingCard(99, Era.I, 3, p -> {}, 0);
        List<BuildingCard> row = new ArrayList<>(List.of(building));

        game.takeBuildingCard(player1, row, building);

        assertTrue(row.isEmpty());
    }

    @Test
    void testTakeBuildingCardThrowsWhenNotAffordable() {
        BuildingCard building = new BuildingCard(99, Era.I, 3, p -> {}, 0);
        List<BuildingCard> row = new ArrayList<>(List.of(building));

        assertThrows(IllegalStateException.class,
                () -> game.takeBuildingCard(player1, row, building));
    }

    @Test
    void testTakeBuildingCardAppliesBuilderDiscount() {
        player1.addFood(3);
        player1.addCharacterCard(new BuilderCard(10, Era.I, 2, 0, 2)); // sconto 2
        BuildingCard building = new BuildingCard(99, Era.I, 3, p -> {}, 0);
        List<BuildingCard> row = new ArrayList<>(List.of(building));

        game.takeBuildingCard(player1, row, building);

        assertEquals(2, player1.getFood()); // 3 - (3 - 2) = 2
    }

    @Test
    void testTakeBuildingCardDiscountCannotMakeCostNegative() {
        player1.addFood(3);
        player1.addCharacterCard(new BuilderCard(10, Era.I, 2, 0, 5)); // sconto 5 > costo
        BuildingCard building = new BuildingCard(99, Era.I, 2, p -> {}, 0);
        List<BuildingCard> row = new ArrayList<>(List.of(building));

        game.takeBuildingCard(player1, row, building); // costo finale = max(0, 2-5) = 0

        assertEquals(3, player1.getFood()); // non paga nulla
    }

    @Test
    void testTakeBuildingCardThrowsWhenCardNotInRow() {
        player1.addFood(5);
        BuildingCard building = new BuildingCard(99, Era.I, 3, p -> {}, 0);
        List<BuildingCard> emptyRow = new ArrayList<>();

        assertThrows(IllegalArgumentException.class,
                () -> game.takeBuildingCard(player1, emptyRow, building));
    }

    // =========================================================
    // DRAW TRIBE CARD
    // =========================================================

    @Test
    void testDrawTribeCardRemovesCardFromDeck() {
        int before = game.getRemainingTributeDeck();
        game.drawTribeCard();
        assertEquals(before - 1, game.getRemainingTributeDeck());
    }

    @Test
    void testDrawTribeCardReturnsNullWhenDeckIsEmpty() {
        game.getTributeDeck().clear();
        assertNull(game.drawTribeCard());
    }
}