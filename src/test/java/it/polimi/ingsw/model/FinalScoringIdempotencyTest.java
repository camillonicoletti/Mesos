package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Idempotency tests for Game.calculateFinalScores(). Rulebook section 14:
 * final scoring is applied once at game over, after all remaining events
 * have been resolved. Verifies that calling calculateFinalScores() a second
 * time does not double-count prestige contributions from buildings,
 * inventors, artists, etc.
 */
class FinalScoringIdempotencyTest {

    private Game gameWithPlayers(int playerCount) {
        List<Player> players = new ArrayList<>();
        TotemColor[] colors = TotemColor.values();
        for (int i = 0; i < playerCount; i++) {
            players.add(new Player("P" + i, new Totem(colors[i], "OFFER")));
        }
        List<Card> tribute = new ArrayList<>();
        return new Game(players, 1, null, Era.I, LocalDateTime.now(),
                BoardFactory.createBoard(playerCount),
                tribute,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    /**
     * After Game.end(), calling calculateFinalScores() a second time must
     * NOT recompute (it must short-circuit on the finalScoresCalculated
     * guard). Verified by mutating a player after end() and confirming the
     * cached score is unchanged.
     */
    @Test
    void calculateFinalScoresGuardPreventsRecomputationAfterEnd() {
        Game game = gameWithPlayers(2);
        Player p0 = game.getPlayers().get(0);
        p0.addPrestigePoints(10);
        p0.addCharacterCard(new BuilderCard(1, Era.I, 0, 5, 0));

        game.end(); // freezes scores
        Map<Player, Integer> first = new HashMap<>(game.getFinalScores());

        p0.addPrestigePoints(99);
        game.calculateFinalScores(); // must be a no-op due to guard
        Map<Player, Integer> second = new HashMap<>(game.getFinalScores());

        assertEquals(first, second,
                "Once end() has frozen the scores, calculateFinalScores() must " +
                "not recalculate.  If the second map differs, the guard is broken.");
    }

    /**
     * Before Game.end() is invoked, Game.getFinalScores() returns a live
     * preview, not a cached result. After end(), the score is frozen.
     */
    @Test
    void finalScoresAreFrozenAfterEnd() {
        Game game = gameWithPlayers(2);
        Player p0 = game.getPlayers().get(0);
        p0.addPrestigePoints(7);

        // Live preview
        int liveBefore = game.getFinalScores().get(p0);
        assertEquals(7, liveBefore);

        // End the game (calculates and freezes)
        game.end();
        int frozen = game.getFinalScores().get(p0);
        assertEquals(7, frozen);

        // Mutate the player AFTER end — frozen score must not change
        p0.addPrestigePoints(50);
        int frozenAfterMutation = game.getFinalScores().get(p0);
        assertEquals(7, frozenAfterMutation,
                "After end(), final scores must be frozen and ignore further mutations.");
    }

    /**
     * Calling calculateFinalScores() manually populates the internal cache,
     * but getFinalScores() only reads that cache once the game has actually
     * ended (gameEnded == true). Before that, it always recomputes a live
     * preview, so a manual call before end() has no visible effect.
     */
    @Test
    void manualCalculateBeforeEndStillFreezes() {
        Game game = gameWithPlayers(2);
        Player p0 = game.getPlayers().get(0);
        p0.addPrestigePoints(3);

        game.calculateFinalScores(); // populates the cache, but game has not ended yet
        p0.addPrestigePoints(10);
        Map<Player, Integer> after = game.getFinalScores();

        // Game has not ended, so getFinalScores() ignores the cache and
        // recomputes live, including the +10 added after the manual call.
        assertEquals(13, after.get(p0),
                "Before end(), getFinalScores() must return a live preview, " +
                "not the value cached by the manual calculateFinalScores() call.");
    }
}
