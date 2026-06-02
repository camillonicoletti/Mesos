package it.polimi.ingsw.network.protocol.serverbound;

/**
 * Player leaving the match. Handled by the server exactly like a
 * {@link DisconnectMessage}: a recoverable exit that suspends the match under
 * FA4 resilience and keeps the persisted state, so the player (or anyone else)
 * can reconnect and resume. It never terminates the game.
 */
public class QuitMessage implements ClientMessage {
    public QuitMessage() {}
}
