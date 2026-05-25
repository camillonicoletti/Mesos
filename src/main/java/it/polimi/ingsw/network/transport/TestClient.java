package it.polimi.ingsw.network.transport;

import it.polimi.ingsw.network.protocol.clientbound.DummyServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.DummyClientMessage;

import java.io.IOException;
import java.net.Socket;

/**
 * Manual smoke test for the client-side socket transport.
 */
public final class TestClient {

    private TestClient() {
    }

    /**
     * Connects to the local test server, sends one dummy message and prints the reply.
     *
     * @param args ignored
     * @throws IOException if the socket connection cannot be opened
     */
    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("127.0.0.1", 12345)) {
            SocketClientConnection connection = new SocketClientConnection(socket);
            connection.setOnMessage(message -> {
                if (message instanceof DummyServerMessage dummyMessage) {
                    System.out.println("Client received: " + dummyMessage.getPayload());
                } else {
                    System.out.println("Client received message: " + message.getClass().getSimpleName());
                }
            });
            connection.setOnDisconnect(() -> System.out.println("Server disconnected"));

            connection.send(new DummyClientMessage("hello from client"));

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                connection.close();
            }
        }
    }
}
