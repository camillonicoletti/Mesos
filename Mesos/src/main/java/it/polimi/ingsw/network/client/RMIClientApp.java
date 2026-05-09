package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.rmi.RMIClientConnection;
import it.polimi.ingsw.network.rmi.RMIServerMain;
import it.polimi.ingsw.network.rmi.RemoteServer;
import it.polimi.ingsw.network.transport.ClientSideConnection;
import it.polimi.ingsw.view.View;
import it.polimi.ingsw.view.tui.Tui;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

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
        ClientModel model = new ClientModel();
        ClientController controller = new ClientController(model, connection);
        View view = selectView(controller, model);
        try {
            ((RMIClientConnection) connection).register();
        } catch (java.rmi.RemoteException e) {
            System.err.println("Unable to register with the RMI server: " + e.getMessage());
            System.exit(1);
        }
        try {
            view.run();
        } catch (Exception exception) {
            System.err.println("Error: " + exception.getMessage());
            System.exit(1);
        } finally {
            connection.close();
        }
    }

    private static View selectView(ClientController controller, ClientModel model) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Select interface:");
            System.out.println("  1 — TUI (terminal)");
            System.out.println("  2 — GUI (graphical interface)");
            System.out.print("> ");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> {
                    return new Tui(controller, model);
                }
                case "2" -> System.out.println("GUI is not available yet.");
                default -> System.out.println("Invalid choice, type 1 or 2.");
            }
        }
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