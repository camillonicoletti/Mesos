package it.polimi.ingsw.network.protocol.clientbound;
public class TotemPlacedMessage implements ServerMessage {
    private final String nickname;
    private final char tileId;
    public TotemPlacedMessage(String nickname, char tileId) {
        this.nickname = nickname;
        this.tileId = tileId;
    }
    public String getNickname() { return nickname; }
    public char getTileId() { return tileId; }
}