package it.polimi.ingsw.network.protocol.clientbound;
public class GameStartedMessage implements ServerMessage {
    private final GameSnapshot snapshot;
    public GameStartedMessage(GameSnapshot snapshot) { this.snapshot = snapshot; }
    public GameSnapshot getSnapshot() { return snapshot; }
}