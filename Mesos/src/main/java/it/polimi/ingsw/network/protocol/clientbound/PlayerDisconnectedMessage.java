package it.polimi.ingsw.network.protocol.clientbound;
public class PlayerDisconnectedMessage implements ServerMessage {
    private final String nickname;
    public PlayerDisconnectedMessage(String nickname) { this.nickname = nickname; }
    public String getNickname() { return nickname; }
}