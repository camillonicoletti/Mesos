package it.polimi.ingsw.network.protocol.clientbound;

/**
 * Sent when only one player is still connected and the match is paused.
 */
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
