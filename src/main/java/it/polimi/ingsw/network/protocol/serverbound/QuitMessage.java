package it.polimi.ingsw.network.protocol.serverbound;

/**
 * Player explicitly leaving the match. Unlike a recoverable
 * {@link DisconnectMessage}, a mid-game quit is treated as an irreversible
 * exit and the server ends the match; pre-game quits still behave like a
 * regular disconnect/removal from the lobby.
 */
public class QuitMessage implements ClientMessage {
    public QuitMessage() {}
}
