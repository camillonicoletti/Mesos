package it.polimi.ingsw.controller.leaderboard;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Used when the DB is not configured or unreachable.
public final class NoOpLeaderboardService implements LeaderboardService {

    @Override
    public Map<String, Integer> recordMatch(UUID matchId, Instant playedAt,
                                            int playersCount,
                                            Map<String, Integer> finalScores) {
        return Collections.emptyMap();
    }

    @Override
    public List<LeaderboardEntry> getTop(int playersCount, int limit) {
        return Collections.emptyList();
    }
}