package it.polimi.ingsw.network.server;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.Persistence;
import it.polimi.ingsw.network.transport.ServerSideConnection;
import it.polimi.ingsw.network.transport.SocketServerConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

public class Server {
    public static void main(String[] args) throws IOException {
        boolean freshStart = false;
        int port = 1234;

        for (String arg : args) {
            if ("--fresh".equalsIgnoreCase(arg)) {
                freshStart = true;
            } else {
                port = Integer.parseInt(arg);
            }
        }

        if (freshStart) {
            Persistence.clear();
        }

        Optional<GameController> restored = freshStart ? Optional.empty() : Persistence.load();
        LobbyManager lobby = restored
                .map(LobbyManager::new)
                .orElseGet(LobbyManager::new);

        if (restored.isPresent()) {
            System.out.println("Previous state restored from disk — "
                    + "waiting for players to reconnect.");
        } else if (freshStart) {
            System.out.println("Clean startup requested — no previous save will be restored.");
        }

        MessageDispatcher dispatcher = new MessageDispatcher(lobby);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                ServerSideConnection conn = new SocketServerConnection(socket);

                conn.setOnMessage(msg -> dispatcher.dispatch(conn, msg));

                conn.setOnDisconnect(() -> {
                    if (conn.getNickname() != null) {
                        lobby.handleDisconnect(conn.getNickname());
                    } else {
                        lobby.unregisterPendingConnection(conn);
                    }
                });

                lobby.registerPendingConnection(conn);

                System.out.println("New connection: " + socket.getRemoteSocketAddress());
            }
        }
    }
}