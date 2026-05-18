package it.polimi.ingsw.network.server;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.Persistence;
import it.polimi.ingsw.controller.leaderboard.LeaderboardFactory;
import it.polimi.ingsw.controller.leaderboard.LeaderboardService;
import it.polimi.ingsw.network.rmi.RMIServer;
import it.polimi.ingsw.network.rmi.RMIServerMain;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Optional;

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
        boolean freshStart = false;
        Integer socketPort = null;
        Integer rmiPort = null;
        String rmiBindingName = null;

        for (String arg : args) {
            if ("--fresh".equalsIgnoreCase(arg)) {
                freshStart = true;
            } else if (socketPort == null) {
                socketPort = Integer.parseInt(arg);
            } else if (rmiPort == null) {
                rmiPort = Integer.parseInt(arg);
            } else if (rmiBindingName == null) {
                rmiBindingName = arg;
            }
        }

        if (socketPort == null) socketPort = 1234;
        if (rmiPort == null)    rmiPort = Registry.REGISTRY_PORT;
        if (rmiBindingName == null) rmiBindingName = RMIServerMain.DEFAULT_BINDING_NAME;

        if (freshStart) {
            Persistence.clear();
        }

        LeaderboardService leaderboard = LeaderboardFactory.createDefault();

        Optional<GameController> restored = freshStart ? Optional.empty() : Persistence.load();
        LobbyManager lobby = restored
                .map(c -> new LobbyManager(c, leaderboard))
                .orElseGet(() -> new LobbyManager(leaderboard));

        if (restored.isPresent()) {
            System.out.println("Previous state restored from disk - "
                    + "waiting for players to reconnect.");
        } else if (freshStart) {
            System.out.println("Clean startup requested - no previous save will be restored.");
        }

        MessageDispatcher dispatcher = new MessageDispatcher(lobby);

        Registry registry = LocateRegistry.createRegistry(rmiPort);
        RMIServer rmiServer = new RMIServer(lobby);
        registry.rebind(rmiBindingName, rmiServer);
        System.out.println("Server listening for RMI clients on registry port " + rmiPort);

        final int finalSocketPort = socketPort;
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
