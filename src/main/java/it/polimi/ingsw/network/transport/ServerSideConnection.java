package it.polimi.ingsw.network.transport;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;

import java.util.function.Consumer;

/**
 * Abstraction of a server-side connection towards a single client.
 */
public interface ServerSideConnection {

    /**
     * Sends a message to the connected client.
     *
     * @param message the message to send
     */
    void send(ServerMessage message);

    /**
     * Closes the connection and releases its resources.
     */
    void close();

    /**
     * Returns whether the underlying connection is still alive.
     *
     * @return true if the connection is open
     */
    boolean isAlive();

    /**
     * Returns the nickname associated with this connection, if known.
     *
     * @return the nickname, or null if not yet assigned
     */
    String getNickname();

    /**
     * Associates a nickname with this connection.
     *
     * @param nickname the player nickname
     */
    void setNickname(String nickname);

    /**
     * Registers the callback to invoke when a client message arrives.
     *
     * @param onMessage the callback for inbound messages
     */
    void setOnMessage(Consumer<ClientMessage> onMessage);

    /**
     * Registers the callback to invoke when the connection is lost.
     *
     * @param onDisconnect the disconnect callback
     */
    void setOnDisconnect(Runnable onDisconnect);
}
