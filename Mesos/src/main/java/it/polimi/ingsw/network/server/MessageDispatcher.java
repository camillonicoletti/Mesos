package it.polimi.ingsw.network.server;

import it.polimi.ingsw.network.protocol.clientbound.ErrorMessage;
import it.polimi.ingsw.network.protocol.serverbound.*;
import it.polimi.ingsw.network.transport.ServerSideConnection;

import java.util.HashMap;
import java.util.Map;

public class MessageDispatcher {
    private final LobbyManager lobby;
    private final Map<Class<? extends ClientMessage>, MessageHandler<ClientMessage>> messageHandlers;

    public MessageDispatcher(LobbyManager lobby) {
        this.lobby = lobby;
        this.messageHandlers = createMessageHandlers();
    }

    public void dispatch(ServerSideConnection conn, ClientMessage msg) {
        try {
            MessageHandler<ClientMessage> handler = messageHandlers.get(msg.getClass());
            if (handler != null) {
                handler.handle(conn, msg);
            }
        } catch (Exception e) {
            conn.send(new ErrorMessage(e.getMessage()));
        }
    }

    private Map<Class<? extends ClientMessage>, MessageHandler<ClientMessage>> createMessageHandlers() {
        Map<Class<? extends ClientMessage>, MessageHandler<ClientMessage>> handlers = new HashMap<>();
        register(handlers, LoginMessage.class, lobby::handleLogin);
        register(handlers, ReconnectMessage.class, lobby::handleReconnect);
        register(handlers, SetPlayersCountMessage.class,
                (conn, message) -> lobby.handleSetPlayersCount(conn.getNickname(), message));
        register(handlers, ReadyMessage.class,
                (conn, message) -> lobby.handleReady(conn.getNickname(), message.isReady()));
        register(handlers, GameActionMessage.class,
                (conn, message) -> lobby.handleGameAction(conn.getNickname(), message.getAction()));
        register(handlers, PongMessage.class, (conn, message) -> {
            // Heartbeat response: no domain action required.
        });
        register(handlers, DisconnectMessage.class,
                (conn, message) -> lobby.handleDisconnect(conn.getNickname()));
        return Map.copyOf(handlers);
    }

    private <T extends ClientMessage> void register(
            Map<Class<? extends ClientMessage>, MessageHandler<ClientMessage>> handlers,
            Class<T> type,
            MessageHandler<T> handler) {
        handlers.put(type, (conn, message) -> handler.handle(conn, type.cast(message)));
    }

    @FunctionalInterface
    private interface MessageHandler<T extends ClientMessage> {
        void handle(ServerSideConnection connection, T message);
    }
}
