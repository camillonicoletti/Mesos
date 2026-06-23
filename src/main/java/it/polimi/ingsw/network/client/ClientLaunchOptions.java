package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.rmi.RMIServerMain;

import java.rmi.registry.Registry;

/**
 * Parsed command-line options for the client launcher.
 * It keeps socket and RMI settings in one object so ClientLauncher stays small.
 */
public final class ClientLaunchOptions {
    private static final int UNUSED_SOCKET_PORT = -1;

    private final String host;
    private final int socketPort;
    private final int registryPort;
    private final String rmiBindingName;
    private final String rmiHost;

    private ClientLaunchOptions(String host, int socketPort, int registryPort, String rmiBindingName, String rmiHost) {
        this.host = host;
        this.socketPort = socketPort;
        this.registryPort = registryPort;
        this.rmiBindingName = rmiBindingName;
        this.rmiHost = rmiHost;
    }

    public static ClientLaunchOptions socket(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected host and socket port.");
        }

        // Socket clients need only host and TCP port.
        return new ClientLaunchOptions(args[0],
                parsePort(args[1], "socket port"),
                Registry.REGISTRY_PORT,
                RMIServerMain.DEFAULT_BINDING_NAME,
                null);
    }

    public static ClientLaunchOptions rmi(String[] args) {
        if (args.length < 1 || args.length > 4) {
            throw new IllegalArgumentException("Expected host, optional registry port, binding name and client RMI host.");
        }

        int registryPort = args.length > 1
                ? parsePort(args[1], "RMI registry port")
                : Registry.REGISTRY_PORT;
        String bindingName = args.length > 2 ? args[2] : RMIServerMain.DEFAULT_BINDING_NAME;
        String rmiHost = args.length > 3 ? args[3] : null;

        // RMI clients use the registry port and binding name instead of a socket port.
        return new ClientLaunchOptions(args[0],
                UNUSED_SOCKET_PORT,
                registryPort,
                bindingName,
                rmiHost);
    }

    public String host() {
        return host;
    }

    public int socketPort() {
        return socketPort;
    }

    public int registryPort() {
        return registryPort;
    }

    public String rmiBindingName() {
        return rmiBindingName;
    }

    public String rmiHost() {
        return rmiHost;
    }

    private static int parsePort(String raw, String label) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + label + ": " + raw, e);
        }
    }

}
