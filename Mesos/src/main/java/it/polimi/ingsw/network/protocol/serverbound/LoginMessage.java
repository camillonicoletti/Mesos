package it.polimi.ingsw.network.protocol.serverbound;
import it.polimi.ingsw.model.Totem;

public class LoginMessage implements ClientMessage {
    private final String nickname;
    private final Totem totem;
    public LoginMessage(String nickname, Totem totem) {
        this.nickname = nickname;
        this.totem = totem;
    }
    public String getNickname() { return nickname; }
    public Totem getTotem() { return totem; }
}