package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.transport.ClientSideConnection;

public class ClientApp {

    public static void main(String[] args) {
        ClientLaunchOptions options;
        try {
            options = ClientLaunchOptions.socket(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Usage: java ClientApp <host> <port>");
            System.exit(1);
            return;
        }

        SocketClientConnectionFactory factory = new SocketClientConnectionFactory(
                options.host(), options.socketPort());
        ClientSideConnection connection = createConnection(factory, factory.endpointDescription());
        ClientRuntime.run(connection, factory);
    }

    private static ClientSideConnection createConnection(ClientConnectionFactory factory,
                                                        String endpointDescription) {
        try {
            return factory.create();
        } catch (Exception e) {
            System.err.println("Unable to connect to " + endpointDescription + " — " + e.getMessage());
            return exitWithFailure();
        }
    }

    private static ClientSideConnection exitWithFailure() {
        System.exit(1);
        throw new IllegalStateException("System.exit returned unexpectedly");
    }

}
