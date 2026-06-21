package it.polimi.ingsw.network.protocol.clientbound;

/**
 * Simple error DTO shown by the client when a request is rejected.
 */
public class ErrorMessage implements ServerMessage {
    private final String reason;
    public ErrorMessage(String reason) { this.reason = reason; }
    public String getReason() { return reason; }
}
