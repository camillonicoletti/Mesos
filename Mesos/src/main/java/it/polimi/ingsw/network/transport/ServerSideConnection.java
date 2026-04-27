package it.polimi.ingsw.network.transport;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import java.util.function.Consumer;

public interface ServerSideConnection {
    void send(ServerMessage message);
    void close();
    boolean isAlive();
    String getNickname();
    void setNickname(String nickname);
    void setOnMessage(Consumer<ClientMessage> handler);
    void setOnDisconnect(Runnable handler);
}