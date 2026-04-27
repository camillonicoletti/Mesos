package it.polimi.ingsw.network.protocol.serverbound;

/**
 * Simple payload used to verify socket communication during manual tests.
 */
public class DummyClientMessage implements ClientMessage {

    private final String payload;

    /**
     * Creates a dummy message containing a text payload.
     *
     * @param payload the text to send to the server
     */
    public DummyClientMessage(String payload) {
        this.payload = payload;
    }

    /**
     * Returns the payload carried by this message.
     *
     * @return the payload text
     */
    public String getPayload() {
        return payload;
    }
}
