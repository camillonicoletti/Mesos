package it.polimi.ingsw.network.protocol.clientbound;

import java.io.Serializable;
import java.util.List;

public class LobbyUpdateMessage implements ServerMessage, Serializable {
    private final List<LobbyPlayerSnapshot> players;
    private final int expectedPlayers;

    public LobbyUpdateMessage(List<LobbyPlayerSnapshot> players, int expectedPlayers) {
        this.players = List.copyOf(players);
        this.expectedPlayers = expectedPlayers;
    }

    public List<LobbyPlayerSnapshot> getPlayers() {
        return players;
    }

    public int getExpectedPlayers() {
        return expectedPlayers;
    }
}