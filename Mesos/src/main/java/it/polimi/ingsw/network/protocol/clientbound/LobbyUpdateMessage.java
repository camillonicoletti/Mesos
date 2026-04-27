package it.polimi.ingsw.network.protocol.clientbound;
import java.util.List;
public class LobbyUpdateMessage implements ServerMessage {
    private final List<String> currentPlayers;
    private final int expectedPlayers;
    public LobbyUpdateMessage(List<String> currentPlayers, int expectedPlayers) {
        this.currentPlayers = currentPlayers;
        this.expectedPlayers = expectedPlayers;
    }
    public List<String> getCurrentPlayers() { return currentPlayers; }
    public int getExpectedPlayers() { return expectedPlayers; }
}