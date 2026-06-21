package it.polimi.ingsw.network.protocol.clientbound;

/**
 * Announces whose turn is active now.
 */
public class CurrentPlayerChangedMessage implements ServerMessage {

    private final String nickname;

    public CurrentPlayerChangedMessage(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }
}
