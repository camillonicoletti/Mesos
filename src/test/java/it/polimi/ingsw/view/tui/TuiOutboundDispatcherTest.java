package it.polimi.ingsw.view.tui;

import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.GameActionMessage;
import it.polimi.ingsw.network.transport.ClientSideConnection;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TuiOutboundDispatcherTest {

    @Test
    void tuiDoesNotBlockCommandParsingWhileRmiSendIsInProgress() throws Exception {
        BlockingConnection connection = new BlockingConnection();
        ClientController controller = new ClientController(new ClientModel(), connection);
        new Tui(controller, new ClientModel());

        try {
            assertTimeoutPreemptively(Duration.ofMillis(250),
                    () -> controller.pickCharacterCard("101", true));

            assertTrue(connection.sendStarted.await(1, TimeUnit.SECONDS));
        } finally {
            connection.releaseSend.countDown();
        }

        assertTrue(connection.sendFinished.await(1, TimeUnit.SECONDS));
        assertInstanceOf(GameActionMessage.class, connection.lastSent.get());
    }

    private static final class BlockingConnection implements ClientSideConnection {
        private final CountDownLatch sendStarted = new CountDownLatch(1);
        private final CountDownLatch releaseSend = new CountDownLatch(1);
        private final CountDownLatch sendFinished = new CountDownLatch(1);
        private final AtomicReference<ClientMessage> lastSent = new AtomicReference<>();

        @Override
        public void send(ClientMessage message) {
            lastSent.set(message);
            sendStarted.countDown();
            try {
                releaseSend.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                sendFinished.countDown();
            }
        }

        @Override
        public void close() {
        }

        @Override
        public void setOnMessage(Consumer<ServerMessage> onMessage) {
        }

        @Override
        public void setOnDisconnect(Runnable onDisconnect) {
        }
    }
}
