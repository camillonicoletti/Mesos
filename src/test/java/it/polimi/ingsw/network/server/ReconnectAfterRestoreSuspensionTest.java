package it.polimi.ingsw.network.server;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.GameSuspendedMessage;
import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReconnectMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.network.transport.ServerSideConnection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for the forced-win countdown delivered on reconnect after a server
 * restart from persistence (resilience + persistence interaction).
 *
 * Scenario reproduced end-to-end through the production restore path: a
 * 2-player game is started, then a fresh LobbyManager is built around the
 * same controller via the restore constructor (as LobbyBootstrap does after
 * a crash). That marks every player disconnected, zero connected, suspended
 * with the countdown not yet running. When the first player reconnects, the
 * connected count goes 0 to 1 and the countdown must be anchored fresh, so
 * the GameSuspendedMessage sent to that client must carry a positive
 * remaining time, not the stale -1 computed while still at zero connected.
 */
class ReconnectAfterRestoreSuspensionTest {

    @Test
    void firstReconnectAfterRestoreReceivesPositiveCountdown() {
        // 1. Start a normal 2-player game.
        LobbyManager original = new LobbyManager();
        GameController controller;
        try {
            RecordingConnection alice = new RecordingConnection();
            RecordingConnection bob = new RecordingConnection();
            original.handleLogin(alice, new LoginMessage("Alice", TotemColor.RED));
            original.handleLogin(bob, new LoginMessage("Bob", TotemColor.BLUE));
            original.handleSetPlayersCount("Alice", new SetPlayersCountMessage(2));
            original.handleReady("Alice", true);
            original.handleReady("Bob", true);
            controller = original.getGameController();
            assertTrue(controller.getGame() != null, "precondition: game must be started");
        } finally {
            original.shutdown();
        }

        // 2. Simulate the server restart: a brand-new LobbyManager wrapping the same
        //    controller. The restore constructor marks all players disconnected, so
        //    the match is suspended with zero connected and no running countdown.
        LobbyManager restored = new LobbyManager(controller);
        try {
            assertTrue(controller.isSuspended(),
                    "restore with all players disconnected must suspend the match");
            assertTrue(controller.getSuspensionRemainingMillis() < 0L,
                    "with 0 connected the countdown must be inactive right after restore");

            long timeout = controller.getSuspensionTimeoutMillis();

            // 3. First player reconnects: count goes 0 → 1.
            RecordingConnection aliceReconnect = new RecordingConnection();
            restored.handleReconnect(aliceReconnect, new ReconnectMessage("Alice"));

            // 4. The client must have received a GameSuspendedMessage, and the LAST one
            //    must carry a positive remaining close to the full timeout — never -1.
            List<GameSuspendedMessage> suspendedMessages = aliceReconnect.messages.stream()
                    .filter(GameSuspendedMessage.class::isInstance)
                    .map(GameSuspendedMessage.class::cast)
                    .toList();
            assertTrue(!suspendedMessages.isEmpty(),
                    "reconnecting client must receive a GameSuspendedMessage");

            GameSuspendedMessage last = suspendedMessages.get(suspendedMessages.size() - 1);
            assertTrue(last.getRemainingMillis() > 0L,
                    "remaining must be positive after the 0 → 1 reconnect, got " + last.getRemainingMillis());
            // Anchored "now": the test runs in milliseconds, so remaining stays near the timeout.
            assertTrue(last.getRemainingMillis() >= timeout - 5_000L,
                    "remaining must be close to the full timeout (" + timeout + "), got "
                            + last.getRemainingMillis());
        } finally {
            restored.shutdown();
        }
    }

    /** Minimal connection stub that records every message the server sends. */
    private static class RecordingConnection implements ServerSideConnection {
        private final List<ServerMessage> messages = new ArrayList<>();
        private String nickname;
        private boolean alive = true;

        @Override
        public void send(ServerMessage message) {
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
