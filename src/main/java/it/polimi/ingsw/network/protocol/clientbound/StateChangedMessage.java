package it.polimi.ingsw.network.protocol.clientbound;
public class StateChangedMessage implements ServerMessage {
    private final String stateName;
    public StateChangedMessage(String stateName) { this.stateName = stateName; }
    public String getStateName() { return stateName; }
}