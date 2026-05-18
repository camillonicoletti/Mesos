package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.leaderboard.LeaderboardEntry;
import it.polimi.ingsw.controller.leaderboard.LeaderboardFactory;
import it.polimi.ingsw.controller.leaderboard.LeaderboardService;
import it.polimi.ingsw.controller.leaderboard.NoOpLeaderboardService;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LeaderboardSmokeTest {

    public static void main(String[] args) {
        System.out.println("Working dir: " + Path.of("").toAbsolutePath());

        LeaderboardService service = LeaderboardFactory.createDefault();

        if (service instanceof NoOpLeaderboardService) {
            System.err.println("FAIL: factory returned NoOp. "
                    + "Check db.properties location and content.");
            System.exit(1);
        }

        System.out.println("[1/3] Factory built a real JdbcLeaderboardService - OK");

        UUID matchId = UUID.randomUUID();
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("Ale", 42);
        scores.put("Bob", 35);
        scores.put("Cleo", 50);

        Map<String, Integer> ranks = service.recordMatch(
                matchId, Instant.now(), 3, scores);

        if (ranks.isEmpty()) {
            System.err.println("FAIL: recordMatch returned empty ranks map.");
            System.exit(1);
        }

        System.out.println("[2/3] recordMatch ranks: " + ranks);
        System.out.println("       Expected: Cleo=1, Ale=2, Bob=3");

        List<LeaderboardEntry> top = service.getTop(3, 10);
        System.out.println("[3/3] Top-10 for playersCount=3:");
        for (LeaderboardEntry e : top) {
            System.out.printf("       #%d  %-10s  score=%d  on=%s%n",
                    e.rank(), e.nickname(), e.finalScore(), e.playedAt());
        }

        System.out.println("\nAll checks passed.");
    }
}