package it.polimi.ingsw.network.protocol.clientbound;

/**
 * Login or reconnect result sent only to the client that tried to enter.
 */
public class LoginAckMessage implements ServerMessage {

    public enum Outcome{
        LOGIN_OK,
        RECONNECT_OK,
        REJECTED
    }

    private final Outcome outcome;
    private final String errorReason;

    public LoginAckMessage(Outcome outcome, String errorReason) {
        this.outcome = outcome;
        this.errorReason = errorReason;
    }

    public Outcome getOutcome() { return outcome; }
    public String getErrorReason() { return errorReason; }
    public boolean isSuccess() { return outcome != Outcome.REJECTED; }
}
