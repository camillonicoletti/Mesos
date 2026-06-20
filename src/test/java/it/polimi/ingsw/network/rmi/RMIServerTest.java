package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.action.SkipExtraPickAction;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.DummyServerMessage;
import it.polimi.ingsw.network.protocol.clientbound.LoginAckMessage;
import it.polimi.ingsw.network.protocol.clientbound.PingMessage;
import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReconnectMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.network.server.LobbyManager;
import it.polimi.ingsw.network.transport.ServerSideConnection;
import org.junit.jupiter.api.Test;

import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RMIServer. The server is constructed directly (no real RMI
 * registry) and exercised through its RemoteServer interface with local
 * RemoteClient stubs. Each test unexports the server afterwards to avoid
 * leaking the exported object across the test suite.
 */
class RMIServerTest {

    @Test
    void constructorRejectsNullLobbyManager() {
        assertThrows(IllegalArgumentException.class, () -> new RMIServer(null));
    }

    @Test
    void registerRejectsNullClient() throws Exception {
        RMIServer server = new RMIServer(new LobbyManager());
        try {
            assertThrows(IllegalArgumentException.class, () -> server.register(null));
        } finally {
            UnicastRemoteObject.unexportObject(server, true);
        }
    }

    @Test
    void pingIsNoOp() throws Exception {
        RMIServer server = new RMIServer(new LobbyManager());
        try {
            assertDoesNotThrow(server::ping);
        } finally {
            UnicastRemoteObject.unexportObject(server, true);
        }
    }

    @Test
    void disconnectNullClientIsNoOp() throws Exception {
        RMIServer server = new RMIServer(new LobbyManager());
        try {
            assertDoesNotThrow(() -> server.disconnect(null));
        } finally {
            UnicastRemoteObject.unexportObject(server, true);
        }
    }

    @Test
    void disconnectUnregisteredClientIsNoOp() throws Exception {
        RMIServer server = new RMIServer(new LobbyManager());
        try {
            CollectingClient stranger = new CollectingClient();
            assertDoesNotThrow(() -> server.disconnect(stranger));
        } finally {
            UnicastRemoteObject.unexportObject(server, true);
        }
    }

    @Test
    void registerThenDisconnectUnregistersPendingConnection() throws Exception {
        RMIServer server = new RMIServer(new LobbyManager());
        try {
            CollectingClient client = new CollectingClient();
            server.register(client);
            // Must not throw — exercises the "no nickname yet" branch.
            assertDoesNotThrow(() -> server.disconnect(client));
            // Second disconnect is a no-op (connection already removed).
            assertDoesNotThrow(() -> server.disconnect(client));
        } finally {
            UnicastRemoteObject.unexportObject(server, true);
        }
    }

    @Test
    void pongOnRegisteredClientDoesNotThrow() throws Exception {
        RMIServer server = new RMIServer(new LobbyManager());
        try {
            CollectingClient client = new CollectingClient();
            server.register(client);
            assertDoesNotThrow(() -> server.pong(client));
        } finally {
            UnicastRemoteObject.unexportObject(server, true);
        }
    }

    @Test
    void loginRoutesThroughDispatcherAndCallsBackClient() throws Exception {
        RMIServer server = new RMIServer(new LobbyManager());
        try {
            CollectingClient client = new CollectingClient();
            server.register(client);
            server.login(client, "RmiServerTest", TotemColor.RED);

            assertTrue(client.awaitMessage(2, TimeUnit.SECONDS),
                    "Server should push at least one clientbound message after login");
            boolean sawLoginAck = client.snapshot().stream()
                    .anyMatch(m -> m instanceof LoginAckMessage);
            assertTrue(sawLoginAck, "Expected a LoginAckMessage in the callback stream");
        } finally {
            UnicastRemoteObject.unexportObject(server, true);
        }
    }

    @Test
    void registerIsIdempotentForSameClient() throws Exception {
        RMIServer server = new RMIServer(new LobbyManager());
        try {
            CollectingClient client = new CollectingClient();
            server.register(client);
            // Re-registering the same client must not throw nor create a
            // second pending connection that would later leak.
            assertDoesNotThrow(() -> server.register(client));
            assertDoesNotThrow(() -> server.disconnect(client));
        } finally {
            UnicastRemoteObject.unexportObject(server, true);
        }
    }

    // ---------- per-method delegation to the dispatcher ----------
    // These tests cover the remaining RemoteServer methods (reconnect,
    // setPlayersCount, setReady, performAction). A RecordingLobbyManager
    // captures the call the dispatcher routes to LobbyManager so we can
    // assert on the actual payload, not just on the absence of exceptions.

    @Test
    void reconnectForwardsNicknameToLobbyManager() throws Exception {
        RecordingLobbyManager lobby = new RecordingLobbyManager();
        RMIServer server = new RMIServer(lobby);
        try {
            CollectingClient client = new CollectingClient();
            server.register(client);
            server.reconnect(client, "alessandro");

            assertEquals("alessandro", lobby.lastReconnectNickname.get(),
                    "Dispatcher must invoke handleReconnect with the nickname carried by ReconnectMessage");
        } finally {
            UnicastRemoteObject.unexportObject(server, true);
        }
    }

    @Test
    void setPlayersCountForwardsCountToLobbyManager() throws Exception {
        RecordingLobbyManager lobby = new RecordingLobbyManager();
        RMIServer server = new RMIServer(lobby);
        try {
            CollectingClient client = new CollectingClient();
            server.register(client);
            server.setPlayersCount(client, 4);

            assertEquals(Integer.valueOf(4), lobby.lastPlayersCount.get(),
                    "Dispatcher must invoke handleSetPlayersCount with the count carried by SetPlayersCountMessage");
        } finally {
            UnicastRemoteObject.unexportObject(server, true);
        }
    }

    @Test
    void setReadyForwardsFlagToLobbyManager() throws Exception {
        RecordingLobbyManager lobby = new RecordingLobbyManager();
        RMIServer server = new RMIServer(lobby);
        try {
            CollectingClient client = new CollectingClient();
            server.register(client);
            server.setReady(client, true);

            assertEquals(Boolean.TRUE, lobby.lastReadyFlag.get(),
                    "Dispatcher must invoke handleReady with the boolean carried by ReadyMessage");

            // Toggle to verify the flag is not hard-coded by the adapter.
            server.setReady(client, false);
            assertEquals(Boolean.FALSE, lobby.lastReadyFlag.get());
        } finally {
            UnicastRemoteObject.unexportObject(server, true);
        }
    }

    @Test
    void performActionForwardsActionInstanceToLobbyManager() throws Exception {
        RecordingLobbyManager lobby = new RecordingLobbyManager();
        RMIServer server = new RMIServer(lobby);
        try {
            CollectingClient client = new CollectingClient();
            server.register(client);

            GameAction action = new SkipExtraPickAction();
            server.performAction(client, action);

            assertSame(action, lobby.lastAction.get(),
                    "Dispatcher must invoke handleGameAction with the exact GameAction wrapped in GameActionMessage");
        } finally {
            UnicastRemoteObject.unexportObject(server, true);
        }
    }

    @Test
    void forceEndGameForwardsToLobbyManager() throws Exception {
        RecordingLobbyManager lobby = new RecordingLobbyManager();
        RMIServer server = new RMIServer(lobby);
        try {
            CollectingClient client = new CollectingClient();
            server.register(client);

            server.forceEndGame(client);

            assertTrue(lobby.forceEndGameCalled.get(),
                    "RMI forceEndGame must reach LobbyManager.handleForceEndGame");
        } finally {
            UnicastRemoteObject.unexportObject(server, true);
        }
    }

    @Test
    void pongCanReturnWhileAnotherRmiCallIsDispatching() throws Exception {
        DeadlockProbeLobbyManager lobby = new DeadlockProbeLobbyManager();
        AtomicReference<RMIServer> serverRef = new AtomicReference<>();
        PongDuringCallbackClient client = new PongDuringCallbackClient(serverRef);
        RMIServer server = new RMIServer(lobby);
        serverRef.set(server);

        CountDownLatch pingReturned = new CountDownLatch(1);
        CountDownLatch actionReturned = new CountDownLatch(1);
        Thread pingThread = null;
        Thread actionThread = null;

        try {
            server.register(client);
            ServerSideConnection connection = lobby.connection.get();
            assertNotNull(connection, "Registration should create the RMI server-side connection");

            pingThread = new Thread(() -> {
                connection.send(new PingMessage());
                pingReturned.countDown();
            }, "rmi-deadlock-test-ping");
            pingThread.start();

            assertTrue(client.awaitPingCallback(1, TimeUnit.SECONDS),
                    "The test must enter the client callback before starting the action");

            actionThread = new Thread(() -> {
                try {
                    server.performAction(client, new SkipExtraPickAction());
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                } finally {
                    actionReturned.countDown();
                }
            }, "rmi-deadlock-test-action");
            actionThread.start();

            assertTrue(lobby.awaitSendAttempt(1, TimeUnit.SECONDS),
                    "The action should try to send a callback while the ping callback is still open");

            client.releasePong();

            assertTrue(pingReturned.await(1, TimeUnit.SECONDS),
                    "Pong must not wait for the whole action dispatch to release the RMIServer monitor");
            assertTrue(actionReturned.await(1, TimeUnit.SECONDS),
                    "After pong returns, the action callback can acquire the connection send lock and complete");
        } finally {
            client.releasePong();
            if (lobby.connection.get() != null) {
                lobby.connection.get().close();
            }
            if (pingThread != null) {
                pingThread.join(100);
            }
            if (actionThread != null) {
                actionThread.join(100);
            }
            UnicastRemoteObject.unexportObject(server, true);
        }
    }

    // ---------- stubs ----------

    /**
     * LobbyManager subclass that records what the dispatcher routes to its
     * handle* entry points, without running any real game logic. Lets the
     * tests assert on the payload that crossed the RMIServer → dispatcher
     * boundary, not just on the absence of exceptions.
     */
    private static class RecordingLobbyManager extends LobbyManager {
        final AtomicReference<String> lastReconnectNickname = new AtomicReference<>();
        final AtomicReference<Integer> lastPlayersCount = new AtomicReference<>();
        final AtomicReference<Boolean> lastReadyFlag = new AtomicReference<>();
        final AtomicReference<GameAction> lastAction = new AtomicReference<>();
        final java.util.concurrent.atomic.AtomicBoolean forceEndGameCalled =
                new java.util.concurrent.atomic.AtomicBoolean(false);

        @Override
        public synchronized void handleReconnect(ServerSideConnection connection, ReconnectMessage message) {
            lastReconnectNickname.set(message.getNickname());
        }

        @Override
        public synchronized void handleSetPlayersCount(String nickname, SetPlayersCountMessage message) {
            lastPlayersCount.set(message.getCount());
        }

        @Override
        public synchronized void handleReady(String nickname, boolean ready) {
            lastReadyFlag.set(ready);
        }

        @Override
        public synchronized void handleGameAction(String nickname, GameAction action) {
            lastAction.set(action);
        }

        @Override
        public synchronized void handleForceEndGame() {
            forceEndGameCalled.set(true);
        }
    }

    private static class DeadlockProbeLobbyManager extends LobbyManager {
        final AtomicReference<ServerSideConnection> connection = new AtomicReference<>();
        private final CountDownLatch sendAttempt = new CountDownLatch(1);

        @Override
        public synchronized void registerPendingConnection(ServerSideConnection connection) {
            this.connection.set(connection);
        }

        @Override
        public synchronized void handleGameAction(String nickname, GameAction action) {
            sendAttempt.countDown();
            connection.get().send(new DummyServerMessage("action callback"));
        }

        boolean awaitSendAttempt(long timeout, TimeUnit unit) throws InterruptedException {
            return sendAttempt.await(timeout, unit);
        }
    }

    private static class PongDuringCallbackClient implements RemoteClient {
        private final AtomicReference<RMIServer> server;
        private final CountDownLatch pingCallbackStarted = new CountDownLatch(1);
        private final CountDownLatch releasePong = new CountDownLatch(1);

        PongDuringCallbackClient(AtomicReference<RMIServer> server) {
            this.server = server;
        }

        @Override
        public void onServerMessage(ServerMessage message) {
            if (!(message instanceof PingMessage)) {
                return;
            }

            pingCallbackStarted.countDown();
            try {
                releasePong.await(1, TimeUnit.SECONDS);
                server.get().pong(this);
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        }

        boolean awaitPingCallback(long timeout, TimeUnit unit) throws InterruptedException {
            return pingCallbackStarted.await(timeout, unit);
        }

        void releasePong() {
            releasePong.countDown();
        }
    }



    /**
     * Local RemoteClient: callbacks are plain Java calls (no RMI marshalling),
     * which is what we want to inspect deterministically from the test.
     */
    private static class CollectingClient implements RemoteClient {
        private final List<ServerMessage> received = new ArrayList<>();
        private final CountDownLatch firstMessage = new CountDownLatch(1);

        @Override
        public synchronized void onServerMessage(ServerMessage message) {
            received.add(message);
            firstMessage.countDown();
        }

        synchronized List<ServerMessage> snapshot() {
            return new ArrayList<>(received);
        }

        boolean awaitMessage(long timeout, TimeUnit unit) throws InterruptedException {
            return firstMessage.await(timeout, unit);
        }
    }
}
