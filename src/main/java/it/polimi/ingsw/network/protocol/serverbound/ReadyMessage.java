package it.polimi.ingsw.network.protocol.serverbound;

/**
 * Changes the player's ready flag in the lobby.
 */
public class ReadyMessage implements ClientMessage {
    private final boolean ready;

    public ReadyMessage(boolean ready) {
        this.ready = ready;
    }

    public boolean isReady() {
        return ready;
    }
}
