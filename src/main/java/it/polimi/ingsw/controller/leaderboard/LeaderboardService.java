package it.polimi.ingsw.controller.leaderboard;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Optional persistent leaderboard plugged into the end-of-game flow. Implementations are best-effort:
// if the backing store is unavailable they must degrade gracefully (empty results) instead of throwing,
// so gameplay is never blocked by a leaderboard failure.
public interface LeaderboardService {

    // Returns the 1-based rank of each player in the leaderboard for matches with that playersCount.
    Map<String, Integer> recordMatch(
            UUID matchId,
            Instant playedAt,
            int playersCount,
            Map<String, Integer> finalScores);

    List<LeaderboardEntry> getTop(int playersCount, int limit);
}