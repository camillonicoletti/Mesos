package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.card.effect.*;
import it.polimi.ingsw.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Game.calculateFinalScores() and getFinalScores().
 *
 * calculateFinalScores() sums:
 *   - base prestige points (accumulated during the game)
 *   - inventor points     (inventors × distinct icons)
 *   - artist points       (pairs of artists × 10)
 *   - builder points      (sum of builder.getPointsEndOfMatch())
 *   - EndGameEffect buildings (via calculatePoints(), not apply())
 */
public class GameScoringTest {

    private Player player1, player2;
    private Game game;

    /** Minimal Game with no board/round needed for scoring tests. */
    private Game buildGame(List<Player> players) {
        return new Game(players, 1, null, Era.I, null, null,
                new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                GameStatus.IN_PROGRESS);
    }

    @BeforeEach
    void setUp() {
        player1 = new Player("Alice", new Totem(TotemColor.RED,  TotemPosition.OFFER_TRACK));
        player2 = new Player("Bob",   new Totem(TotemColor.BLUE, TotemPosition.OFFER_TRACK));
        game = buildGame(List.of(player1, player2));
    }

    // -------------------------------------------------------
    // base prestige points
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
    // inventor points — inventors × distinct icons
    // -------------------------------------------------------

    @Test
    void testFinalScoreInventorPointsTwoInventorsSameIcon() {
        // 2 inventors, 1 distinct icon → 2 * 1 = 2
        player1.addCharacterCard(new InventorCard(1, InventorIcons.INVENTION_1, Era.I, 2));
        player1.addCharacterCard(new InventorCard(2, InventorIcons.INVENTION_1, Era.I, 2));

        assertEquals(2, game.getFinalScores().get(player1));
    }

    @Test
    void testFinalScoreInventorPointsTwoInventorsDifferentIcons() {
        // 2 inventors, 2 distinct icons → 2 * 2 = 4
        player1.addCharacterCard(new InventorCard(1, InventorIcons.INVENTION_1, Era.I, 2));
        player1.addCharacterCard(new InventorCard(2, InventorIcons.INVENTION_2, Era.I, 2));

        assertEquals(4, game.getFinalScores().get(player1));
    }

    // -------------------------------------------------------
    // artist points — pairs × 10
    // -------------------------------------------------------

    @Test
    void testFinalScoreArtistPointsOnePair() {
        // 2 artists → 1 pair → 10 PP
        player1.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player1.addCharacterCard(new ArtistCard(2, Era.I, 2));

        assertEquals(10, game.getFinalScores().get(player1));
    }

    @Test
    void testFinalScoreArtistPointsThreeArtistsOneCompletePair() {
        // 3 artists → 1 pair → 10 PP
        player1.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player1.addCharacterCard(new ArtistCard(2, Era.I, 2));
        player1.addCharacterCard(new ArtistCard(3, Era.I, 2));

        assertEquals(10, game.getFinalScores().get(player1));
    }

    @Test
    void testFinalScoreArtistPointsFourArtistsTwoPairs() {
        // 4 artists → 2 pairs → 20 PP
        player1.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player1.addCharacterCard(new ArtistCard(2, Era.I, 2));
        player1.addCharacterCard(new ArtistCard(3, Era.I, 2));
        player1.addCharacterCard(new ArtistCard(4, Era.I, 2));

        assertEquals(20, game.getFinalScores().get(player1));
    }

    // -------------------------------------------------------
    // builder base points — sum of getPointsEndOfMatch()
    // -------------------------------------------------------

    @Test
    void testFinalScoreBuilderBasePoints() {
        // BuilderCard(id, pointsEndOfMatch, foodDiscount, era, minPlayers)
        // 1 builder with 3 PP end of match → 3
        player1.addCharacterCard(new BuilderCard(1, 3, 0, Era.I, 2));

        assertEquals(3, game.getFinalScores().get(player1));
    }

    @Test
    void testFinalScoreTwoBuildersBasePoints() {
        // 2 builders → 3 + 2 = 5
        player1.addCharacterCard(new BuilderCard(1, 3, 0, Era.I, 2));
        player1.addCharacterCard(new BuilderCard(2, 2, 0, Era.I, 2));

        assertEquals(5, game.getFinalScores().get(player1));
    }

    // -------------------------------------------------------
    // EndPPEffect — +25 PP fissi
    // -------------------------------------------------------

    @Test
    void testFinalScoreWithEndPPEffect() {
        player1.addBuildingCard(new BuildingCard(1, 0, new EndPPEffect(), Era.I, 0, 2));

        assertEquals(25, game.getFinalScores().get(player1));
    }

    @Test
    void testFinalScoreWithEndPPEffectPlusBasePoints() {
        player1.addPrestigePoints(5);
        player1.addBuildingCard(new BuildingCard(1, 0, new EndPPEffect(), Era.I, 0, 2));

        assertEquals(30, game.getFinalScores().get(player1)); // 5 + 25
    }

    // -------------------------------------------------------
    // EndFullSetPPEffect — 6 PP per set completo
    // -------------------------------------------------------

    @Test
    void testFinalScoreWithEndFullSetPPEffectOneSet() {
        player1.addBuildingCard(new BuildingCard(1, 0, new EndFullSetPPEffect(), Era.I, 0, 2));
        player1.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player1.addCharacterCard(new BuilderCard(2, 0, 0, Era.I, 2));
        player1.addCharacterCard(new GathererCard(3, Era.I, 2));
        player1.addCharacterCard(new HunterCard(4, false, Era.I, 2));
        player1.addCharacterCard(new InventorCard(5, InventorIcons.INVENTION_1, Era.I, 2));
        player1.addCharacterCard(new ShamanCard(6, Era.I, 1, 2));

        // 1 set × 6 PP + inventor points (1×1=1) = 7
        assertEquals(7, game.getFinalScores().get(player1));
    }

    @Test
    void testFinalScoreWithEndFullSetPPEffectNoSet() {
        player1.addBuildingCard(new BuildingCard(1, 0, new EndFullSetPPEffect(), Era.I, 0, 2));
        // nessun set completo → 0 dal building
        player1.addCharacterCard(new ArtistCard(1, Era.I, 2));

        assertEquals(0, game.getFinalScores().get(player1));
    }

    // -------------------------------------------------------
    // EndCharacterCountPPEffect — N carte del tipo × pointsPerCard
    // -------------------------------------------------------

    @Test
    void testFinalScoreWithEndCharacterCountPPEffectHunters() {
        // 3 hunter × 2 PP = 6
        player1.addBuildingCard(new BuildingCard(1, 0,
                new EndCharacterCountPPEffect(2, HunterCard.class), Era.I, 0, 2));
        player1.addCharacterCard(new HunterCard(1, false, Era.I, 2));
        player1.addCharacterCard(new HunterCard(2, false, Era.I, 2));
        player1.addCharacterCard(new HunterCard(3, false, Era.I, 2));

        assertEquals(6, game.getFinalScores().get(player1));
    }

    @Test
    void testFinalScoreWithEndCharacterCountPPEffectNoMatchingCards() {
        // building conta hunter, ma il player non ne ha → 0
        player1.addBuildingCard(new BuildingCard(1, 0,
                new EndCharacterCountPPEffect(2, HunterCard.class), Era.I, 0, 2));
        player1.addCharacterCard(new ArtistCard(1, Era.I, 2));

        assertEquals(0, game.getFinalScores().get(player1));
    }

    // -------------------------------------------------------
    // EndBuilderPPEffect — raddoppia i punti dei builder
    // -------------------------------------------------------

    @Test
    void testFinalScoreWithEndBuilderPPEffect() {
        // builder con 3 PP: base (calculateBuilderPoints) = 3
        //                   EndBuilderPPEffect.calculatePoints() = 3
        //                   totale builder = 6
        player1.addBuildingCard(new BuildingCard(1, 0, new EndBuilderPPEffect(), Era.I, 0, 2));
        player1.addCharacterCard(new BuilderCard(2, 3, 0, Era.I, 2));

        assertEquals(6, game.getFinalScores().get(player1));
    }

    // -------------------------------------------------------
    // idempotenza — calculateFinalScores chiamata due volte
    // -------------------------------------------------------

    @Test
    void testCalculateFinalScoresIsIdempotent() {
        player1.addPrestigePoints(10);

        game.calculateFinalScores();
        int firstResult = game.getFinalScores().get(player1);

        // modifica ignorata dopo il lock
        player1.addPrestigePoints(99);
        game.calculateFinalScores();

        assertEquals(firstResult, game.getFinalScores().get(player1));
    }

    // -------------------------------------------------------
    // due giocatori con punteggi diversi
    // -------------------------------------------------------

    @Test
    void testFinalScoresForTwoPlayersAreIndependent() {
        player1.addPrestigePoints(15);
        player1.addBuildingCard(new BuildingCard(1, 0, new EndPPEffect(), Era.I, 0, 2)); // +25

        player2.addPrestigePoints(5);

        Map<Player, Integer> scores = game.getFinalScores();

        assertEquals(40, scores.get(player1)); // 15 + 25
        assertEquals(5,  scores.get(player2));
    }
}
