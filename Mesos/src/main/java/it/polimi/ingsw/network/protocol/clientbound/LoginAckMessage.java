package it.polimi.ingsw.network.protocol.clientbound;

public class LoginAckMessage implements ServerMessage {
    private final boolean success;
    private final String errorReason;
    public LoginAckMessage(boolean success, String errorReason) {
        this.success = success;
        this.errorReason = errorReason;
    }
    public boolean isSuccess() { return success; }
    public String getErrorReason() { return errorReason; }
}