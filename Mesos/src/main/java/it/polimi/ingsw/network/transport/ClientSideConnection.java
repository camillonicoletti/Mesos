package it.polimi.ingsw.network.transport;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;

import java.io.IOException;
import java.util.function.Consumer;

public interface ClientSideConnection {
    void connect(String host, int port) throws IOException;
    void send(ClientMessage message);
    void setMessageHandler(Consumer<ServerMessage> handler);
    void close();
}