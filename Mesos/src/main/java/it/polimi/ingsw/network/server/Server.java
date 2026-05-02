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
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 1234;

        Optional<GameController> restored = Persistence.load();
        LobbyManager lobby = restored
                .map(LobbyManager::new)
                .orElseGet(LobbyManager::new);

        if (restored.isPresent()) {
            System.out.println("Stato precedente ripristinato da disco — "
                    + "in attesa che i giocatori si riconnettano.");
        }

        MessageDispatcher dispatcher = new MessageDispatcher(lobby);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server avviato su porta " + port);

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

                System.out.println("Nuova connessione: " + socket.getRemoteSocketAddress());
            }
        }
    }
}