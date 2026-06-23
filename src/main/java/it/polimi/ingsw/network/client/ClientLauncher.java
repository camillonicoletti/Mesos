package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.rmi.RMIHostConfigurator;
import it.polimi.ingsw.network.transport.ClientSideConnection;
import java.util.Arrays;

/**
 * Main entry point for the client JAR.
 * The first argument selects the transport, while the rest is parsed by ClientLaunchOptions.
 */
public final class ClientLauncher {

    private ClientLauncher() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsageAndExit();
            return;
        }

        String protocol = args[0].toLowerCase();
        String[] protocolArgs = Arrays.copyOfRange(args, 1, args.length);

        // The two transports stay explicit for the user, but share the same client runtime.
        switch (protocol) {
            case "socket" -> runSocketClient(protocolArgs);
            case "rmi" -> runRmiClient(protocolArgs);
            default -> printUsageAndExit();
        }
    }

    private static void runSocketClient(String[] args) {
        ClientLaunchOptions options;
        try {
            options = ClientLaunchOptions.socket(args);
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            printUsageAndExit();
            return;
        }

        SocketClientConnectionFactory factory = new SocketClientConnectionFactory(
                options.host(), options.socketPort());
        ClientSideConnection connection = createConnectionOrExit(factory, factory.endpointDescription());
        // Socket does not need an extra registration step after the TCP connection.
        ClientRuntime.run(connection, factory);
    }

    private static void runRmiClient(String[] args) {
        ClientLaunchOptions options;
        try {
            options = ClientLaunchOptions.rmi(args);
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            printUsageAndExit();
            return;
        }

        RmiClientConnectionFactory factory = new RmiClientConnectionFactory(
                options.host(),
                options.registryPort(),
                options.rmiBindingName());
        String advertisedHost = RMIHostConfigurator.configure(options.rmiHost());
        if (advertisedHost != null) {
            System.out.println("RMI callback advertises host " + advertisedHost);
        }
        ClientSideConnection connection = createConnectionOrExit(factory, factory.endpointDescription());
        // RMI must export and register the client callback before the view starts.
        ClientRuntime.run(connection, factory,
                () -> ((it.polimi.ingsw.network.rmi.RMIClientConnection) connection).register());
    }

    private static ClientSideConnection createConnectionOrExit(ClientConnectionFactory factory,
                                                              String endpointDescription) {
        try {
            return factory.create();
        } catch (Exception exception) {
            System.err.println("Unable to connect to " + endpointDescription + " - " + exception.getMessage());
            return exitWithFailure();
        }
    }

    private static void printUsageAndExit() {
        System.err.println("Usage:");
        System.err.println("  java -jar mesos-client.jar socket <host> <socketPort>");
        System.err.println("  java -jar mesos-client.jar rmi <host> [registryPort] [bindingName] [clientRmiHost]");
        System.exit(1);
    }

    private static ClientSideConnection exitWithFailure() {
        System.exit(1);
        throw new IllegalStateException("System.exit returned unexpectedly");
    }
}
