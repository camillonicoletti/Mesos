package it.polimi.ingsw.controller.leaderboard;

import java.time.Instant;
import java.util.UUID;

public record MatchResult(
        UUID matchId,
        String nickname,
        int finalScore,
        Instant playedAt,
        int playersCount
) {}