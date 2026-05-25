package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.network.server.LobbyBootstrap;
import it.polimi.ingsw.network.server.LobbyManager;
import it.polimi.ingsw.network.server.ServerLaunchOptions;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Bootstrap for the RMI transport.
 */
public final class RMIServerMain {

    public static final String DEFAULT_BINDING_NAME = "MesosServer";

    private RMIServerMain() {
    }

    public static void main(String[] args) throws Exception {
        ServerLaunchOptions options = ServerLaunchOptions.rmiOnly(args);
        LobbyManager lobbyManager = LobbyBootstrap.create(options.freshStart());
        Registry registry = LocateRegistry.createRegistry(options.rmiPort());
        RMIServer server = new RMIServer(lobbyManager);
        registry.rebind(options.rmiBindingName(), server);

        System.out.println("RMI server started on registry port "
                + options.rmiPort() + " with name '" + options.rmiBindingName() + "'");
    }
}
