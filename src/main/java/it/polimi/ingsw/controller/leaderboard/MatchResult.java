package it.polimi.ingsw.controller.leaderboard;

import java.time.Instant;
import java.util.UUID;

/**
 * Per-player outcome of a finished match, used as the unit of insertion into
 * the leaderboard store. One {@code MatchResult} per player per match.
 */
public record MatchResult(
        UUID matchId,
        String nickname,
        int finalScore,
        Instant playedAt,
        int playersCount
) {}