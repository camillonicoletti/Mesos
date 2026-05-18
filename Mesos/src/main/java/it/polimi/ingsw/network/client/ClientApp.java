package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.transport.ClientSideConnection;
import it.polimi.ingsw.network.transport.SocketClientConnection;

import java.io.IOException;
import java.net.Socket;

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

        ClientSideConnection connection = createConnection(options.host(), options.socketPort());
        ClientRuntime.run(connection);
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
