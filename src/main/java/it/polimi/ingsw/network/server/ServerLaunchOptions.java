package it.polimi.ingsw.network.server;

import it.polimi.ingsw.network.rmi.RMIServerMain;

import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        PositionalArguments parsed = PositionalArguments.from(args);
        Optional<Integer> socketPort = parsed.nextPort("socket port");
        parsed.rejectRemaining("Too many arguments for socket server startup.");

        return new ServerLaunchOptions(parsed.freshStart(),
                socketPort.orElse(DEFAULT_SOCKET_PORT),
                Registry.REGISTRY_PORT,
                RMIServerMain.DEFAULT_BINDING_NAME);
    }

    public static ServerLaunchOptions rmiOnly(String[] args) {
        PositionalArguments parsed = PositionalArguments.from(args);
        Optional<Integer> rmiPort = parsed.nextPort("RMI registry port");
        Optional<String> rmiBindingName = parsed.nextString();
        parsed.rejectRemaining("Too many arguments for RMI server startup.");

        return new ServerLaunchOptions(parsed.freshStart(),
                DEFAULT_SOCKET_PORT,
                rmiPort.orElse(Registry.REGISTRY_PORT),
                rmiBindingName.orElse(RMIServerMain.DEFAULT_BINDING_NAME));
    }

    public static ServerLaunchOptions unified(String[] args) {
        PositionalArguments parsed = PositionalArguments.from(args);
        Optional<Integer> socketPort = parsed.nextPort("socket port");
        Optional<Integer> rmiPort = parsed.nextPort("RMI registry port");
        Optional<String> rmiBindingName = parsed.nextString();
        parsed.rejectRemaining("Too many arguments for unified server startup.");

        return new ServerLaunchOptions(parsed.freshStart(),
                socketPort.orElse(DEFAULT_SOCKET_PORT),
                rmiPort.orElse(Registry.REGISTRY_PORT),
                rmiBindingName.orElse(RMIServerMain.DEFAULT_BINDING_NAME));
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

    private static final class PositionalArguments {
        private final boolean freshStart;
        private final List<String> values;
        private int cursor;

        private PositionalArguments(boolean freshStart, List<String> values) {
            this.freshStart = freshStart;
            this.values = values;
        }

        static PositionalArguments from(String[] args) {
            boolean freshStart = false;
            List<String> values = new ArrayList<>();
            for (String arg : args) {
                if ("--fresh".equalsIgnoreCase(arg)) {
                    freshStart = true;
                    continue;
                }
                values.add(arg);
            }
            return new PositionalArguments(freshStart, values);
        }

        boolean freshStart() {
            return freshStart;
        }

        Optional<Integer> nextPort(String label) {
            return nextString().map(raw -> parsePort(raw, label));
        }

        Optional<String> nextString() {
            if (cursor >= values.size()) {
                return Optional.empty();
            }
            String value = values.get(cursor);
            cursor++;
            return Optional.of(value);
        }

        void rejectRemaining(String message) {
            if (cursor < values.size()) {
                throw new IllegalArgumentException(message);
            }
        }
    }
}
