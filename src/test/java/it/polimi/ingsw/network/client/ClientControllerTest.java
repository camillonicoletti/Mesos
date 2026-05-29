package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.protocol.clientbound.ErrorMessage;
import it.polimi.ingsw.network.protocol.clientbound.PingMessage;
import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.PongMessage;
import it.polimi.ingsw.network.transport.ClientSideConnection;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Unit tests for ClientController (network.client).
 *
 * <p>Verifies: client-side handling of server messages and user commands.
 */
class ClientControllerTest {

    @Test
    void registeredHandlerAppliesServerMessageToModel() {
        ClientModel model = new ClientModel();
        FakeConnection connection = new FakeConnection();
        ClientController controller = new ClientController(model, connection);

        controller.onMessage(new ErrorMessage("azione non valida"));

        assertEquals("azione non valida", model.getLastError());
    }

    @Test
    void pingStillSendsPongWithoutTouchingModelDispatcher() {
        ClientModel model = new ClientModel();
        FakeConnection connection = new FakeConnection();
        ClientController controller = new ClientController(model, connection);

        controller.onMessage(new PingMessage());

        assertInstanceOf(PongMessage.class, connection.lastSent);
    }

    private static final class FakeConnection implements ClientSideConnection {
        private ClientMessage lastSent;

        @Override
        public void send(ClientMessage message) {
            lastSent = message;
        }

        @Override
        public void close() {
            // Nothing to release in tests.
        }

        @Override
        public void setOnMessage(Consumer<ServerMessage> onMessage) {
            // Tests invoke ClientController.onMessage directly.
        }

        @Override
        public void setOnDisconnect(Runnable onDisconnect) {
            // Tests invoke controller behavior directly.
        }
    }
}
