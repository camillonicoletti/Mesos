package it.polimi.ingsw.network.protocol.serverbound;

public class ReconnectMessage implements ClientMessage {
    private final String nickname;
    public ReconnectMessage(String nickname) { this.nickname = nickname; }
    public String getNickname() { return nickname; }
}