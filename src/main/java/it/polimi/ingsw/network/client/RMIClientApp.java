package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.rmi.RMIClientConnection;
import it.polimi.ingsw.network.transport.ClientSideConnection;

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

        RmiClientConnectionFactory factory = new RmiClientConnectionFactory(
                options.host(),
                options.registryPort(),
                options.rmiBindingName());
        ClientSideConnection connection = createConnection(factory, factory.endpointDescription());
        ClientRuntime.run(connection, factory, () -> registerWithServer(connection));
    }

    private static void registerWithServer(ClientSideConnection connection) throws java.rmi.RemoteException {
        ((RMIClientConnection) connection).register();
    }

    private static ClientSideConnection createConnection(ClientConnectionFactory factory,
                                                        String endpointDescription) {
        try {
            return factory.create();
        } catch (Exception exception) {
            System.err.println("Unable to connect to " + endpointDescription + " — " + exception.getMessage());
            return exitWithFailure();
        }
    }

    private static ClientSideConnection exitWithFailure() {
        System.exit(1);
        throw new IllegalStateException("System.exit returned unexpectedly");
    }
}
