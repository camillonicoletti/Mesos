package it.polimi.ingsw.controller.leaderboard;

import it.polimi.ingsw.network.protocol.clientbound.LeaderboardMessage;
import it.polimi.ingsw.network.protocol.clientbound.LeaderboardRankMessage;
import it.polimi.ingsw.network.protocol.serverbound.GetLeaderboardMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the leaderboard package and its on-the-wire DTOs.
 *
 * <p>Covers three concerns:
 * <ul>
 *   <li>{@link NoOpLeaderboardService}: the graceful-degradation implementation
 *       returned whenever the database is not available. It must never throw
 *       and must return empty results so gameplay is never blocked by a
 *       leaderboard failure.</li>
 *   <li>{@link LeaderboardFactory} fallback paths: missing, empty, incomplete,
 *       or malformed {@code db.properties} must all yield a
 *       {@code NoOpLeaderboardService} rather than propagating exceptions.
 *       Configurations are written to a per-test {@code @TempDir} to keep the
 *       tests hermetic and independent from the working directory.</li>
 *   <li>Protocol DTOs ({@link it.polimi.ingsw.network.protocol.serverbound.GetLeaderboardMessage},
 *       {@link it.polimi.ingsw.network.protocol.clientbound.LeaderboardRankMessage},
 *       {@link it.polimi.ingsw.network.protocol.clientbound.LeaderboardMessage}):
 *       they cross the socket transport, so they must round-trip through Java
 *       serialization preserving every field.</li>
 * </ul>
 *
 * <p>No real MySQL instance is contacted: the "DB unreachable" path is
 * intentionally not exercised here to keep the suite deterministic and
 * independent from local network state.
 */
class LeaderboardServicesTest {

    // ---------- NoOpLeaderboardService ----------

    @Test
    void noOpRecordMatchReturnsEmptyMap() {
        LeaderboardService svc = new NoOpLeaderboardService();
        Map<String, Integer> out = svc.recordMatch(
                UUID.randomUUID(),
                Instant.now(),
                3,
                Map.of("alice", 12, "bob", 7));
        assertNotNull(out);
        assertTrue(out.isEmpty());
    }

    @Test
    void noOpGetTopReturnsEmptyList() {
        LeaderboardService svc = new NoOpLeaderboardService();
        List<LeaderboardEntry> top = svc.getTop(4, 10);
        assertNotNull(top);
        assertTrue(top.isEmpty());
    }

    @Test
    void noOpToleratesNullAndExtremeArguments() {
        LeaderboardService svc = new NoOpLeaderboardService();
        // No-op must never throw, regardless of input.
        assertTrue(svc.recordMatch(null, null, 0, Map.of()).isEmpty());
        assertTrue(svc.getTop(0, 0).isEmpty());
        assertTrue(svc.getTop(-1, Integer.MAX_VALUE).isEmpty());
    }

    // ---------- LeaderboardFactory fallback paths ----------

    @Test
    void factoryFallsBackWhenPropertiesFileMissing(@TempDir Path tmp) {
        Path missing = tmp.resolve("does-not-exist.properties");
        LeaderboardService svc = LeaderboardFactory.create(missing);
        assertInstanceOf(NoOpLeaderboardService.class, svc);
    }

    @Test
    void factoryFallsBackWhenPropertiesIncomplete(@TempDir Path tmp) throws Exception {
        Path p = tmp.resolve("db.properties");
        // Missing db.user and db.password
        Files.writeString(p, "db.url=jdbc:mysql://localhost:3306/whatever\n");
        LeaderboardService svc = LeaderboardFactory.create(p);
        assertInstanceOf(NoOpLeaderboardService.class, svc);
    }

    @Test
    void factoryFallsBackWhenPropertiesEmpty(@TempDir Path tmp) throws Exception {
        Path p = tmp.resolve("db.properties");
        Files.writeString(p, "");
        LeaderboardService svc = LeaderboardFactory.create(p);
        assertInstanceOf(NoOpLeaderboardService.class, svc);
    }

    @Test
    void factoryFallsBackWhenUrlIsMalformed(@TempDir Path tmp) throws Exception {
        Path p = tmp.resolve("db.properties");
        Files.writeString(p,
                "db.url=not-a-valid-jdbc-url\n" +
                "db.user=x\n" +
                "db.password=y\n");
        LeaderboardService svc = LeaderboardFactory.create(p);
        assertInstanceOf(NoOpLeaderboardService.class, svc);
    }

    // ---------- record value semantics ----------

    @Test
    void leaderboardEntryExposesFields() {
        Instant t = Instant.parse("2026-05-30T10:15:30Z");
        LeaderboardEntry e = new LeaderboardEntry(2, "alice", 42, t, 3);
        assertEquals(2, e.rank());
        assertEquals("alice", e.nickname());
        assertEquals(42, e.finalScore());
        assertEquals(t, e.playedAt());
        assertEquals(3, e.playersCount());
        assertEquals(e, new LeaderboardEntry(2, "alice", 42, t, 3));
        assertEquals(e.hashCode(), new LeaderboardEntry(2, "alice", 42, t, 3).hashCode());
    }

    @Test
    void matchResultExposesFields() {
        UUID id = UUID.randomUUID();
        Instant t = Instant.parse("2026-05-30T10:15:30Z");
        MatchResult m = new MatchResult(id, "bob", 17, t, 4);
        assertEquals(id, m.matchId());
        assertEquals("bob", m.nickname());
        assertEquals(17, m.finalScore());
        assertEquals(t, m.playedAt());
        assertEquals(4, m.playersCount());
    }

    // ---------- protocol messages ----------

    @Test
    void getLeaderboardMessageRoundTripsViaSerialization() throws Exception {
        GetLeaderboardMessage msg = new GetLeaderboardMessage(3, 25);
        GetLeaderboardMessage back = roundTrip(msg);
        assertEquals(3, back.playersCount());
        assertEquals(25, back.limit());
        assertEquals(msg, back);
    }

    @Test
    void leaderboardRankMessageRoundTripsViaSerialization() throws Exception {
        LeaderboardRankMessage msg = new LeaderboardRankMessage("alice", 1, 3);
        LeaderboardRankMessage back = roundTrip(msg);
        assertEquals("alice", back.nickname());
        assertEquals(1, back.rank());
        assertEquals(3, back.playersCount());
        assertEquals(msg, back);
    }

    @Test
    void leaderboardMessageRoundTripsViaSerialization() throws Exception {
        Instant t = Instant.parse("2026-05-30T10:15:30Z");
        List<LeaderboardEntry> entries = List.of(
                new LeaderboardEntry(1, "alice", 50, t, 3),
                new LeaderboardEntry(2, "bob",   40, t, 3));
        LeaderboardMessage msg = new LeaderboardMessage(3, entries);
        LeaderboardMessage back = roundTrip(msg);
        assertEquals(3, back.playersCount());
        assertEquals(entries, back.entries());
        assertEquals(msg, back);
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        try (ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray()))) {
            return (T) ois.readObject();
        }
    }
}
