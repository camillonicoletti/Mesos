package it.polimi.ingsw.controller.leaderboard;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

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
        int    poolSize = Integer.parseInt(props.getProperty("db.poolSize", "4"));

        if (url == null || user == null || password == null) {
            System.err.println("[Leaderboard] db.properties is missing required keys, "
                    + "leaderboard disabled");
            return new NoOpLeaderboardService();
        }

        try {
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(url);
            cfg.setUsername(user);
            cfg.setPassword(password);
            cfg.setMaximumPoolSize(poolSize);
            cfg.setPoolName("mesos-leaderboard");

            HikariDataSource ds = new HikariDataSource(cfg);
            // touch the pool so a bad config fails fast instead of at first save
            ds.getConnection().close();
            return new JdbcLeaderboardService(ds);
        } catch (Exception ex) {
            System.err.println("[Leaderboard] failed to initialize datasource: "
                    + ex.getMessage() + " — leaderboard disabled");
            return new NoOpLeaderboardService();
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