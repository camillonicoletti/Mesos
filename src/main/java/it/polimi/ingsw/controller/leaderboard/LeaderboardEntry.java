package it.polimi.ingsw.controller.leaderboard;

import java.io.Serializable;
import java.time.Instant;

/**
 * Immutable row of a leaderboard query: a player's rank in matches with the
 * given {@code playersCount}, with their best score and when it was achieved.
 */
public record LeaderboardEntry(
        int rank,
        String nickname,
        int finalScore,
        Instant playedAt,
        int playersCount
) implements Serializable {}