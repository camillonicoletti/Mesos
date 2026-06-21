package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.transport.ClientSideConnection;
import it.polimi.ingsw.network.transport.SocketClientConnection;

import java.net.Socket;

/** Factory for socket client connections. */
public final class SocketClientConnectionFactory implements ClientConnectionFactory {
    private final String host;
    private final int port;

    public SocketClientConnectionFactory(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public ClientSideConnection create() throws Exception {
        // Each call opens a new TCP socket, useful both at startup and during reconnect.
        return new SocketClientConnection(new Socket(host, port));
    }

    public String endpointDescription() {
        return host + ":" + port;
    }
}
