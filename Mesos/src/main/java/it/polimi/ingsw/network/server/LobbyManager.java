package it.polimi.ingsw.network.server;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.network.protocol.clientbound.ErrorMessage;
import it.polimi.ingsw.network.protocol.clientbound.LobbyUpdateMessage;
import it.polimi.ingsw.network.protocol.clientbound.LoginAckMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.network.transport.ServerSideConnection;

import java.util.*;

public class LobbyManager {
    private final GameController gameController = new GameController();
    private final Map<String, ServerSideConnection> connections = new HashMap<>();
    private final Map<String, VirtualView> views = new HashMap<>();

    public synchronized void handleLogin(ServerSideConnection conn, LoginMessage msg) {
        String nick = msg.getNickname();

        if (connections.containsKey(nick)) {
            conn.send(new LoginAckMessage(false, "Nickname già in uso"));
            return;
        }

        Totem totem = new Totem(msg.getTotemColor(), null);
        try {
            gameController.onMessageReceived(nick, new JoinGameAction(totem));
        } catch (Exception e) {
            conn.send(new LoginAckMessage(false, e.getMessage()));
            return;
        }

        connections.put(nick, conn);
        VirtualView view = new VirtualView(conn, nick);
        views.put(nick, view);
        gameController.addListener(view);
        conn.setNickname(nick);

        conn.send(new LoginAckMessage(true, null));
        broadcastLobbyUpdate();
    }

    public synchronized void handleSetPlayersCount(String nick, SetPlayersCountMessage msg) {
        try {
            gameController.onMessageReceived(nick, new SetPlayerCountAction(msg.getCount()));
            broadcastLobbyUpdate();
        } catch (Exception e) {
            ServerSideConnection c = connections.get(nick);
            if (c != null) c.send(new ErrorMessage(e.getMessage()));
        }
    }

    public synchronized void handleReady(String nick, boolean ready) {
        try {
            gameController.onMessageReceived(nick, new ReadyAction(ready));
            broadcastLobbyUpdate();
        } catch (Exception e) {
            ServerSideConnection c = connections.get(nick);
            if (c != null) c.send(new ErrorMessage(e.getMessage()));
        }
    }

    public synchronized void handleDisconnect(String nick) {
        for (ServerSideConnection c : connections.values()) {
            c.send(new ErrorMessage("Player " + nick + " disconnesso. Partita terminata."));
            c.close();
        }
        connections.clear();
        views.clear();
    }

    private void broadcastLobbyUpdate() {
        List<String> players = new ArrayList<>(connections.keySet());
        int expected = gameController.getChosenPlayersCount();
        LobbyUpdateMessage msg = new LobbyUpdateMessage(players, expected);
        for (ServerSideConnection c : connections.values()) {
            c.send(msg);
        }
    }

    public GameController getGameController() { return gameController; }
}