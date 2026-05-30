package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.action.SkipExtraPickAction;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.LoginAckMessage;
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
 * Unit tests for {@link RMIServer}.
 *
 * <p>The server is constructed directly (no real RMI registry) and exercised
 * through its {@link RemoteServer} interface with local {@link RemoteClient}
 * stubs. Each test unexports the server afterwards to avoid leaking the
 * exported object across the test suite.
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
