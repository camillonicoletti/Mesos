package it.polimi.ingsw.network.server;

import it.polimi.ingsw.network.protocol.clientbound.ErrorMessage;
import it.polimi.ingsw.network.protocol.serverbound.*;
import it.polimi.ingsw.network.protocol.serverbound.ForceEndGameMessage;
import it.polimi.ingsw.network.transport.ServerSideConnection;
import it.polimi.ingsw.network.protocol.serverbound.GetLeaderboardMessage;
public class MessageDispatcher {
    private final LobbyManager lobby;

    public MessageDispatcher(LobbyManager lobby) {
        this.lobby = lobby;
    }

    public void dispatch(ServerSideConnection conn, ClientMessage msg) {
        try {
            if (msg instanceof LoginMessage m) {
                lobby.handleLogin(conn, m);
            } else if (msg instanceof ReconnectMessage m) {
                lobby.handleReconnect(conn, m);
            } else if (msg instanceof SetPlayersCountMessage m) {
                lobby.handleSetPlayersCount(conn.getNickname(), m);
            } else if (msg instanceof ReadyMessage m) {
                lobby.handleReady(conn.getNickname(), m.isReady());
            } else if (msg instanceof GameActionMessage m) {
                lobby.handleGameAction(conn.getNickname(), m.getAction());
            } else if (msg instanceof GetLeaderboardMessage m) {
                lobby.handleGetLeaderboard(conn, m);
            } else if (msg instanceof PongMessage) {
                // heartbeat
            } else if (msg instanceof DisconnectMessage) {
                lobby.handleDisconnect(conn.getNickname());
            } else if (msg instanceof ForceEndGameMessage) {
                lobby.handleForceEndGame();
            }
        } catch (Exception e) {
            conn.send(new ErrorMessage(e.getMessage()));
        }
    }
}