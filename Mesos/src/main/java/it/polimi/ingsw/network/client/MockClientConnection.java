package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.transport.ClientSideConnection;

import java.util.function.Consumer;

public class MockClientConnection implements ClientSideConnection {

    @Override
    public void connect(String host, int port) {
        System.out.println("[MOCK] connect → " + host + ":" + port);
    }

    @Override
    public void send(ClientMessage message) {
        System.out.println("[MOCK] send → " + message.getClass().getSimpleName());
    }

    @Override
    public void setMessageHandler(Consumer<ServerMessage> handler) {
        // non fa nulla — nessun messaggio simulato
    }

    @Override
    public void close() {
        System.out.println("[MOCK] connessione chiusa");
    }
}