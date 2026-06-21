package it.polimi.ingsw.network.server;

import it.polimi.ingsw.network.transport.ServerSideConnection;
import it.polimi.ingsw.network.transport.SocketServerConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Blocking accept loop for socket clients.
 * Each accepted socket is wrapped in a ServerSideConnection and wired to the
 * same lobby/dispatcher pipeline used by RMI.
 */
final class SocketAcceptLoop {

    private SocketAcceptLoop() {
    }

    static void runBlocking(int port,
                            LobbyManager lobby,
                            MessageDispatcher dispatcher,
                            String startedMessage,
                            String connectionMessage) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(startedMessage + port);

            while (true) {
                // Accept waits here until a new TCP client connects.
                Socket socket = serverSocket.accept();
                ServerSideConnection connection = new SocketServerConnection(socket);
                wireConnection(connection, lobby, dispatcher);
                lobby.registerPendingConnection(connection);
                System.out.println(connectionMessage + socket.getRemoteSocketAddress());
            }
        }
    }

    private static void wireConnection(ServerSideConnection connection,
                                       LobbyManager lobby,
                                       MessageDispatcher dispatcher) {
        // Incoming messages go to the dispatcher; disconnections go back to the lobby.
        connection.setOnMessage(message -> dispatcher.dispatch(connection, message));
        connection.setOnDisconnect(() -> lobby.handleConnectionClosed(connection));
    }
}
