package it.polimi.ingsw.network.server;

import it.polimi.ingsw.network.transport.ServerSideConnection;
import it.polimi.ingsw.network.transport.SocketServerConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

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
        connection.setOnMessage(message -> dispatcher.dispatch(connection, message));
        connection.setOnDisconnect(() -> lobby.handleConnectionClosed(connection));
    }
}
