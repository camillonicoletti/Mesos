package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.ActionSlot;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.FoodSlot;
import it.polimi.ingsw.model.board.NoSlotEffect;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.board.TurnOrderTile;
import it.polimi.ingsw.model.board.TurnSlot;
import it.polimi.ingsw.model.board.UpperRowSlot;
import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.RoundPhase;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Game aggregate (model).
 *
 * <p>Verifies: round lifecycle, turn order, refill and final scoring orchestration in the model layer.
 */
public class GameTest {

    private Game game;
    private Round round;
    private Player player1;
    private Player player2;
    private OfferTile tileA;
    private OfferTile tileB;
    private Board board;
    private List<Card> tributeDeck;
    private List<BuildingCard> era1Deck;
    private List<BuildingCard> era2Deck;
    private List<BuildingCard> era3Deck;

    private final Effect noEffect = player -> {};

    @BeforeEach
    void setUp() {
        player1 = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player2 = new Player("Bob", new Totem(TotemColor.BLUE, "OFFER_TRACK"));

        tileA = new OfferTile('A', 2, List.of(new ActionSlot(new UpperRowSlot(), 2)));
        tileB = new OfferTile('B', 2, List.of(new ActionSlot(new FoodSlot(), 3)));

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
                1,
                null,
                Era.I,
                null,
                board,
                tributeDeck,
                era1Deck,
                era2Deck,
                era3Deck
        );

        round = new Round(
                1,
                game,
                RoundPhase.TOTEM_PLACEMENT,
                Era.I,
                new ArrayList<>(),
                new ArrayList<>(),
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
                new ArrayList<>(),
                new ArrayList<>()
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

        Round newRound = new Round(
                2,
                game,
                RoundPhase.TOTEM_PLACEMENT,
                Era.I,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
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
                0,
                null,
                null,
                null,
                board,
                new ArrayList<>(tributeDeck),
                new ArrayList<>(era1Deck),
                new ArrayList<>(era2Deck),
                new ArrayList<>(era3Deck)
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
                10,
                null,
                Era.I,
                null,
                board,
                new ArrayList<>(tributeDeck),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );

        Round maxRound = new Round(
                10,
                maxRoundGame,
                RoundPhase.TOTEM_PLACEMENT,
                Era.I,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );

        maxRoundGame.setCurrentRound(maxRound);
        maxRoundGame.nextRound();

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

    @Test
    void testEndEraFromIIDiscardsLowerAndMovesUpperBuildings() {
        game.nextEra();

        List<BuildingCard> lower = new ArrayList<>(List.of(
                new BuildingCard(1, Era.I, 0, noEffect, 0)
        ));
        List<BuildingCard> upper = new ArrayList<>(List.of(
                new BuildingCard(2, Era.II, 0, noEffect, 0)
        ));

        Round eraIIRound = new Round(
                1,
                game,
                RoundPhase.TOTEM_PLACEMENT,
                Era.II,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                lower,
                upper
        );

        game.setCurrentRound(eraIIRound);
        game.endEra();

        assertEquals(Era.III, game.getCurrentEra());
        assertEquals(1, eraIIRound.getLowerRowBuildings().size());
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
                10,
                null,
                Era.I,
                null,
                board,
                new ArrayList<>(tributeDeck),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );

        Round maxRound = new Round(
                10,
                maxGame,
                RoundPhase.TOTEM_PLACEMENT,
                Era.I,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );

        maxGame.setCurrentRound(maxRound);
        maxGame.nextRound();

        assertTrue(maxGame.isGameOver());
    }

    // =========================================================
    // MISC
    // =========================================================

    @Test
    void testGetBuildingsForEraReturnsEmptyListNotNull() {
        Game emptyGame = new Game(
                List.of(player1, player2),
                1,
                null,
                Era.I,
                null,
                board,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
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
        List<CharacterCard> row = replaceLowerRowWith(card);

        game.takeCharacterCard(player1, row, card);

        assertEquals(1, player1.getTribe().getCharacterCards().size());
        assertTrue(player1.getTribe().getCharacterCards().contains(card));
    }

    @Test
    void testTakeCharacterCardRemovesCardFromRow() {
        HunterCard card = new HunterCard(99, Era.I, 2, false);
        List<CharacterCard> row = replaceLowerRowWith(card);

        game.takeCharacterCard(player1, row, card);

        assertFalse(round.getLowerRowTribute().contains(card));
        assertEquals(0, round.getLowerRowTribute().size());
    }

    @Test
    void testTakeCharacterCardThrowsWhenCardNotInRow() {
        HunterCard card = new HunterCard(99, Era.I, 2, false);
        List<CharacterCard> emptyRow = emptyLowerRow();

        assertThrows(IllegalArgumentException.class,
                () -> game.takeCharacterCard(player1, emptyRow, card));
    }

    @Test
    void testTakeCharacterCardHunterWithFoodIconGivesFoodEqualToHunterCount() {
        HunterCard hunter = new HunterCard(99, Era.I, 2, true);
        List<CharacterCard> row = replaceLowerRowWith(hunter);

        game.takeCharacterCard(player1, row, hunter);

        assertEquals(1, player1.getFood());
    }

    @Test
    void testTakeCharacterCardHunterWithFoodIconCountsAllHuntersInTribe() {
        player1.addCharacterCard(new HunterCard(10, Era.I, 2, false));
        player1.addCharacterCard(new HunterCard(11, Era.I, 2, false));

        HunterCard newHunter = new HunterCard(99, Era.I, 2, true);
        List<CharacterCard> row = replaceLowerRowWith(newHunter);

        game.takeCharacterCard(player1, row, newHunter);

        assertEquals(3, player1.getFood());
    }

    @Test
    void testTakeCharacterCardHunterWithoutFoodIconGivesNoFood() {
        HunterCard hunter = new HunterCard(99, Era.I, 2, false);
        List<CharacterCard> row = replaceLowerRowWith(hunter);

        game.takeCharacterCard(player1, row, hunter);

        assertEquals(0, player1.getFood());
    }

    // =========================================================
    // TAKE BUILDING CARD
    // =========================================================

    @Test
    void testTakeBuildingCardSpendsCorrectFood() {
        player1.addFood(5);
        BuildingCard building = new BuildingCard(99, Era.I, 3, noEffect, 0);
        List<BuildingCard> row = replaceUpperBuildingsWith(building);

        game.takeBuildingCard(player1, row, building);

        assertEquals(1, player1.getTribe().getBuildingCards().size());
        assertEquals(2, player1.getFood());
    }

    @Test
    void testTakeBuildingCardRemovesCardFromRow() {
        player1.addFood(5);
        BuildingCard building = new BuildingCard(99, Era.I, 3, noEffect, 0);
        List<BuildingCard> row = replaceUpperBuildingsWith(building);

        game.takeBuildingCard(player1, row, building);

        assertFalse(round.getUpperRowBuildings().contains(building));
        assertEquals(0, round.getUpperRowBuildings().size());
    }

    @Test
    void testTakeBuildingCardThrowsWhenNotAffordable() {
        BuildingCard building = new BuildingCard(99, Era.I, 3, noEffect, 0);
        List<BuildingCard> row = replaceUpperBuildingsWith(building);

        assertThrows(IllegalStateException.class,
                () -> game.takeBuildingCard(player1, row, building));
    }

    @Test
    void testTakeBuildingCardAppliesBuilderDiscount() {
        player1.addFood(3);
        player1.addCharacterCard(new BuilderCard(10, Era.I, 2, 0, 2));

        BuildingCard building = new BuildingCard(99, Era.I, 3, noEffect, 0);
        List<BuildingCard> row = replaceUpperBuildingsWith(building);

        game.takeBuildingCard(player1, row, building);

        assertEquals(2, player1.getFood());
    }

    @Test
    void testTakeBuildingCardDiscountCannotMakeCostNegative() {
        player1.addFood(3);
        player1.addCharacterCard(new BuilderCard(10, Era.I, 2, 0, 5));

        BuildingCard building = new BuildingCard(99, Era.I, 2, noEffect, 0);
        List<BuildingCard> row = replaceUpperBuildingsWith(building);

        game.takeBuildingCard(player1, row, building);

        assertEquals(3, player1.getFood());
    }

    @Test
    void testTakeBuildingCardThrowsWhenCardNotInRow() {
        player1.addFood(5);
        BuildingCard building = new BuildingCard(99, Era.I, 3, noEffect, 0);
        List<BuildingCard> emptyRow = emptyUpperBuildings();

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
        // Drain the deck via the public API rather than mutating the internal list directly.
        while (game.drawTribeCard() != null) {
            // intentionally empty
        }

        assertNull(game.drawTribeCard());
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private List<CharacterCard> replaceLowerRowWith(CharacterCard card) {
        round.mutableLowerRowTribute().clear();
        round.mutableLowerRowTribute().add(card);
        return round.getLowerRowTribute();
    }

    private List<CharacterCard> emptyLowerRow() {
        round.mutableLowerRowTribute().clear();
        return round.getLowerRowTribute();
    }

    private List<BuildingCard> replaceUpperBuildingsWith(BuildingCard building) {
        round.mutableUpperRowBuildings().clear();
        round.mutableUpperRowBuildings().add(building);
        return round.getUpperRowBuildings();
    }

    private List<BuildingCard> emptyUpperBuildings() {
        round.mutableUpperRowBuildings().clear();
        return round.getUpperRowBuildings();
    }
}