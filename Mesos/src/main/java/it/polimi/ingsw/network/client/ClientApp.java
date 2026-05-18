package it.polimi.ingsw.network.client;

import it.polimi.ingsw.view.View;
import it.polimi.ingsw.network.transport.ClientSideConnection;
import it.polimi.ingsw.network.transport.SocketClientConnection;

import java.io.IOException;
import java.net.Socket;

public class ClientApp {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ClientApp <host> <port>");
            System.exit(1);
        }

        String host = args[0];
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port: " + args[1]);
            System.exit(1);
            return;
        }

        ClientSideConnection connection = createConnection(host, port);
        ClientModel model               = new ClientModel();
        ClientController controller     = new ClientController(model, connection);
        View view                       = ClientViewSelector.select(controller, model);

        try {
            view.run();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } finally {
            connection.close();
        }
    }

    private static ClientSideConnection createConnection(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            return new SocketClientConnection(socket);
        } catch (IOException e) {
            System.err.println("Unable to connect to " + host + ":" + port + " — " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

}
