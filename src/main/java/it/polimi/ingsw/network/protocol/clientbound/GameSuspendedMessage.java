package it.polimi.ingsw.network.protocol.clientbound;

public class GameSuspendedMessage implements ServerMessage {

    private final long remainingMillis;

    public GameSuspendedMessage() {
        this(-1L);
    }

    public GameSuspendedMessage(long remainingMillis) {
        this.remainingMillis = remainingMillis;
    }

    public long getRemainingMillis() {
        return remainingMillis;
    }
}
