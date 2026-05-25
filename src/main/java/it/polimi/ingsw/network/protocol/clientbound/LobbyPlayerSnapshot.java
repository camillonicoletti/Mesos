package it.polimi.ingsw.network.protocol.clientbound;

import it.polimi.ingsw.model.enums.TotemColor;
import java.io.Serializable;

public class LobbyPlayerSnapshot implements Serializable {
    private final String nickname;
    private final TotemColor totemColor;
    private final boolean ready;
    private final boolean connected;

    public LobbyPlayerSnapshot(String nickname, TotemColor totemColor,
                               boolean ready, boolean connected) {
        this.nickname = nickname;
        this.totemColor = totemColor;
        this.ready = ready;
        this.connected = connected;
    }

    public String getNickname() { return nickname; }
    public TotemColor getTotemColor() { return totemColor; }
    public boolean isReady() { return ready; }
    public boolean isConnected() { return connected; }
}