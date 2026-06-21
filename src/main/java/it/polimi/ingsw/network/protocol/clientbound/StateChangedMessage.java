package it.polimi.ingsw.network.protocol.clientbound;

/**
 * Sends the current server state name to keep the UI aligned.
 */
public class StateChangedMessage implements ServerMessage {
    private final String stateName;
    public StateChangedMessage(String stateName) { this.stateName = stateName; }
    public String getStateName() { return stateName; }
}
