package it.polimi.ingsw.network.protocol.clientbound;

/** Test message used to verify transport communication. */
public class DummyServerMessage implements ServerMessage {

    private final String payload;

    public DummyServerMessage(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }
}
