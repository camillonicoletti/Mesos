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
        String advertisedHost = RMIHostConfigurator.configure(options.rmiHost());
        // The registry is the RMI name service where clients look up the server.
        Registry registry = LocateRegistry.createRegistry(options.rmiPort());
        RMIServer server = new RMIServer(lobbyManager);
        // The binding name is the public name used by RMI clients.
        registry.rebind(options.rmiBindingName(), server);

        System.out.println("RMI server started on registry port "
                + options.rmiPort() + " with name '" + options.rmiBindingName() + "'");
        if (advertisedHost != null) {
            System.out.println("RMI stubs advertise host " + advertisedHost);
        }
    }
}
