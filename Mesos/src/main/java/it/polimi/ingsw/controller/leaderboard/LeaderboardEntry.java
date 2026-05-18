package it.polimi.ingsw.controller.leaderboard;

import java.io.Serializable;
import java.time.Instant;

public record LeaderboardEntry(
        int rank,
        String nickname,
        int finalScore,
        Instant playedAt,
        int playersCount
) implements Serializable {}