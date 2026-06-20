package it.polimi.ingsw.view.tui;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.transport.ClientSideConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Test double that records every message the controller sends, so command-parser
 * tests can assert on what was dispatched (the parser now executes actions
 * through ClientController instead of returning the message).
 */
final class CapturingClientConnection implements ClientSideConnection {

    final List<ClientMessage> sent = new ArrayList<>();

    @Override
    public void send(ClientMessage message) {
        sent.add(message);
    }

    @Override
    public void close() {
    }

    @Override
    public void setOnMessage(Consumer<ServerMessage> onMessage) {
    }

    @Override
    public void setOnDisconnect(Runnable onDisconnect) {
    }
}
