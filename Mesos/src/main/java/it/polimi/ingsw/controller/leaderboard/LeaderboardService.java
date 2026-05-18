package it.polimi.ingsw.controller.leaderboard;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface LeaderboardService {

    /**
     * Persists one row per player and returns the rank (1-based) of each player
     * in the leaderboard for matches with that {@code playersCount}.
     * Implementations must be best-effort: never throw, return empty map on error.
     */
    Map<String, Integer> recordMatch(
            UUID matchId,
            Instant playedAt,
            int playersCount,
            Map<String, Integer> finalScores);

    /**
     * Returns the top-N leaderboard for matches with the given player count.
     * Returns an empty list on error.
     */
    List<LeaderboardEntry> getTop(int playersCount, int limit);
}