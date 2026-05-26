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
 * Bug-hunter tests for final scoring idempotency.
 *
 * Brief: "Final scoring must be invoked only at game over, after the final events,
 * and only once."
 *
 * These tests verify the cached behaviour and that calling
 * {@link Game#calculateFinalScores()} twice does not double-count.
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
     * After {@link Game#end()}, calling {@link Game#calculateFinalScores()}
     * a second time must NOT recompute (it must short-circuit on the
     * {@code finalScoresCalculated} guard).  Verified by mutating a player
     * AFTER end() and confirming the cached score is unchanged.
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
     * Before {@link Game#end()} is invoked, {@link Game#getFinalScores()}
     * returns a LIVE preview, not a cached result.  After end(), the score
     * is frozen.
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
     * Before end(), calculateFinalScores() can be called manually.  This
     * still freezes the cache.  This documents the current contract — it
     * may be a footgun but it's the existing behaviour.
     */
    @Test
    void manualCalculateBeforeEndStillFreezes() {
        Game game = gameWithPlayers(2);
        Player p0 = game.getPlayers().get(0);
        p0.addPrestigePoints(3);

        game.calculateFinalScores(); // freezes prematurely
        p0.addPrestigePoints(10);
        Map<Player, Integer> after = game.getFinalScores();

        // getFinalScores() with isGameOver()==false returns a LIVE preview
        // (it always recomputes).  But getFinalScore(player) returns the cached
        // value once frozen and after end().  Since end() hasn't been called,
        // getFinalScores() still returns a fresh preview and INCLUDES the +10.
        assertEquals(13, after.get(p0),
                "Before end(), getFinalScores() returns a live preview. " +
                "If you see 3, then calculateFinalScores() prematurely froze " +
                "the visible result without end() being called.");
    }
}
