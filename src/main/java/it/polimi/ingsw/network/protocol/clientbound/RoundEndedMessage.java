package it.polimi.ingsw.network.protocol.clientbound;

/**
 * Marks the end of a round for the client view.
 */
public class RoundEndedMessage implements ServerMessage {
    private final int roundNumber;
    public RoundEndedMessage(int roundNumber) { this.roundNumber = roundNumber; }
    public int getRoundNumber() { return roundNumber; }
}
