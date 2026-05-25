package it.polimi.ingsw.network.transport;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;

import java.util.function.Consumer;

/**
 * Abstraction of a client-side connection towards the server.
 */
public interface ClientSideConnection {

    /**
     * Sends a message to the server.
     *
     * @param message the message to send
     */
    void send(ClientMessage message);

    /**
     * Closes the connection and releases its resources.
     */
    void close();

    /**
     * Registers the callback to invoke when a server message arrives.
     *
     * @param onMessage the callback for inbound messages
     */
    void setOnMessage(Consumer<ServerMessage> onMessage);

    /**
     * Registers the callback to invoke when the connection is lost.
     *
     * @param onDisconnect the disconnect callback
     */
    void setOnDisconnect(Runnable onDisconnect);
}
