package it.polimi.ingsw.network.protocol.clientbound;

import java.io.Serializable;

public class GameSyncMessage implements ServerMessage, Serializable {
    private final GameSnapshot snapshot;

    public GameSyncMessage(GameSnapshot snapshot) {
        this.snapshot = snapshot;
    }
    public GameSnapshot getSnapshot() { return snapshot; }
}