package it.polimi.ingsw.network.protocol.serverbound;

/** Test message used to verify transport communication. */
public class DummyClientMessage implements ClientMessage {

    private final String payload;

    public DummyClientMessage(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }
}
