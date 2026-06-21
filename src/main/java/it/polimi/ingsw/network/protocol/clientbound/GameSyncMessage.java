package it.polimi.ingsw.network.protocol.clientbound;

import java.io.Serializable;

/**
 * Server-authoritative snapshot used to realign a client after reconnect.
 */
public class GameSyncMessage implements ServerMessage, Serializable {
    private final GameSnapshot snapshot;

    public GameSyncMessage(GameSnapshot snapshot) {
        this.snapshot = snapshot;
    }
    public GameSnapshot getSnapshot() { return snapshot; }
}
