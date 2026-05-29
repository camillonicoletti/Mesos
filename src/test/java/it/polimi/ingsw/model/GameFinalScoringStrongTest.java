package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.card.ArtistCard;
import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.GathererCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.InventorCard;
import it.polimi.ingsw.model.card.ShamanCard;
import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.card.effect.EndCharacterCountPPEffect;
import it.polimi.ingsw.model.card.effect.EndFullSetPPEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.InventorIcons;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Game final scoring (model).
 *
 * <p>Verifies: end-of-game scoring with mixed character compositions and building effects.
 */
class GameFinalScoringStrongTest {

    /**
     * Verifies that final scoring includes the prestige points already earned during the game,
     * builder end-game points, and printed building points.
     */
    @Test
    void finalScoreIncludesCurrentPrestigeBuilderPointsAndPrintedBuildingPoints() {
        Player alice = createPlayer("Alice", TotemColor.RED);
        Game game = createGame(alice);

        alice.addPrestigePoints(7);
        alice.addCharacterCard(new BuilderCard(1, Era.I, 2, 3, 0));
        alice.addCharacterCard(new BuilderCard(2, Era.I, 2, 4, 1));
        alice.addBuildingCard(new BuildingCard(3, Era.I, 0, new NoEffect(), 8));

        game.calculateFinalScores();

        assertEquals(22, game.getFinalScore(alice),
                "Final score must be 7 current PP + 3 builder PP + 4 builder PP + 8 building PP.");
    }

    /**
     * Verifies artist scoring: every pair of artists gives 10 points.
     * An unpaired artist must not give partial points.
     */
    @Test
    void artistScoringGivesTenPointsPerPairOnly() {
        Player alice = createPlayer("Alice", TotemColor.RED);
        Game game = createGame(alice);

        alice.addCharacterCard(new ArtistCard(1, Era.I, 2));
        alice.addCharacterCard(new ArtistCard(2, Era.I, 2));
        alice.addCharacterCard(new ArtistCard(3, Era.I, 2));

        game.calculateFinalScores();

        assertEquals(10, game.getFinalScore(alice),
                "Three artists contain only one complete pair, so they must give 10 PP total.");
    }

    /**
     * Verifies inventor scoring: points are equal to number of inventors multiplied by
     * the number of distinct invention icons. Duplicate icons increase inventor count
     * but not distinct icon count.
     */
    @Test
    void inventorScoringUsesInventorCountTimesDistinctIcons() {
        Player alice = createPlayer("Alice", TotemColor.RED);
        Game game = createGame(alice);

        alice.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        alice.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_1));
        alice.addCharacterCard(new InventorCard(3, Era.I, 2, InventorIcons.INVENTION_2));
        alice.addCharacterCard(new InventorCard(4, Era.I, 2, InventorIcons.INVENTION_3));

        game.calculateFinalScores();

        assertEquals(12, game.getFinalScore(alice),
                "Four inventors with three distinct icons must give 4 * 3 = 12 PP.");
    }

    /**
     * Verifies end-game building effects based on character count.
     * Printed building points and effect points must both be counted exactly once.
     */
    @Test
    void buildingEndCharacterCountEffectIsAddedOnceTogetherWithPrintedPoints() {
        Player alice = createPlayer("Alice", TotemColor.RED);
        Game game = createGame(alice);

        alice.addCharacterCard(new HunterCard(1, Era.I, 2, false));
        alice.addCharacterCard(new HunterCard(2, Era.I, 2, false));
        alice.addCharacterCard(new HunterCard(3, Era.I, 2, true));

        alice.addBuildingCard(new BuildingCard(
                4,
                Era.I,
                0,
                new EndCharacterCountPPEffect(5, HunterCard.class),
                8
        ));

        game.calculateFinalScores();

        assertEquals(23, game.getFinalScore(alice),
                "Score must be 8 printed building PP + 3 hunters * 5 PP.");
        assertEquals(0, alice.getPrestigePoints(),
                "End-game scoring must not mutate the player's in-game prestige points.");
    }

    /**
     * Verifies full-set building scoring.
     * A full set is one card of each of the six character types.
     * The final score also includes normal artist and inventor end-game scoring.
     */
    @Test
    void fullSetBuildingEffectCountsCompleteCharacterSetsOnly() {
        Player alice = createPlayer("Alice", TotemColor.RED);
        Game game = createGame(alice);

        addOneFullCharacterSet(alice);

        alice.addCharacterCard(new HunterCard(10, Era.I, 2, false));
        alice.addCharacterCard(new ArtistCard(11, Era.I, 2));

        alice.addBuildingCard(new BuildingCard(
                20,
                Era.I,
                0,
                new EndFullSetPPEffect(),
                0
        ));

        game.calculateFinalScores();

        assertEquals(17, game.getFinalScore(alice),
                "Score must be 6 PP from one full set building effect + 10 PP from one artist pair + 1 inventor PP.");
    }

    /**
     * Verifies winner selection by final score.
     * Food must not matter when final scores are different.
     */
    @Test
    void winnerIsPlayerWithHighestFinalScoreBeforeFoodTieBreaker() {
        Player alice = createPlayer("Alice", TotemColor.RED);
        Player bob = createPlayer("Bob", TotemColor.BLUE);
        Game game = createGame(alice, bob);

        alice.addPrestigePoints(20);
        bob.addPrestigePoints(15);
        bob.addFood(99);

        game.calculateFinalScores();

        assertSame(alice, game.getWinner(),
                "Alice has the highest final score, so Bob's higher food must not matter.");
    }

    /**
     * Verifies the food tie-breaker rule.
     * If final scores are tied, the tied player with more food must win.
     */
    @Test
    void foodBreaksTieWhenFinalScoresAreEqual() {
        Player alice = createPlayer("Alice", TotemColor.RED);
        Player bob = createPlayer("Bob", TotemColor.BLUE);
        Game game = createGame(alice, bob);

        alice.addPrestigePoints(30);
        bob.addPrestigePoints(30);
        alice.addFood(2);
        bob.addFood(5);

        game.calculateFinalScores();

        assertEquals(30, game.getFinalScore(alice));
        assertEquals(30, game.getFinalScore(bob));
        assertSame(bob, game.getWinner(),
                "When final scores are tied, the player with more food must win.");
    }

    private void addOneFullCharacterSet(Player player) {
        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new BuilderCard(2, Era.I, 2, 0, 0));
        player.addCharacterCard(new GathererCard(3, Era.I, 2));
        player.addCharacterCard(new HunterCard(4, Era.I, 2, false));
        player.addCharacterCard(new InventorCard(5, Era.I, 2, InventorIcons.INVENTION_1));
        player.addCharacterCard(new ShamanCard(6, Era.I, 2, 1));
    }

    private Player createPlayer(String nickname, TotemColor color) {
        return new Player(nickname, new Totem(color, "OFFER_TRACK"));
    }

    private Game createGame(Player... players) {
        return new Game(
                new ArrayList<>(List.of(players)),
                1,
                null,
                Era.I,
                LocalDateTime.now(),
                (Board) null,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );
    }

    private static class NoEffect implements Effect {
        @Override
        public void apply(Player player) {
            // No effect needed for tests that only verify printed building points.
        }
    }
}