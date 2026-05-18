package it.polimi.ingsw.network.server;

import it.polimi.ingsw.network.rmi.RMIServerMain;

import java.rmi.registry.Registry;

public final class ServerLaunchOptions {
    public static final int DEFAULT_SOCKET_PORT = 1234;

    private final boolean freshStart;
    private final int socketPort;
    private final int rmiPort;
    private final String rmiBindingName;

    private ServerLaunchOptions(boolean freshStart, int socketPort, int rmiPort, String rmiBindingName) {
        this.freshStart = freshStart;
        this.socketPort = socketPort;
        this.rmiPort = rmiPort;
        this.rmiBindingName = rmiBindingName;
    }

    public static ServerLaunchOptions socketOnly(String[] args) {
        boolean freshStart = false;
        Integer socketPort = null;

        for (String arg : args) {
            if ("--fresh".equalsIgnoreCase(arg)) {
                freshStart = true;
            } else if (socketPort == null) {
                socketPort = parsePort(arg, "socket port");
            } else {
                throw new IllegalArgumentException("Too many arguments for socket server startup.");
            }
        }

        return new ServerLaunchOptions(freshStart,
                defaultIfNull(socketPort, DEFAULT_SOCKET_PORT),
                Registry.REGISTRY_PORT,
                RMIServerMain.DEFAULT_BINDING_NAME);
    }

    public static ServerLaunchOptions rmiOnly(String[] args) {
        boolean freshStart = false;
        Integer rmiPort = null;
        String rmiBindingName = null;

        for (String arg : args) {
            if ("--fresh".equalsIgnoreCase(arg)) {
                freshStart = true;
            } else if (rmiPort == null) {
                rmiPort = parsePort(arg, "RMI registry port");
            } else if (rmiBindingName == null) {
                rmiBindingName = arg;
            } else {
                throw new IllegalArgumentException("Too many arguments for RMI server startup.");
            }
        }

        return new ServerLaunchOptions(freshStart,
                DEFAULT_SOCKET_PORT,
                defaultIfNull(rmiPort, Registry.REGISTRY_PORT),
                defaultIfNull(rmiBindingName, RMIServerMain.DEFAULT_BINDING_NAME));
    }

    public static ServerLaunchOptions unified(String[] args) {
        boolean freshStart = false;
        Integer socketPort = null;
        Integer rmiPort = null;
        String rmiBindingName = null;

        for (String arg : args) {
            if ("--fresh".equalsIgnoreCase(arg)) {
                freshStart = true;
            } else if (socketPort == null) {
                socketPort = parsePort(arg, "socket port");
            } else if (rmiPort == null) {
                rmiPort = parsePort(arg, "RMI registry port");
            } else if (rmiBindingName == null) {
                rmiBindingName = arg;
            } else {
                throw new IllegalArgumentException("Too many arguments for unified server startup.");
            }
        }

        return new ServerLaunchOptions(freshStart,
                defaultIfNull(socketPort, DEFAULT_SOCKET_PORT),
                defaultIfNull(rmiPort, Registry.REGISTRY_PORT),
                defaultIfNull(rmiBindingName, RMIServerMain.DEFAULT_BINDING_NAME));
    }

    public boolean freshStart() {
        return freshStart;
    }

    public int socketPort() {
        return socketPort;
    }

    public int rmiPort() {
        return rmiPort;
    }

    public String rmiBindingName() {
        return rmiBindingName;
    }

    private static int parsePort(String raw, String label) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + label + ": " + raw, e);
        }
    }

    private static int defaultIfNull(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static String defaultIfNull(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }
}
