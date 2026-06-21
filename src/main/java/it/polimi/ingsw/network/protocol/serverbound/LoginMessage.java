package it.polimi.ingsw.network.protocol.serverbound;
import it.polimi.ingsw.model.enums.TotemColor;

/**
 * First message sent by a new client to enter the lobby.
 */
public class LoginMessage implements ClientMessage {
    private final String nickname;
    private final TotemColor totemColor;
    public LoginMessage(String nickname, TotemColor totemColor) {
        this.nickname = nickname;
        this.totemColor = totemColor;
    }
    public String getNickname() { return nickname; }
    public TotemColor getTotemColor() { return totemColor; }
}
