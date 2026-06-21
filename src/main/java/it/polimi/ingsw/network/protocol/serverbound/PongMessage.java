package it.polimi.ingsw.network.protocol.serverbound;

/**
 * Heartbeat answer sent after the client receives a PingMessage.
 */
public class PongMessage implements ClientMessage {
    public PongMessage() {}
}
