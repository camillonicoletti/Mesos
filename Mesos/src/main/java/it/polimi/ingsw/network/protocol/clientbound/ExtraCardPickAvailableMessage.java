package it.polimi.ingsw.network.protocol.clientbound;
public class ExtraCardPickAvailableMessage implements ServerMessage {
    private final String nickname;
    private final int picks;
    public ExtraCardPickAvailableMessage(String nickname, int picks) {
        this.nickname = nickname;
        this.picks = picks;
    }
    public String getNickname() { return nickname; }
    public int getPicks() { return picks; }
}