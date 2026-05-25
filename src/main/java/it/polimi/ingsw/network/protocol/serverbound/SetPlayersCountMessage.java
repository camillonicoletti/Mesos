package it.polimi.ingsw.network.protocol.serverbound;

public class SetPlayersCountMessage implements ClientMessage {
    private final int count;
    public SetPlayersCountMessage(int count) { this.count = count; }
    public int getCount() { return count; }
}