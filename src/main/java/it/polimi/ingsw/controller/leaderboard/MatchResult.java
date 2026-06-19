package it.polimi.ingsw.controller.leaderboard;

import java.time.Instant;
import java.util.UUID;

// Test-only: per-player outcome of a finished match (not used by the JDBC leaderboard implementation).
public record MatchResult(
        UUID matchId,
        String nickname,
        int finalScore,
        Instant playedAt,
        int playersCount
) {}