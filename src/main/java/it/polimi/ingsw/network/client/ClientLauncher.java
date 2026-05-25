package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.rmi.RMIClientConnection;
import it.polimi.ingsw.network.rmi.RemoteServer;
import it.polimi.ingsw.network.transport.ClientSideConnection;
import it.polimi.ingsw.network.transport.SocketClientConnection;

import java.io.IOException;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

/**
 * Single client entry point used by the final executable JAR.
 *
 * Usage:
 *   java -jar mesos-client.jar socket <host> <socketPort>
 *   java -jar mesos-client.jar rmi <host> [registryPort] [bindingName]
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

        ClientSideConnection connection = createSocketConnection(options.host(), options.socketPort());
        ClientRuntime.run(connection);
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

        ClientSideConnection connection = createRmiConnection(
                options.host(),
                options.registryPort(),
                options.rmiBindingName());
        ClientRuntime.run(connection, () -> ((RMIClientConnection) connection).register());
    }

    private static ClientSideConnection createSocketConnection(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            return new SocketClientConnection(socket);
        } catch (IOException exception) {
            System.err.println("Unable to connect to " + host + ":" + port + " - " + exception.getMessage());
            System.exit(1);
            return null;
        }
    }

    private static ClientSideConnection createRmiConnection(String host, int registryPort, String bindingName) {
        try {
            Registry registry = LocateRegistry.getRegistry(host, registryPort);
            RemoteServer remoteServer = (RemoteServer) registry.lookup(bindingName);
            return new RMIClientConnection(remoteServer);
        } catch (Exception exception) {
            System.err.println("Unable to connect to the RMI registry at "
                    + host + ":" + registryPort + " - " + exception.getMessage());
            System.exit(1);
            return null;
        }
    }

    private static void printUsageAndExit() {
        System.err.println("Usage:");
        System.err.println("  java -jar mesos-client.jar socket <host> <socketPort>");
        System.err.println("  java -jar mesos-client.jar rmi <host> [registryPort] [bindingName]");
        System.exit(1);
    }
}
