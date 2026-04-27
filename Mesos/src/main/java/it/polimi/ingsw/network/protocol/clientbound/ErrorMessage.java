package it.polimi.ingsw.network.protocol.clientbound;
public class ErrorMessage implements ServerMessage {
    private final String reason;
    public ErrorMessage(String reason) { this.reason = reason; }
    public String getReason() { return reason; }
}