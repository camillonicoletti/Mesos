package it.polimi.ingsw.controller.leaderboard;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Builds a {@link LeaderboardService} from {@code db.properties}, applying
 * the bundled MySQL schema on first connect. Falls back to a no-op service
 * whenever the configuration is missing, malformed, or the database cannot
 * be reached, so the rest of the application can run without a leaderboard.
 */
public final class LeaderboardFactory {

    private static final String DEFAULT_PROPERTIES_PATH = "db.properties";

    private LeaderboardFactory() {}

    /**
     * Builds a {@link LeaderboardService} from {@code db.properties} in the
     * working directory. Falls back to a no-op implementation if the file is
     * missing, malformed, or the database is unreachable.
     */
    public static LeaderboardService createDefault() {
        return create(Path.of(DEFAULT_PROPERTIES_PATH));
    }

    public static LeaderboardService create(Path propertiesPath) {
        Properties props = loadProperties(propertiesPath);
        if (props == null) {
            System.err.println("[Leaderboard] " + propertiesPath
                    + " not found, leaderboard disabled");
            return new NoOpLeaderboardService();
        }

        String url      = props.getProperty("db.url");
        String user     = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        if (url == null || user == null || password == null) {
            System.err.println("[Leaderboard] db.properties is missing required keys, "
                    + "leaderboard disabled");
            return new NoOpLeaderboardService();
        }

        try {
            // smoke-test the connection so a bad config fails fast
            try (Connection probe = DriverManager.getConnection(url, user, password)) {
                initSchema(probe);
            }
            return new JdbcLeaderboardService(url, user, password);
        } catch (Exception ex) {
            System.err.println("[Leaderboard] failed to initialize database: "
                    + ex.getMessage() + " — leaderboard disabled");
            return new NoOpLeaderboardService();
        }
    }

    private static void initSchema(Connection conn) throws SQLException, IOException {
        try (InputStream in = LeaderboardFactory.class
                .getResourceAsStream("/db/schema.sql")) {
            if (in == null) {
                throw new IOException("schema.sql not found on classpath");
            }
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement st = conn.createStatement()) {
                for (String stmt : script.split(";")) {
                    String trimmed = stmt.trim();
                    if (!trimmed.isEmpty()) {
                        st.execute(trimmed);
                    }
                }
            }
        }
    }

    private static Properties loadProperties(Path path) {
        if (!Files.exists(path)) {
            return null;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
            return props;
        } catch (IOException ex) {
            System.err.println("[Leaderboard] failed to read " + path + ": "
                    + ex.getMessage());
            return null;
        }
    }
}