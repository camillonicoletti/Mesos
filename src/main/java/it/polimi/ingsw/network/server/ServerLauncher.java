package it.polimi.ingsw.network.server;

import it.polimi.ingsw.controller.leaderboard.LeaderboardFactory;
import it.polimi.ingsw.controller.leaderboard.LeaderboardService;
import it.polimi.ingsw.network.rmi.RMIServer;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Unified server bootstrap: starts both the Socket and the RMI transports
 * sharing a single {@link LobbyManager}, so clients connecting via different
 * technologies can take part in the same match.
 *
 * Usage: java ServerLauncher [--fresh] [socketPort] [rmiPort] [rmiBindingName]
 * Defaults: socketPort=1234, rmiPort=1099, rmiBindingName="MesosServer".
 */
public final class ServerLauncher {

    private ServerLauncher() {}

    public static void main(String[] args) throws Exception {
        ServerLaunchOptions options = ServerLaunchOptions.unified(args);
        LeaderboardService leaderboard = LeaderboardFactory.createDefault();
        LobbyManager lobby = LobbyBootstrap.create(options.freshStart(), leaderboard);
        MessageDispatcher dispatcher = new MessageDispatcher(lobby);

        Registry registry = LocateRegistry.createRegistry(options.rmiPort());
        RMIServer rmiServer = new RMIServer(lobby);
        registry.rebind(options.rmiBindingName(), rmiServer);
        System.out.println("Server listening for RMI clients on registry port " + options.rmiPort());

        final int finalSocketPort = options.socketPort();
        Thread socketThread = new Thread(() -> runSocketAcceptLoop(finalSocketPort, lobby, dispatcher),
                "socket-accept");
        socketThread.setDaemon(false);
        socketThread.start();
    }

    private static void runSocketAcceptLoop(int port, LobbyManager lobby, MessageDispatcher dispatcher) {
        try {
            SocketAcceptLoop.runBlocking(port, lobby, dispatcher,
                    "Socket server started on port ",
                    "New socket connection: ");
        } catch (IOException e) {
            System.err.println("Socket accept loop terminated: " + e.getMessage());
        }
    }
}
