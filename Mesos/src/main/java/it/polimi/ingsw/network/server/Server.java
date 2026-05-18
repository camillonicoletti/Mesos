package it.polimi.ingsw.network.server;

import java.io.IOException;

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

        LobbyManager lobby = LobbyBootstrap.create(freshStart);
        MessageDispatcher dispatcher = new MessageDispatcher(lobby);

        SocketAcceptLoop.runBlocking(port, lobby, dispatcher,
                "Server started on port ",
                "New connection: ");
    }
}
