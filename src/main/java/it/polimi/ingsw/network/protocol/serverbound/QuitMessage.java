package it.polimi.ingsw.network.protocol.serverbound;

/**
 * Voluntary quit. Semantically distinct from {@link DisconnectMessage}:
 * a disconnect can be a network drop that may be recovered via reconnect under
 * FA4 resilience, while a quit is the player explicitly leaving the match —
 * the server treats it as an irreversible exit that terminates the game.
 */
public class QuitMessage implements ClientMessage {
    public QuitMessage() {}
}
