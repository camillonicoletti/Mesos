package it.polimi.ingsw.network.server;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.network.protocol.clientbound.ErrorMessage;
import it.polimi.ingsw.network.protocol.clientbound.GameStartedMessage;
import it.polimi.ingsw.network.protocol.clientbound.GameSuspendedMessage;
import it.polimi.ingsw.network.protocol.clientbound.GameSyncMessage;
import it.polimi.ingsw.network.protocol.clientbound.LobbyPlayerSnapshot;
import it.polimi.ingsw.network.protocol.clientbound.LobbyUpdateMessage;
import it.polimi.ingsw.network.protocol.clientbound.LoginAckMessage;
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

    public synchronized void handleLogin(ServerSideConnection connection, LoginMessage message) {
        String nickname = message.getNickname();

        if (nickname == null || nickname.isBlank()) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "Nickname non valido"));
            return;
        }
        if (connections.containsKey(nickname)) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "Nickname già in uso"));
            return;
        }
        if (gameController.containsLobbyPlayer(nickname) || gameController.containsGamePlayer(nickname)) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED,
                    "Nickname già registrato (usa reconnect)"));
            return;
        }
        if (gameController.getGame() != null) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "La partita è già iniziata"));
            return;
        }

        Totem totem = new Totem(message.getTotemColor(), null);
        try {
            gameController.onMessageReceived(nickname, new JoinGameAction(totem));
        } catch (Exception exception) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, exception.getMessage()));
            return;
        }

        registerConnection(nickname, connection);
        connection.send(new LoginAckMessage(LoginAckMessage.Outcome.LOGIN_OK, null));
        broadcastLobbyUpdate();
        connection.send(new StateChangedMessage(gameController.getCurrentState().getClass().getSimpleName()));
    }

    public synchronized void handleReconnect(ServerSideConnection connection, ReconnectMessage message) {
        String nickname = message.getNickname();

        if (nickname == null || nickname.isBlank()) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "Nickname non valido"));
            return;
        }
        if (!gameController.containsGamePlayer(nickname)) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "Player sconosciuto"));
            return;
        }
        if (connections.containsKey(nickname)) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, "Player già connesso"));
            return;
        }
        if (!gameController.isPlayerDisconnected(nickname)) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED,
                    "Player non risulta disconnesso"));
            return;
        }

        try {
            gameController.onPlayerReconnection(nickname);
        } catch (Exception exception) {
            connection.send(new LoginAckMessage(LoginAckMessage.Outcome.REJECTED, exception.getMessage()));
            return;
        }

        registerConnection(nickname, connection);
        connection.send(new LoginAckMessage(LoginAckMessage.Outcome.RECONNECT_OK, null));
        connection.send(new GameSyncMessage(SnapshotBuilder.build(gameController)));
        connection.send(new StateChangedMessage(gameController.getCurrentState().getClass().getSimpleName()));
        if (gameController.isSuspended()) {
            connection.send(new GameSuspendedMessage());
        }
    }

    public synchronized void handleSetPlayersCount(String nickname, SetPlayersCountMessage message) {
        try {
            gameController.onMessageReceived(nickname, new SetPlayerCountAction(message.getCount()));
            broadcastLobbyUpdate();
        } catch (Exception exception) {
            ServerSideConnection connection = connections.get(nickname);
            if (connection != null) {
                connection.send(new ErrorMessage(exception.getMessage()));
            }
        }
    }

    public synchronized void handleReady(String nickname, boolean ready) {
        try {
            boolean gameWasNotStarted = gameController.getGame() == null;
            gameController.onMessageReceived(nickname, new ReadyAction(ready));
            if (gameWasNotStarted && gameController.getGame() != null) {
                broadcastGameStarted();
            } else {
                broadcastLobbyUpdate();
            }
        } catch (Exception exception) {
            ServerSideConnection connection = connections.get(nickname);
            if (connection != null) {
                connection.send(new ErrorMessage(exception.getMessage()));
            }
        }
    }

    public synchronized void handleDisconnect(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return;
        }

        unregisterConnection(nickname);

        if (!gameController.containsLobbyPlayer(nickname) && !gameController.containsGamePlayer(nickname)) {
            return;
        }

        try {
            gameController.onPlayerDisconnection(nickname);
        } catch (Exception ignored) {
            // The controller is the source of truth; if the player was already processed,
            // we do not want a network-side failure to cascade.
        }

        if (gameController.getGame() == null) {
            broadcastLobbyUpdate();
        }
    }

    private void broadcastLobbyUpdate() {
        List<LobbyPlayerSnapshot> players = new ArrayList<>();
        for (Player player : gameController.getLobbyPlayers()) {
            String nickname = player.getNickname();
            players.add(new LobbyPlayerSnapshot(
                    nickname,
                    player.getTotem() != null ? player.getTotem().getColor() : null,
                    gameController.isPlayerReady(nickname),
                    !gameController.isPlayerDisconnected(nickname)
            ));
        }

        LobbyUpdateMessage message = new LobbyUpdateMessage(players, gameController.getChosenPlayersCount());
        for (ServerSideConnection connection : connections.values()) {
            connection.send(message);
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

    public GameController getGameController() {
        return gameController;
    }
}
