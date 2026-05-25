package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.rmi.RMIClientConnection;
import it.polimi.ingsw.network.rmi.RemoteServer;
import it.polimi.ingsw.network.transport.ClientSideConnection;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Client bootstrap for the RMI transport.
 */
public class RMIClientApp {

    public static void main(String[] args) {
        ClientLaunchOptions options;
        try {
            options = ClientLaunchOptions.rmi(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Usage: java RMIClientApp <host> [registryPort] [bindingName]");
            System.exit(1);
            return;
        }

        ClientSideConnection connection = createConnection(
                options.host(),
                options.registryPort(),
                options.rmiBindingName());
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
