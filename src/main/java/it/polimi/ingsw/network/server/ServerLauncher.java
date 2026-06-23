package it.polimi.ingsw.network.server;

import it.polimi.ingsw.controller.leaderboard.LeaderboardFactory;
import it.polimi.ingsw.controller.leaderboard.LeaderboardService;
import it.polimi.ingsw.network.rmi.RMIHostConfigurator;
import it.polimi.ingsw.network.rmi.RMIServer;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Main entry point for the server JAR.
 * It starts one shared lobby and exposes it through both Socket and RMI.
 */
public final class ServerLauncher {

    private ServerLauncher() {}

    public static void main(String[] args) throws Exception {
        ServerLaunchOptions options = ServerLaunchOptions.unified(args);
        LeaderboardService leaderboard = LeaderboardFactory.createDefault();
        LobbyManager lobby = LobbyBootstrap.create(options.freshStart(), leaderboard);
        MessageDispatcher dispatcher = new MessageDispatcher(lobby);
        String advertisedHost = RMIHostConfigurator.configure(options.rmiHost());

        // RMI clients find this server through the registry and binding name.
        Registry registry = LocateRegistry.createRegistry(options.rmiPort());
        RMIServer rmiServer = new RMIServer(lobby);
        registry.rebind(options.rmiBindingName(), rmiServer);
        System.out.println("Server listening for RMI clients on registry port " + options.rmiPort());
        if (advertisedHost != null) {
            System.out.println("RMI stubs advertise host " + advertisedHost);
        }

        // Socket clients are accepted on a dedicated thread but use the same lobby.
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
