package it.polimi.ingsw.network.protocol.clientbound;

import java.util.List;

public class LobbyUpdateMessage implements ServerMessage {
    private final List<String> currentPlayers;
    private final int expectedPlayers;
    private final List<String> readyPlayers;

    public LobbyUpdateMessage(List<String> currentPlayers, int expectedPlayers, List<String> readyPlayers) {
        this.currentPlayers = currentPlayers;
        this.expectedPlayers = expectedPlayers;
        this.readyPlayers = readyPlayers;
    }

    public List<String> getCurrentPlayers() { return currentPlayers; }
    public int getExpectedPlayers()         { return expectedPlayers; }
    public List<String> getReadyPlayers()   { return readyPlayers; }
}