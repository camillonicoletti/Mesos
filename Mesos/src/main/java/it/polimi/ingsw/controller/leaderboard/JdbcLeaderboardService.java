package it.polimi.ingsw.controller.leaderboard;

import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class JdbcLeaderboardService implements LeaderboardService, AutoCloseable {

    private static final String INSERT_SQL = """
            INSERT INTO match_result
                (match_id, nickname, final_score, played_at, players_count)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String RANK_SQL = """
            WITH player_stats AS (
                SELECT nickname,
                       COUNT(*)          AS games_played,
                       MIN(played_at)    AS first_played_at
                FROM   match_result
                WHERE  players_count = ?
                GROUP  BY nickname
            ),
            ranked AS (
                SELECT mr.match_id,
                       mr.nickname,
                       ROW_NUMBER() OVER (
                           ORDER BY mr.final_score    DESC,
                                    ps.games_played   ASC,
                                    ps.first_played_at DESC
                       ) AS rank
                FROM   match_result mr
                JOIN   player_stats ps ON ps.nickname = mr.nickname
                WHERE  mr.players_count = ?
            )
            SELECT nickname, rank
            FROM   ranked
            WHERE  match_id = ?
            """;

    private static final String TOP_SQL = """
            WITH player_stats AS (
                SELECT nickname,
                       COUNT(*)          AS games_played,
                       MIN(played_at)    AS first_played_at
                FROM   match_result
                WHERE  players_count = ?
                GROUP  BY nickname
            )
            SELECT mr.nickname,
                   mr.final_score,
                   mr.played_at,
                   mr.players_count,
                   ROW_NUMBER() OVER (
                       ORDER BY mr.final_score    DESC,
                                ps.games_played   ASC,
                                ps.first_played_at DESC
                   ) AS rank
            FROM   match_result mr
            JOIN   player_stats ps ON ps.nickname = mr.nickname
            WHERE  mr.players_count = ?
            ORDER  BY rank
            LIMIT  ?
            """;

    private final DataSource dataSource;

    public JdbcLeaderboardService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Map<String, Integer> recordMatch(UUID matchId, Instant playedAt,
                                            int playersCount,
                                            Map<String, Integer> finalScores) {
        if (finalScores == null || finalScores.isEmpty()) {
            return Collections.emptyMap();
        }

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                insertRows(conn, matchId, playedAt, playersCount, finalScores);
                Map<String, Integer> ranks = queryRanks(conn, matchId, playersCount);
                conn.commit();
                return ranks;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            System.err.println("[Leaderboard] recordMatch failed: " + ex.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public List<LeaderboardEntry> getTop(int playersCount, int limit) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(TOP_SQL)) {

            ps.setInt(1, playersCount);
            ps.setInt(2, playersCount);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                List<LeaderboardEntry> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new LeaderboardEntry(
                            rs.getInt("rank"),
                            rs.getString("nickname"),
                            rs.getInt("final_score"),
                            rs.getTimestamp("played_at").toInstant(),
                            rs.getInt("players_count")
                    ));
                }
                return out;
            }
        } catch (SQLException ex) {
            System.err.println("[Leaderboard] getTop failed: " + ex.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void close() {
        if (dataSource instanceof HikariDataSource hikari) {
            hikari.close();
        }
    }

    private void insertRows(Connection conn, UUID matchId, Instant playedAt,
                            int playersCount, Map<String, Integer> finalScores)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            Timestamp ts = Timestamp.from(playedAt);
            for (Map.Entry<String, Integer> e : finalScores.entrySet()) {
                ps.setObject(1, matchId);
                ps.setString(2, e.getKey());
                ps.setInt(3, e.getValue());
                ps.setTimestamp(4, ts);
                ps.setInt(5, playersCount);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private Map<String, Integer> queryRanks(Connection conn, UUID matchId,
                                            int playersCount) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(RANK_SQL)) {
            ps.setInt(1, playersCount);
            ps.setInt(2, playersCount);
            ps.setObject(3, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, Integer> out = new HashMap<>();
                while (rs.next()) {
                    out.put(rs.getString("nickname"), rs.getInt("rank"));
                }
                return out;
            }
        }
    }
}