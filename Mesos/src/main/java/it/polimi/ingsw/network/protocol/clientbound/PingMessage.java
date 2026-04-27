package it.polimi.ingsw.network.protocol.clientbound;

/**
 * Heartbeat message sent by the server to verify that the client is still alive.
 */
public class PingMessage implements ServerMessage {
    public PingMessage() {
    }
}
