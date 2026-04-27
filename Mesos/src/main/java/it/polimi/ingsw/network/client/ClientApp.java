package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.client.tui.Tui;
import it.polimi.ingsw.network.transport.ClientSideConnection;

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

        ClientSideConnection connection = createConnection();
        ClientModel model               = new ClientModel();
        ClientController controller     = new ClientController(model, connection);
        Tui tui                         = new Tui(controller, model);

        try {
            System.out.println("Client pronto per la connessione a " + host + ":" + port);
            tui.run();
        } catch (Exception e) {
            System.err.println("Errore di connessione: " + e.getMessage());
            System.exit(1);
        } finally {
            connection.close();
        }
    }

    private static ClientSideConnection createConnection() {
        // quando SocketClientConnection sarà pronta, sostituisci questa riga:
        // return new SocketClientConnection();
        return new MockClientConnection();
    }
}
