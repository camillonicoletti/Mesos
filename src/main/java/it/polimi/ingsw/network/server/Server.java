package it.polimi.ingsw.network.server;

import java.io.IOException;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerLaunchOptions options = ServerLaunchOptions.socketOnly(args);
        LobbyManager lobby = LobbyBootstrap.create(options.freshStart());
        MessageDispatcher dispatcher = new MessageDispatcher(lobby);

        SocketAcceptLoop.runBlocking(options.socketPort(), lobby, dispatcher,
                "Server started on port ",
                "New connection: ");
    }
}
