package it.polimi.ingsw.network.server;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.network.protocol.clientbound.ErrorMessage;
import it.polimi.ingsw.network.protocol.clientbound.GameSyncMessage;
import it.polimi.ingsw.network.protocol.clientbound.GameStartedMessage;
import it.polimi.ingsw.network.protocol.clientbound.GameSuspendedMessage;
import it.polimi.ingsw.network.protocol.clientbound.GameSyncMessage;
import it.polimi.ingsw.network.protocol.clientbound.LobbyPlayerSnapshot;
import it.polimi.ingsw.network.protocol.clientbound.LobbyPlayerSnapshot;
import it.polimi.ingsw.network.protocol.clientbound.LobbyUpdateMessage;
import it.polimi.ingsw.network.protocol.clientbound.LoginAckMessage;
import it.polimi.ingsw.network.protocol.clientbound.LoginAckMessage.Outcome;
import it.polimi.ingsw.network.protocol.clientbound.StateChangedMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReconnectMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.network.transport.ServerSideConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LobbyManager {

    private final GameController gameController = new GameController();
    private final Map<String, ServerSideConnection> connections = new HashMap<>();
    private final Map<String, VirtualView> views = new HashMap<>();

    public synchronized void handleLogin(ServerSideConnection conn, LoginMessage msg) {
        String nick = msg.getNickname();

        if (nick == null || nick.isBlank()) {
            conn.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "Nickname non valido"));
            conn.send(new LoginAckMessage(Outcome.REJECTED, "Nickname non valido"));
            return;
        }
        if (connections.containsKey(nick)) {
            conn.send(new LoginAckMessage(Outcome.REJECTED, "Nickname già in uso"));
            return;
        }

        if (connections.containsKey(nick)) {
            conn.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "Nickname già in uso"));
        if (gameController.containsGamePlayer(nick) || gameController.containsLobbyPlayer(nick)) {
            conn.send(new LoginAckMessage(Outcome.REJECTED, "Nickname già registrato (usa reconnect)"));
            return;
        }
        if (gameController.getGame() != null) {
            conn.send(new LoginAckMessage(Outcome.REJECTED, "La partita è già iniziata"));
            return;
        }

        Totem totem = new Totem(msg.getTotemColor(), null);
        try {
            gameController.onMessageReceived(nick, new JoinGameAction(totem));
        } catch (Exception e) {
            conn.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, e.getMessage()));
            conn.send(new LoginAckMessage(Outcome.REJECTED, e.getMessage()));
            return;
        }

        registerConnection(nick, conn);
        conn.send(new LoginAckMessage(Outcome.LOGIN_OK, null));
        broadcastLobbyUpdate();
        conn.send(new StateChangedMessage(gameController.getCurrentState().getClass().getSimpleName()));
    }

    public synchronized void handleReconnect(ServerSideConnection conn, ReconnectMessage msg) {
        String nick = msg.getNickname();

        if (nick == null || nick.isBlank()) {
            conn.send(new LoginAckMessage(Outcome.REJECTED, "Nickname non valido"));
            return;
        }
        if (!gameController.containsGamePlayer(nick) && !gameController.containsLobbyPlayer(nick)) {
            conn.send(new LoginAckMessage(Outcome.REJECTED, "Nickname sconosciuto"));
            return;
        }
        if (connections.containsKey(nick)) {
            conn.send(new LoginAckMessage(Outcome.REJECTED, "Player già connesso"));
            return;
        }
        if (!gameController.isPlayerDisconnected(nick)) {
            conn.send(new LoginAckMessage(Outcome.REJECTED, "Player non risulta disconnesso"));
            return;
        }

        registerConnection(nick, conn);

        conn.send(new LoginAckMessage(LoginAckMessage.Outcome.LOGIN_OK, null));
        broadcastLobbyUpdate();
        conn.send(new LoginAckMessage(Outcome.RECONNECT_OK, null));
        if (gameController.getGame() != null) {
            conn.send(new GameSyncMessage(SnapshotBuilder.build(gameController)));
        } else {
            broadcastLobbyUpdate();
        }
        conn.send(new StateChangedMessage(gameController.getCurrentState().getClass().getSimpleName()));

        try {
            gameController.onPlayerReconnection(nick);
        } catch (Exception e) {
            conn.send(new ErrorMessage(e.getMessage()));
        }

        if (gameController.isSuspended()) {
            conn.send(new GameSuspendedMessage());
        }
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

    public synchronized void handleReconnect(ServerSideConnection conn, ReconnectMessage msg) {
        String nick = msg.getNickname();
        if (nick == null || nick.isBlank()) {
            conn.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "Nickname non valido"));
            return;
        }
        if (!gameController.containsGamePlayer(nick)) {
            conn.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "Player sconosciuto"));
            return;
        }
        if (!gameController.isPlayerDisconnected(nick)) {
            conn.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "Player già connesso"));
            return;
        }

        try {
            gameController.onPlayerReconnection(nick);
        } catch (Exception e) {
            conn.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, e.getMessage()));
            return;
        }

        registerConnection(nick, conn);
        conn.send(new LoginAckMessage(LoginAckMessage.Outcome.RECONNECT_OK, null));
        conn.send(new GameSyncMessage(SnapshotBuilder.build(gameController)));
        conn.send(new StateChangedMessage(gameController.getCurrentState().getClass().getSimpleName()));
        if (gameController.isSuspended()) {
            conn.send(new GameSuspendedMessage());
        }
    }

    public synchronized void handleReady(String nick, boolean ready) {
        try {
            boolean gameWasNotStarted = gameController.getGame() == null;
            gameController.onMessageReceived(nick, new ReadyAction(ready));
            if (gameWasNotStarted && gameController.getGame() != null) {
                broadcastGameStarted();
            } else {
                broadcastLobbyUpdate();
            }
        } catch (Exception e) {
            ServerSideConnection c = connections.get(nick);
            if (c != null) c.send(new ErrorMessage(e.getMessage()));
        }
    }

    public synchronized void handleDisconnect(String nick) {
        if (nick == null || nick.isBlank()) return;

        unregisterConnection(nick);

        if (!gameController.containsLobbyPlayer(nick) && !gameController.containsGamePlayer(nick)) {
            return;
        }

        try {
            gameController.onPlayerDisconnection(nick);
        } catch (Exception ignored) {}

        if (gameController.getGame() == null) {
            broadcastLobbyUpdate();
        }
    }

    private void broadcastLobbyUpdate() {
        List<LobbyPlayerSnapshot> players = gameController.getLobbyPlayers().stream()
                .map(player -> new LobbyPlayerSnapshot(
                        player.getNickname(),
                        player.getTotem().getColor(),
                        gameController.isPlayerReady(player.getNickname()),
                        !gameController.isPlayerDisconnected(player.getNickname())
                ))
                .collect(Collectors.toList());
        int expected = gameController.getChosenPlayersCount();
        LobbyUpdateMessage msg = new LobbyUpdateMessage(players, expected);
        List<LobbyPlayerSnapshot> snaps = new ArrayList<>();
        for (Player p : gameController.getLobbyPlayers()) {
            String nick = p.getNickname();
            snaps.add(new LobbyPlayerSnapshot(
                    nick,
                    p.getTotem() != null ? p.getTotem().getColor() : null,
                    gameController.isPlayerReady(nick),
                    !gameController.isPlayerDisconnected(nick)
            ));
        }
        LobbyUpdateMessage msg = new LobbyUpdateMessage(snaps, gameController.getChosenPlayersCount());
        for (ServerSideConnection c : connections.values()) {
            c.send(msg);
        }
    }

    private void broadcastGameStarted() {
        GameStartedMessage message = new GameStartedMessage(SnapshotBuilder.build(gameController));
        for (ServerSideConnection connection : connections.values()) {
            connection.send(message);
        }
    }

    private void registerConnection(String nickname, ServerSideConnection connection) {
        unregisterConnection(nickname);
        connections.put(nickname, connection);
        VirtualView view = new VirtualView(connection, nickname);
        views.put(nickname, view);
        gameController.addListener(view);
        connection.setNickname(nickname);
    }

    private void unregisterConnection(String nickname) {
        ServerSideConnection removedConnection = connections.remove(nickname);
        VirtualView removedView = views.remove(nickname);
        if (removedView != null) {
            gameController.removeListener(removedView);
        }
        if (removedConnection != null && removedConnection.isAlive()) {
            removedConnection.close();
        }
    }

    public GameController getGameController() { return gameController; }
}
