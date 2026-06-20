package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.network.protocol.clientbound.LoginAckMessage;
import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.PongMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReadyMessage;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RMIServerConnection: the server-side adapter that fronts a
 * remote callback object and feeds inbound messages to the dispatcher.
 * No real RMI registry is involved — RemoteClient is a local stub.
 */
class RMIServerConnectionTest {

    @Test
    void constructorRejectsNullClient() {
        assertThrows(IllegalArgumentException.class, () -> new RMIServerConnection(null));
    }

    @Test
    void newConnectionIsAliveWithNoNickname() {
        RMIServerConnection conn = new RMIServerConnection(new CollectingClient());
        try {
            assertTrue(conn.isAlive());
            assertNull(conn.getNickname());
        } finally {
            conn.close();
        }
    }

    @Test
    void sendDeliversMessageToRemoteClient() {
        CollectingClient client = new CollectingClient();
        RMIServerConnection conn = new RMIServerConnection(client);
        try {
            ServerMessage msg = new LoginAckMessage(LoginAckMessage.Outcome.LOGIN_OK, null);
            conn.send(msg);
            assertEquals(1, client.received.size());
            assertSame(msg, client.received.get(0));
        } finally {
            conn.close();
        }
    }

    @Test
    void sendRejectsNullMessage() {
        RMIServerConnection conn = new RMIServerConnection(new CollectingClient());
        try {
            assertThrows(IllegalArgumentException.class, () -> conn.send(null));
        } finally {
            conn.close();
        }
    }

    @Test
    void sendAfterCloseThrows() {
        RMIServerConnection conn = new RMIServerConnection(new CollectingClient());
        conn.close();
        assertFalse(conn.isAlive());
        assertThrows(IllegalStateException.class,
                () -> conn.send(new LoginAckMessage(LoginAckMessage.Outcome.LOGIN_OK, null)));
    }

    @Test
    void sendOnFailingRemoteWrapsAndFiresDisconnectOnce() {
        ThrowingClient client = new ThrowingClient();
        RMIServerConnection conn = new RMIServerConnection(client);
        AtomicInteger disconnects = new AtomicInteger();
        conn.setOnDisconnect(disconnects::incrementAndGet);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> conn.send(new LoginAckMessage(LoginAckMessage.Outcome.LOGIN_OK, null)));
        assertTrue(ex.getCause() instanceof RemoteException);
        assertFalse(conn.isAlive());
        assertEquals(1, disconnects.get(),
                "onDisconnect must fire exactly once when the remote callback breaks");

        // A subsequent send on a dead connection must throw ISE without
        // firing onDisconnect again.
        assertThrows(IllegalStateException.class,
                () -> conn.send(new LoginAckMessage(LoginAckMessage.Outcome.LOGIN_OK, null)));
        assertEquals(1, disconnects.get());
    }

    @Test
    void nicknameGetterAndSetter() {
        RMIServerConnection conn = new RMIServerConnection(new CollectingClient());
        try {
            assertNull(conn.getNickname());
            conn.setNickname("alessandro");
            assertEquals("alessandro", conn.getNickname());
        } finally {
            conn.close();
        }
    }

    @Test
    void receivePongDoesNotDispatchToHandler() {
        RMIServerConnection conn = new RMIServerConnection(new CollectingClient());
        AtomicInteger dispatched = new AtomicInteger();
        conn.setOnMessage(msg -> dispatched.incrementAndGet());
        try {
            conn.receive(new PongMessage());
            assertEquals(0, dispatched.get(),
                    "PongMessage is a heartbeat ack and must not reach the dispatcher");
        } finally {
            conn.close();
        }
    }

    @Test
    void receiveNonPongDispatchesToHandler() {
        RMIServerConnection conn = new RMIServerConnection(new CollectingClient());
        AtomicReference<Object> last = new AtomicReference<>();
        conn.setOnMessage(last::set);
        try {
            ReadyMessage msg = new ReadyMessage(true);
            conn.receive(msg);
            assertSame(msg, last.get());
        } finally {
            conn.close();
        }
    }

    @Test
    void receiveAfterCloseThrows() {
        RMIServerConnection conn = new RMIServerConnection(new CollectingClient());
        conn.close();
        assertThrows(IllegalStateException.class, () -> conn.receive(new ReadyMessage(true)));
    }

    @Test
    void nullOnMessageAndOnDisconnectAreReplacedByDefaults() {
        RMIServerConnection conn = new RMIServerConnection(new CollectingClient());
        try {
            conn.setOnMessage(null);
            conn.setOnDisconnect(null);
            // Default callbacks must be no-ops, not nulls — these calls must not throw.
            assertDoesNotThrow(() -> conn.receive(new ReadyMessage(false)));
        } finally {
            conn.close();
        }
    }

    @Test
    void doubleCloseIsSafe() {
        RMIServerConnection conn = new RMIServerConnection(new CollectingClient());
        conn.close();
        assertDoesNotThrow(conn::close);
        assertFalse(conn.isAlive());
    }

    // ---------- stubs ----------

    private static class CollectingClient implements RemoteClient {
        final List<ServerMessage> received = new ArrayList<>();
        @Override
        public void onServerMessage(ServerMessage message) {
            received.add(message);
        }
    }

    private static class ThrowingClient implements RemoteClient {
        @Override
        public void onServerMessage(ServerMessage message) throws RemoteException {
            throw new RemoteException("simulated remote failure");
        }
    }
}
