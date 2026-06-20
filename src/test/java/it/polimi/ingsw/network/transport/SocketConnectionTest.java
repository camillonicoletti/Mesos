package it.polimi.ingsw.network.transport;

import it.polimi.ingsw.network.protocol.clientbound.DummyServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.DummyClientMessage;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for Socket transport (network.transport).
 *
 * Verifies: round-trip message exchange between SocketClientConnection and SocketServerConnection over a real loopback socket.
 */
class SocketConnectionTest {

    @Test
    void clientAndServerExchangeSerializedMessages() throws Exception {
        CountDownLatch accepted = new CountDownLatch(1);
        CountDownLatch echoed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<SocketServerConnection> serverConnection = new AtomicReference<>();

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            var executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Socket acceptedSocket = serverSocket.accept();
                    SocketServerConnection connection = new SocketServerConnection(acceptedSocket);
                    serverConnection.set(connection);
                    connection.setOnMessage(message -> {
                        if (message instanceof DummyClientMessage dummy
                                && "client-to-server".equals(dummy.getPayload())) {
                            connection.send(new DummyServerMessage("server-to-client"));
                        }
                    });
                    accepted.countDown();
                } catch (Throwable throwable) {
                    failure.set(throwable);
                    accepted.countDown();
                }
            });

            SocketClientConnection client = new SocketClientConnection(
                    new Socket("127.0.0.1", serverSocket.getLocalPort())
            );
            client.setOnMessage(message -> {
                if (message instanceof DummyServerMessage dummy
                        && "server-to-client".equals(dummy.getPayload())) {
                    echoed.countDown();
                }
            });

            assertTrue(accepted.await(2, TimeUnit.SECONDS));
            if (failure.get() != null) {
                fail(failure.get());
            }

            client.send(new DummyClientMessage("client-to-server"));

            assertTrue(echoed.await(2, TimeUnit.SECONDS));
            client.close();
            SocketServerConnection connection = serverConnection.get();
            if (connection != null) {
                connection.close();
            }
            executor.shutdownNow();
        }
    }
}
