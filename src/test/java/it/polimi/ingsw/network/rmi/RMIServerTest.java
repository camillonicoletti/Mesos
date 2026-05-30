package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.LoginAckMessage;
import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.server.LobbyManager;
import org.junit.jupiter.api.Test;

import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    // ---------- stub ----------

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
