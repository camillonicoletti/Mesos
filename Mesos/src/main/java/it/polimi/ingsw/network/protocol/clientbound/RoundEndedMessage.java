package it.polimi.ingsw.network.protocol.clientbound;
public class RoundEndedMessage implements ServerMessage {
    private final int roundNumber;
    public RoundEndedMessage(int roundNumber) { this.roundNumber = roundNumber; }
    public int getRoundNumber() { return roundNumber; }
}