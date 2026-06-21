package it.polimi.ingsw.network.protocol.clientbound;

/**
 * Tells clients that a player is temporarily offline.
 */
public class PlayerDisconnectedMessage implements ServerMessage {
    private final String nickname;
    public PlayerDisconnectedMessage(String nickname) { this.nickname = nickname; }
    public String getNickname() { return nickname; }
}
