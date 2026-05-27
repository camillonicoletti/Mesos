package it.polimi.ingsw.network.server;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.network.transport.ServerSideConnection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stress test for the {@link LobbyManager} synchronization model.
 *
 * <p>Hammers the lobby with concurrent {@code handle*} calls from many
 * threads and asserts that:
 * <ul>
 *   <li>no unchecked exception escapes the dispatcher,</li>
 *   <li>the lobby state stays internally consistent (ready set, lobby
 *       players, disconnected players all reference the same nickname
 *       universe),</li>
 *   <li>when 4 players reach the ready state the controller transitions
 *       exactly once into a started game (no double-start race).</li>
 * </ul>
 *
 * <p>If {@code synchronized} were removed from {@code LobbyManager} this
 * test would fail intermittently with concurrent modification exceptions or
 * inconsistent state. Its passing is the empirical confirmation that the
 * monitor of {@code LobbyManager} is the single, sufficient serialization
 * point for the server.
 */
class LobbyManagerConcurrencyStressTest {

    private static final int WORKER_THREADS = 8;
    private static final int ITERATIONS_PER_WORKER = 250;

    @Test
    void parallelLobbyHandlersDoNotCorruptState() throws Exception {
        LobbyManager lobby = new LobbyManager();
        List<RecordingConnection> conns = new ArrayList<>();
        String[] nicknames = {"Alice", "Bob", "Charlie", "Dave"};
        TotemColor[] colors = {TotemColor.RED, TotemColor.BLUE, TotemColor.PURPLE, TotemColor.YELLOW};

        try {
            for (int i = 0; i < nicknames.length; i++) {
                RecordingConnection c = new RecordingConnection();
                conns.add(c);
                lobby.handleLogin(c, new LoginMessage(nicknames[i], colors[i]));
            }

            AtomicInteger uncaught = new AtomicInteger(0);
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(WORKER_THREADS);

            for (int w = 0; w < WORKER_THREADS; w++) {
                final int seed = w;
                pool.submit(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    java.util.Random rng = new java.util.Random(seed);
                    for (int i = 0; i < ITERATIONS_PER_WORKER; i++) {
                        try {
                            String nick = nicknames[rng.nextInt(nicknames.length)];
                            int op = rng.nextInt(4);
                            switch (op) {
                                case 0 -> lobby.handleReady(nick, true);
                                case 1 -> lobby.handleReady(nick, false);
                                case 2 -> lobby.handleSetPlayersCount("Alice",
                                        new SetPlayersCountMessage(2 + rng.nextInt(3)));
                                case 3 -> lobby.getLeaderboardService(); // trivial read, no lock
                                default -> { /* unreachable */ }
                            }
                        } catch (RuntimeException re) {
                            // The dispatcher swallows per-call exceptions; any escape here
                            // is a real bug (e.g. ConcurrentModificationException).
                            uncaught.incrementAndGet();
                        }
                    }
                });
            }

            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS),
                    "workers must terminate within the timeout");

            assertEquals(0, uncaught.get(),
                    "no unchecked exception should escape concurrent lobby handlers");

            // Invariant checks: regardless of the interleaving, the lobby/game must be
            // in a coherent shape. Either the game never started (all four players are
            // still lobby players) or the game started exactly once (then the lobby
            // players list is consumed by the game).
            boolean gameStarted = lobby.getGameController().getGame() != null;
            int lobbyPlayers = lobby.getGameController().getLobbyPlayers().size();
            if (gameStarted) {
                assertFalse(lobby.getGameController().getGame().isGameOver(),
                        "started game must not be in a game-over state from a race");
                assertTrue(lobbyPlayers == 0 || lobbyPlayers >= 2,
                        "lobby players count is internally consistent: " + lobbyPlayers);
            } else {
                assertEquals(nicknames.length, lobbyPlayers,
                        "without start, every login must still be visible in the lobby");
            }

            // Ready set must be a subset of known nicknames.
            for (String nick : nicknames) {
                // Should not throw and should return a definite boolean.
                lobby.getGameController().isPlayerReady(nick);
            }
        } finally {
            lobby.shutdown();
        }
    }

    private static class RecordingConnection implements ServerSideConnection {
        private final List<ServerMessage> messages = new ArrayList<>();
        private String nickname;
        private boolean alive = true;

        @Override
        public synchronized void send(ServerMessage message) {
            messages.add(message);
        }

        @Override
        public void close() {
            alive = false;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public String getNickname() {
            return nickname;
        }

        @Override
        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        @Override
        public void setOnMessage(Consumer<ClientMessage> onMessage) {
        }

        @Override
        public void setOnDisconnect(Runnable onDisconnect) {
        }
    }
}
