package it.polimi.ingsw.model;

import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bug hunter tests for final scoring lifecycle.
 */
class FinalScoringBugHunterTest {

    /**
     * Reading the winner before the game is actually over must not freeze final scores.
     * If scores are cached too early, later prestige changes are ignored at game end.
     */
    @Test
    void prematureWinnerReadMustNotFreezeFinalScores() {
        Player alice = new Player("Alice", new Totem(TotemColor.RED, "TURN_ORDER"));
        Player bob = new Player("Bob", new Totem(TotemColor.BLUE, "TURN_ORDER"));

        alice.addPrestigePoints(10);
        bob.addPrestigePoints(5);

        Game game = createMinimalGame(alice, bob);

        assertEquals(alice, game.getWinner());

        bob.addPrestigePoints(20);

        game.end();

        assertEquals(25, game.getFinalScore(bob));
        assertEquals(bob, game.getWinner());
    }

    /**
     * Reading a player's final score before the end must not freeze the final score cache.
     */
    @Test
    void prematureFinalScoreReadMustNotFreezeFinalScores() {
        Player alice = new Player("Alice", new Totem(TotemColor.RED, "TURN_ORDER"));
        Player bob = new Player("Bob", new Totem(TotemColor.BLUE, "TURN_ORDER"));

        alice.addPrestigePoints(7);
        bob.addPrestigePoints(3);

        Game game = createMinimalGame(alice, bob);

        assertEquals(3, game.getFinalScore(bob));

        bob.addPrestigePoints(10);

        game.end();

        assertEquals(13, game.getFinalScore(bob));
    }

    private Game createMinimalGame(Player... players) {
        return new Game(
                new ArrayList<>(List.of(players)),
                1,
                null,
                Era.I,
                LocalDateTime.now(),
                null,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );
    }
}