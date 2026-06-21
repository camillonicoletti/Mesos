package it.polimi.ingsw.network.protocol.serverbound;

/**
 * Sent by a client that wants to recover an already known nickname.
 */
public class ReconnectMessage implements ClientMessage {
    private final String nickname;
    public ReconnectMessage(String nickname) { this.nickname = nickname; }
    public String getNickname() { return nickname; }
}
