package it.polimi.ingsw.network.transport;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import java.util.function.Consumer;

public interface ClientSideConnection {
    void send(ClientMessage message);
    void close();
    void setOnMessage(Consumer<ServerMessage> handler);
    void setOnDisconnect(Runnable handler);
}