package it.polimi.ingsw.network.protocol.clientbound;
public class PlayerReconnectedMessage implements ServerMessage {
    private final String nickname;
    public PlayerReconnectedMessage(String nickname) { this.nickname = nickname; }
    public String getNickname() { return nickname; }
}