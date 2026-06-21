package it.polimi.ingsw.network.protocol.serverbound;

/**
 * Lobby request to change how many players the match should wait for.
 */
public class SetPlayersCountMessage implements ClientMessage {
    private final int count;
    public SetPlayersCountMessage(int count) { this.count = count; }
    public int getCount() { return count; }
}
