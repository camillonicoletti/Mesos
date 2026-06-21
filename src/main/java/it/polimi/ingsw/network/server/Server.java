package it.polimi.ingsw.network.server;

import java.io.IOException;

/**
 * Legacy socket-only server entry point.
 * The final JAR uses ServerLauncher, which starts both Socket and RMI.
 */
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
