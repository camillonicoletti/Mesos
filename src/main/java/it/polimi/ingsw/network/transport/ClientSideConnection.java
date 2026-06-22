package it.polimi.ingsw.network.transport;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;

import java.util.function.Consumer;

/**
 * Common interface used by the client to talk with the server.
 *
 * The client code does not need to know if the real transport is Socket or
 * RMI: it only sends messages, receives server messages and reacts when the
 * connection drops.
 */
public interface ClientSideConnection {

    /**
     * Sends one message to the server.
     *
     * @param message the message to send
     */
    void send(ClientMessage message);

    /**
     * Closes the connection and releases its resources.
     */
    void close();

    /**
     * Registers the callback used for messages coming from the server.
     *
     * @param onMessage the callback for inbound messages
     */
    void setOnMessage(Consumer<ServerMessage> onMessage);

    /**
     * Registers the callback used when the connection is lost.
     *
     * @param onDisconnect the disconnect callback
     */
    void setOnDisconnect(Runnable onDisconnect);
}
