package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.PongMessage;
import it.polimi.ingsw.network.transport.ClientSideConnection;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Targeted tests for the involuntary server-disconnection routing introduced
 * alongside the dedicated "server disconnected" GUI overlay.
 *
 * <p>The model exposes two distinct entry points for connection loss:
 * {@link ClientModel#applyConnectionLost(String)} for voluntary
 * {@code quit}/{@code disconnect}, and
 * {@link ClientModel#applyServerDisconnected(String)} for involuntary drops
 * (socket failure, heartbeat timeout, remote exception). These tests pin down
 * which path each {@link ClientController} entry uses, since mixing the two
 * would make the GUI show the wrong overlay on a user-initiated quit.</p>
 */
class ServerDisconnectedRoutingTest {

    /**
     * Voluntary path must not set the server-disconnected flag, otherwise a
     * normal {@code quit} would surface the "server disconnected" overlay.
     */
    @Test
    void applyConnectionLostLeavesServerDisconnectedFlagFalse() {
        ClientModel model = new ClientModel();

        model.applyConnectionLost("You left the match.");

        assertFalse(model.isConnected(), "voluntary disconnect must clear connected");
        assertFalse(model.isServerDisconnected(),
                "voluntary disconnect must NOT set serverDisconnected");
        assertEquals("You left the match.", model.getLastError());
    }

    /**
     * Involuntary path must set both flags and record the reason so the GUI
     * has a deterministic signal for switching overlays.
     */
    @Test
    void applyServerDisconnectedSetsBothFlagsAndRecordsReason() {
        ClientModel model = new ClientModel();

        model.applyServerDisconnected("Connection to server lost");

        assertFalse(model.isConnected());
        assertTrue(model.isServerDisconnected());
        assertEquals("Connection to server lost", model.getLastError());
    }

    /**
     * The flag is terminal: there is no client-side reconnect, so a later
     * {@code applyConnectionLost} (e.g. the user clicking quit from the
     * server-disconnected overlay) must not clear it.
     */
    @Test
    void serverDisconnectedFlagIsTerminal() {
        ClientModel model = new ClientModel();
        model.applyServerDisconnected("drop");

        model.applyConnectionLost("You left the match.");

        assertTrue(model.isServerDisconnected(),
                "serverDisconnected must remain true once set, even after a later voluntary applyConnectionLost");
    }

    /**
     * On the application send path, a transport-level RuntimeException must
     * route through {@code applyServerDisconnected} and rethrow so existing
     * callers' error handling stays unchanged.
     */
    @Test
    void controllerSendFailureRoutesThroughServerDisconnected() {
        ClientModel model = new ClientModel();
        ThrowingConnection connection = new ThrowingConnection();
        ClientController controller = new ClientController(model, connection);

        assertThrows(RuntimeException.class, () -> controller.send(new PongMessage()));

        assertTrue(model.isServerDisconnected(),
                "send() failure must mark the model as involuntarily server-disconnected");
        assertFalse(model.isConnected());
    }

    /**
     * The transport-layer {@code onDisconnect} callback is wired in the
     * controller's constructor; firing it must mark the drop as involuntary.
     */
    @Test
    void transportOnDisconnectCallbackRoutesThroughServerDisconnected() {
        ClientModel model = new ClientModel();
        CapturingConnection connection = new CapturingConnection();
        new ClientController(model, connection);

        assertNotNull(connection.capturedOnDisconnect,
                "controller must register an onDisconnect callback on construction");
        connection.capturedOnDisconnect.run();

        assertTrue(model.isServerDisconnected());
        assertFalse(model.isConnected());
    }

    /**
     * Fresh model invariant: nothing has happened yet, so neither flag should
     * be tripped. Guards against accidental default-value regressions.
     */
    @Test
    void freshModelHasNeitherFlagSet() {
        ClientModel model = new ClientModel();

        assertTrue(model.isConnected());
        assertFalse(model.isServerDisconnected());
    }

    /** Connection stub whose {@code send} always throws, simulating a dead socket. */
    private static final class ThrowingConnection implements ClientSideConnection {
        @Override
        public void send(ClientMessage message) {
            throw new RuntimeException("socket closed");
        }

        @Override
        public void close() {
            // no-op for tests
        }

        @Override
        public void setOnMessage(Consumer<ServerMessage> onMessage) {
            // no-op: tests do not exercise inbound delivery
        }

        @Override
        public void setOnDisconnect(Runnable onDisconnect) {
            // no-op: tests do not exercise the callback path here
        }
    }

    /** Connection stub that captures the {@code onDisconnect} callback so the test can fire it. */
    private static final class CapturingConnection implements ClientSideConnection {
        private Runnable capturedOnDisconnect;

        @Override
        public void send(ClientMessage message) {
            // no-op
        }

        @Override
        public void close() {
            // no-op
        }

        @Override
        public void setOnMessage(Consumer<ServerMessage> onMessage) {
            // no-op
        }

        @Override
        public void setOnDisconnect(Runnable onDisconnect) {
            this.capturedOnDisconnect = onDisconnect;
        }
    }
}
