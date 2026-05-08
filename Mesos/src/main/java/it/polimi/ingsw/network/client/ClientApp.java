package it.polimi.ingsw.network.client;

import it.polimi.ingsw.view.View;
import it.polimi.ingsw.view.tui.Tui;
import it.polimi.ingsw.network.transport.ClientSideConnection;
import it.polimi.ingsw.network.transport.SocketClientConnection;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClientApp {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java ClientApp <host> <port>");
            System.exit(1);
        }

        String host = args[0];
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Porta non valida: " + args[1]);
            System.exit(1);
            return;
        }

        ClientSideConnection connection = createConnection(host, port);
        ClientModel model               = new ClientModel();
        ClientController controller     = new ClientController(model, connection);
        View view                       = selectView(controller, model);

        try {
            view.run();
        } catch (Exception e) {
            System.err.println("Errore: " + e.getMessage());
            System.exit(1);
        } finally {
            connection.close();
        }
    }

    private static View selectView(ClientController controller, ClientModel model) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Seleziona interfaccia:");
            System.out.println("  1 — TUI (terminale)");
            System.out.println("  2 — GUI (grafica)");
            System.out.print("> ");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> { return new Tui(controller, model); }
                case "2" -> System.out.println("GUI non ancora disponibile.");
                default  -> System.out.println("Scelta non valida, digita 1 o 2.");
            }
        }
    }

    private static ClientSideConnection createConnection(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            return new SocketClientConnection(socket);
        } catch (IOException e) {
            System.err.println("Impossibile connettersi a " + host + ":" + port + " — " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

}