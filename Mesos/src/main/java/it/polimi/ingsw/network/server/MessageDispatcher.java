package it.polimi.ingsw.network.server;

import it.polimi.ingsw.network.protocol.clientbound.ErrorMessage;
import it.polimi.ingsw.network.protocol.serverbound.*;
import it.polimi.ingsw.network.transport.ServerSideConnection;

public class MessageDispatcher {
    private final LobbyManager lobby;

    public MessageDispatcher(LobbyManager lobby) {
        this.lobby = lobby;
    }

    public void dispatch(ServerSideConnection conn, ClientMessage msg) {
        try {
            if (msg instanceof LoginMessage m) {
                lobby.handleLogin(conn, m);
            } else if (msg instanceof SetPlayersCountMessage m) {
                lobby.handleSetPlayersCount(conn.getNickname(), m);
            } else if (msg instanceof ReadyMessage) {
                lobby.handleReady(conn.getNickname(), true);
            } else if (msg instanceof GameActionMessage m) {
                lobby.getGameController().onMessageReceived(conn.getNickname(), m.getAction());
            } else if (msg instanceof PongMessage) {
                // heartbeat: aggiorna timestamp se implementato
            } else if (msg instanceof DisconnectMessage) {
                lobby.handleDisconnect(conn.getNickname());
            }
        } catch (Exception e) {
            conn.send(new ErrorMessage(e.getMessage()));
        }
    }
}