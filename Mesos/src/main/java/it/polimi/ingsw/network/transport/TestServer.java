package it.polimi.ingsw.network.transport;

import it.polimi.ingsw.network.protocol.clientbound.DummyServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.DummyClientMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Manual smoke test for the server-side socket transport.
 */
public final class TestServer {

    private TestServer() {
    }

    /**
     * Accepts one client and replies to a dummy message.
     *
     * @param args ignored
     * @throws IOException if the server socket cannot be opened
     */
    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("TestServer listening on port 12345");
            Socket socket = serverSocket.accept();
            SocketServerConnection connection = new SocketServerConnection(socket);
            connection.setOnMessage(message -> {
                if (message instanceof DummyClientMessage dummyMessage) {
                    System.out.println("Server received: " + dummyMessage.getPayload());
                    connection.send(new DummyServerMessage("ACK: " + dummyMessage.getPayload()));
                } else {
                    System.out.println("Server received message: " + message.getClass().getSimpleName());
                }
            });
            connection.setOnDisconnect(() -> System.out.println("Client disconnected"));

            while (connection.isAlive()) {
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    connection.close();
                }
            }
        }
    }
}
