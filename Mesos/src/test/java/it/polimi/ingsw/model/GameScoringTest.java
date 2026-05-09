package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.ArtistCard;
import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.GathererCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.InventorCard;
import it.polimi.ingsw.model.card.ShamanCard;
import it.polimi.ingsw.model.card.effect.EndBuilderPPEffect;
import it.polimi.ingsw.model.card.effect.EndCharacterCountPPEffect;
import it.polimi.ingsw.model.card.effect.EndFullSetPPEffect;
import it.polimi.ingsw.model.card.effect.EndPPEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.InventorIcons;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Game final score calculation and winner selection.
 *
 * Final score includes:
 * - base prestige points accumulated during the game;
 * - inventor points: inventors multiplied by distinct invention icons;
 * - artist points: pairs of artists multiplied by 10;
 * - builder points: sum of builder end-game points;
 * - building printed end-game points;
 * - building EndGameEffect points.
 */
public class GameScoringTest {

    private Player player1;
    private Player player2;
    private Game game;

    /**
     * Builds a minimal game for scoring tests.
     * No board or round is needed because these tests only inspect players' tribes and scores.
     */
    private Game buildGame(List<Player> players) {
        return new Game(
                players,
                1,
                null,
                Era.I,
                null,
                null,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );
    }

    @BeforeEach
    void setUp() {
        player1 = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player2 = new Player("Bob", new Totem(TotemColor.BLUE, "OFFER_TRACK"));
        game = buildGame(List.of(player1, player2));
    }

    // -------------------------------------------------------
    // Base prestige points
    // -------------------------------------------------------

    @Test
    void testFinalScoreIncludesBasePrestigePoints() {
        player1.addPrestigePoints(10);

        Map<Player, Integer> scores = game.getFinalScores();

        assertEquals(10, scores.get(player1));
    }

    @Test
    void testFinalScoreWithNoCardsAndNoBuildingsIsZero() {
        Map<Player, Integer> scores = game.getFinalScores();

        assertEquals(0, scores.get(player1));
        assertEquals(0, scores.get(player2));
    }

    // -------------------------------------------------------
    // Inventor points: inventors × distinct icons
    // -------------------------------------------------------

    @Test
    void testFinalScoreInventorPointsTwoInventorsSameIcon() {
        player1.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        player1.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_1));

        assertEquals(2, game.getFinalScores().get(player1));
    }

    @Test
    void testFinalScoreInventorPointsTwoInventorsDifferentIcons() {
        player1.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        player1.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_2));

        assertEquals(4, game.getFinalScores().get(player1));
    }

    // -------------------------------------------------------
    // Artist points: pairs × 10
    // -------------------------------------------------------

    @Test
    void testFinalScoreArtistPointsOnePair() {
        player1.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player1.addCharacterCard(new ArtistCard(2, Era.I, 2));

        assertEquals(10, game.getFinalScores().get(player1));
    }

    @Test
    void testFinalScoreArtistPointsThreeArtistsOneCompletePair() {
        player1.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player1.addCharacterCard(new ArtistCard(2, Era.I, 2));
        player1.addCharacterCard(new ArtistCard(3, Era.I, 2));

        assertEquals(10, game.getFinalScores().get(player1));
    }

    @Test
    void testFinalScoreArtistPointsFourArtistsTwoPairs() {
        player1.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player1.addCharacterCard(new ArtistCard(2, Era.I, 2));
        player1.addCharacterCard(new ArtistCard(3, Era.I, 2));
        player1.addCharacterCard(new ArtistCard(4, Era.I, 2));

        assertEquals(20, game.getFinalScores().get(player1));
    }

    // -------------------------------------------------------
    // Builder base points
    // -------------------------------------------------------

    @Test
    void testFinalScoreBuilderBasePoints() {
        player1.addCharacterCard(new BuilderCard(1, Era.I, 2, 3, 0));

        assertEquals(3, game.getFinalScores().get(player1));
    }

    @Test
    void testFinalScoreTwoBuildersBasePoints() {
        player1.addCharacterCard(new BuilderCard(1, Era.I, 2, 3, 0));
        player1.addCharacterCard(new BuilderCard(2, Era.I, 2, 2, 0));

        assertEquals(5, game.getFinalScores().get(player1));
    }

    // -------------------------------------------------------
    // Building printed end-game points
    // -------------------------------------------------------

    @Test
    void testFinalScoreIncludesBuildingPrintedEndPoints() {
        player1.addBuildingCard(new BuildingCard(1, Era.I, 0, null, 8));

        assertEquals(8, game.getFinalScores().get(player1));
    }

    @Test
    void testFinalScoreIncludesMultipleBuildingPrintedEndPoints() {
        player1.addBuildingCard(new BuildingCard(1, Era.I, 0, null, 8));
        player1.addBuildingCard(new BuildingCard(2, Era.I, 0, null, 2));

        assertEquals(10, game.getFinalScores().get(player1));
    }

    // -------------------------------------------------------
    // EndPPEffect: +25 PP
    // -------------------------------------------------------

    @Test
    void testFinalScoreWithEndPPEffect() {
        player1.addBuildingCard(new BuildingCard(1, Era.I, 0, new EndPPEffect(), 0));

        assertEquals(25, game.getFinalScores().get(player1));
    }

    @Test
    void testFinalScoreWithEndPPEffectPlusBasePoints() {
        player1.addPrestigePoints(5);
        player1.addBuildingCard(new BuildingCard(1, Era.I, 0, new EndPPEffect(), 0));

        assertEquals(30, game.getFinalScores().get(player1));
    }

    // -------------------------------------------------------
    // EndFullSetPPEffect: 6 PP per full set
    // -------------------------------------------------------

    @Test
    void testFinalScoreWithEndFullSetPPEffectOneSet() {
        player1.addBuildingCard(new BuildingCard(1, Era.I, 0, new EndFullSetPPEffect(), 0));
        player1.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player1.addCharacterCard(new BuilderCard(2, Era.I, 2, 0, 0));
        player1.addCharacterCard(new GathererCard(3, Era.I, 2));
        player1.addCharacterCard(new HunterCard(4, Era.I, 2, false));
        player1.addCharacterCard(new InventorCard(5, Era.I, 2, InventorIcons.INVENTION_1));
        player1.addCharacterCard(new ShamanCard(6, Era.I, 1, 2));

        assertEquals(7, game.getFinalScores().get(player1));
    }

    @Test
    void testFinalScoreWithEndFullSetPPEffectNoSet() {
        player1.addBuildingCard(new BuildingCard(1, Era.I, 0, new EndFullSetPPEffect(), 0));
        player1.addCharacterCard(new ArtistCard(1, Era.I, 2));

        assertEquals(0, game.getFinalScores().get(player1));
    }

    // -------------------------------------------------------
    // EndCharacterCountPPEffect: matching cards × points per card
    // -------------------------------------------------------

    @Test
    void testFinalScoreWithEndCharacterCountPPEffectHunters() {
        player1.addBuildingCard(new BuildingCard(
                1,
                Era.I,
                0,
                new EndCharacterCountPPEffect(2, HunterCard.class),
                0
        ));
        player1.addCharacterCard(new HunterCard(1, Era.I, 2, false));
        player1.addCharacterCard(new HunterCard(2, Era.I, 2, false));
        player1.addCharacterCard(new HunterCard(3, Era.I, 2, false));

        assertEquals(6, game.getFinalScores().get(player1));
    }

    @Test
    void testFinalScoreWithEndCharacterCountPPEffectNoMatchingCards() {
        player1.addBuildingCard(new BuildingCard(
                1,
                Era.I,
                0,
                new EndCharacterCountPPEffect(2, HunterCard.class),
                0
        ));
        player1.addCharacterCard(new ArtistCard(1, Era.I, 2));

        assertEquals(0, game.getFinalScores().get(player1));
    }

    // -------------------------------------------------------
    // EndBuilderPPEffect: doubles builder end-game points
    // -------------------------------------------------------

    @Test
    void testFinalScoreWithEndBuilderPPEffect() {
        player1.addBuildingCard(new BuildingCard(1, Era.I, 0, new EndBuilderPPEffect(), 0));
        player1.addCharacterCard(new BuilderCard(2, Era.I, 2, 3, 0));

        assertEquals(6, game.getFinalScores().get(player1));
    }

    // -------------------------------------------------------
    // Final score cache lifecycle
    // -------------------------------------------------------

    @Test
    void testFinalScoresAreLiveBeforeGameEnd() {
        player1.addPrestigePoints(10);

        assertEquals(10, game.getFinalScores().get(player1));

        player1.addPrestigePoints(99);

        assertEquals(109, game.getFinalScores().get(player1));
    }

    @Test
    void testCalculateFinalScoresIsIdempotentAfterGameEnd() {
        player1.addPrestigePoints(10);

        game.end();
        int firstResult = game.getFinalScores().get(player1);

        player1.addPrestigePoints(99);
        game.calculateFinalScores();

        assertEquals(firstResult, game.getFinalScores().get(player1));
    }

    // -------------------------------------------------------
    // Multiple players
    // -------------------------------------------------------

    @Test
    void testFinalScoresForTwoPlayersAreIndependent() {
        player1.addPrestigePoints(15);
        player1.addBuildingCard(new BuildingCard(1, Era.I, 0, new EndPPEffect(), 0));

        player2.addPrestigePoints(5);

        Map<Player, Integer> scores = game.getFinalScores();

        assertEquals(40, scores.get(player1));
        assertEquals(5, scores.get(player2));
    }

    // -------------------------------------------------------
    // getWinner / getFinalScore
    // -------------------------------------------------------

    @Test
    void testGetWinnerReturnsPlayerWithHighestScore() {
        player1.addPrestigePoints(20);
        player2.addPrestigePoints(10);

        assertEquals(player1, game.getWinner());
    }

    @Test
    void testGetWinnerReturnsNotNullEvenWithTiedScores() {
        player1.addPrestigePoints(10);
        player2.addPrestigePoints(10);

        Player winner = game.getWinner();

        assertNotNull(winner);
        assertTrue(winner == player1 || winner == player2);
    }

    @Test
    void testGetFinalScoreForSinglePlayer() {
        player1.addPrestigePoints(7);

        assertEquals(7, game.getFinalScore(player1));
    }

    @Test
    void testGetWinnerWithOnlyBuildingEffects() {
        player1.addBuildingCard(new BuildingCard(1, Era.I, 0, new EndPPEffect(), 0));

        assertEquals(player1, game.getWinner());
    }

    @Test
    void testWinnerUsesFoodAsTieBreaker() {
        player1.addPrestigePoints(10);
        player2.addPrestigePoints(10);
        player2.addFood(1);

        assertEquals(player2, game.getWinner());
    }
}