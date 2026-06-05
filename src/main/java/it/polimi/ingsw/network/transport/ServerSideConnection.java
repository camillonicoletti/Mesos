package it.polimi.ingsw.network.transport;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;

import java.util.function.Consumer;

/**
 * Common interface used by the server to talk with one client.
 *
 * <p>LobbyManager and VirtualView use this interface without caring if the
 * client is connected through Socket or RMI.</p>
 */
public interface ServerSideConnection {

    /**
     * Sends one message to the connected client.
     *
     * @param message the message to send
     */
    void send(ServerMessage message);

    /**
     * Closes the connection and releases its resources.
     */
    void close();

    /**
     * Returns whether the underlying connection is still usable.
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
     * Registers the callback used for messages coming from this client.
     *
     * @param onMessage the callback for inbound messages
     */
    void setOnMessage(Consumer<ClientMessage> onMessage);

    /**
     * Registers the callback used when this client disconnects unexpectedly.
     *
     * @param onDisconnect the disconnect callback
     */
    void setOnDisconnect(Runnable onDisconnect);
}
