package it.polimi.ingsw.controller.leaderboard;

import java.io.Serializable;
import java.time.Instant;

// One row of a leaderboard query: a player's rank and score in matches with a given playersCount.
public record LeaderboardEntry(
        int rank,
        String nickname,
        int finalScore,
        Instant playedAt,
        int playersCount
) implements Serializable {}