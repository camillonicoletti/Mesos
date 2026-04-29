package it.polimi.ingsw.network.protocol.serverbound;

public class ReadyMessage implements ClientMessage {
    private final boolean ready;

    public ReadyMessage(boolean ready) {
        this.ready = ready;
    }

    public boolean isReady() {
        return ready;
    }
}
