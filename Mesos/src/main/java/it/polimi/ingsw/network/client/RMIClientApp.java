package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.rmi.RMIClientConnection;
import it.polimi.ingsw.network.rmi.RMIServerMain;
import it.polimi.ingsw.network.rmi.RemoteServer;
import it.polimi.ingsw.network.transport.ClientSideConnection;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Client bootstrap for the RMI transport.
 */
public class RMIClientApp {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java RMIClientApp <host> [registryPort] [bindingName]");
            System.exit(1);
        }

        String host = args[0];
        int registryPort = args.length > 1 ? Integer.parseInt(args[1]) : Registry.REGISTRY_PORT;
        String bindingName = args.length > 2 ? args[2] : RMIServerMain.DEFAULT_BINDING_NAME;

        ClientSideConnection connection = createConnection(host, registryPort, bindingName);
        ClientRuntime.run(connection, () -> registerWithServer(connection));
    }

    private static void registerWithServer(ClientSideConnection connection) throws java.rmi.RemoteException {
        ((RMIClientConnection) connection).register();
    }

    private static ClientSideConnection createConnection(String host, int registryPort, String bindingName) {
        try {
            Registry registry = LocateRegistry.getRegistry(host, registryPort);
            RemoteServer remoteServer = (RemoteServer) registry.lookup(bindingName);
            return new RMIClientConnection(remoteServer);
        } catch (Exception exception) {
            System.err.println("Unable to connect to the RMI registry at "
                    + host + ":" + registryPort + " — " + exception.getMessage());
            System.exit(1);
            return null;
        }
    }
}
