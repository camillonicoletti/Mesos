package it.polimi.ingsw.network.server;

import it.polimi.ingsw.network.protocol.clientbound.ErrorMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.DisconnectMessage;
import it.polimi.ingsw.network.protocol.serverbound.ForceEndGameMessage;
import it.polimi.ingsw.network.protocol.serverbound.GameActionMessage;
import it.polimi.ingsw.network.protocol.serverbound.GetLeaderboardMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.PongMessage;
import it.polimi.ingsw.network.protocol.serverbound.QuitMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReadyMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReconnectMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.network.transport.ServerSideConnection;

import java.util.HashMap;
import java.util.Map;

public class MessageDispatcher {

    private final LobbyManager lobby;
    private final Map<Class<? extends ClientMessage>, MessageHandler<ClientMessage>> handlers;

    public MessageDispatcher(LobbyManager lobby) {
        if (lobby == null) {
            throw new IllegalArgumentException("Lobby manager cannot be null");
        }
        this.lobby = lobby;
        this.handlers = createHandlers();
    }

    public void dispatch(ServerSideConnection connection, ClientMessage message) {
        try {
            MessageHandler<ClientMessage> handler = handlers.get(message.getClass());
            if (handler != null) {
                handler.handle(connection, message);
            }
        } catch (Exception exception) {
            connection.send(new ErrorMessage(exception.getMessage()));
        }
    }

    private Map<Class<? extends ClientMessage>, MessageHandler<ClientMessage>> createHandlers() {
        Map<Class<? extends ClientMessage>, MessageHandler<ClientMessage>> map = new HashMap<>();
        register(map, LoginMessage.class, lobby::handleLogin);
        register(map, ReconnectMessage.class, lobby::handleReconnect);
        register(map, SetPlayersCountMessage.class,
                (connection, message) -> lobby.handleSetPlayersCount(connection.getNickname(), message));
        register(map, ReadyMessage.class,
                (connection, message) -> lobby.handleReady(connection.getNickname(), message.isReady()));
        register(map, GameActionMessage.class,
                (connection, message) -> lobby.handleGameAction(connection.getNickname(), message.getAction()));
        register(map, GetLeaderboardMessage.class, lobby::handleGetLeaderboard);
        register(map, PongMessage.class, (connection, message) -> { });
        register(map, DisconnectMessage.class,
                (connection, message) -> lobby.handleDisconnect(connection.getNickname()));
        register(map, QuitMessage.class,
                (connection, message) -> lobby.handleQuit(connection.getNickname()));
        register(map, ForceEndGameMessage.class,
                (connection, message) -> lobby.handleForceEndGame());
        return Map.copyOf(map);
    }

    private <T extends ClientMessage> void register(Map<Class<? extends ClientMessage>, MessageHandler<ClientMessage>> map,
                                                    Class<T> type,
                                                    MessageHandler<T> handler) {
        map.put(type, (connection, message) -> handler.handle(connection, type.cast(message)));
    }

    @FunctionalInterface
    private interface MessageHandler<T extends ClientMessage> {
        void handle(ServerSideConnection connection, T message);
    }
}
