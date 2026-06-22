package it.polimi.ingsw.network.protocol.clientbound;

/**
 * Login or reconnect result sent only to the client that tried to enter.
 */
public class LoginAckMessage implements ServerMessage {

    private final LoginResult result;

    public LoginAckMessage(LoginResult result) {
        this.result = result;
    }

    public LoginResult getResult() { return result; }
    public String getErrorReason() { return result.errorReason(); }
    public boolean isSuccess() { return result.isSuccess(); }
}
