package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.serverbound.GameActionMessage;
import it.polimi.ingsw.network.protocol.serverbound.ForceEndGameMessage;
import it.polimi.ingsw.network.protocol.serverbound.GetLeaderboardMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.PongMessage;
import it.polimi.ingsw.network.protocol.serverbound.QuitMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReadyMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReconnectMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.network.server.LobbyManager;
import it.polimi.ingsw.network.server.MessageDispatcher;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RMI server endpoint that adapts remote invocations to the existing
 * {@link LobbyManager}/{@link MessageDispatcher} pipeline.
 */
public class RMIServer extends UnicastRemoteObject implements RemoteServer {

    private final LobbyManager lobbyManager;
    private final MessageDispatcher dispatcher;
    private final Map<RemoteClient, RMIServerConnection> connections;

    public RMIServer() throws RemoteException {
        this(new LobbyManager());
    }

    public RMIServer(LobbyManager lobbyManager) throws RemoteException {
        super();
        if (lobbyManager == null) {
            throw new IllegalArgumentException("Lobby manager cannot be null");
        }
        this.lobbyManager = lobbyManager;
        this.dispatcher = new MessageDispatcher(lobbyManager);
        this.connections = new ConcurrentHashMap<>();
    }

    @Override
    public void register(RemoteClient client) throws RemoteException {
        // Registration creates the same server-side connection object used by the lobby.
        connectionFor(client);
    }

    @Override
    public void login(RemoteClient client, String nickname, TotemColor color) throws RemoteException {
        // Remote calls are converted back into our normal protocol DTOs.
        receive(client, new LoginMessage(nickname, color));
    }

    @Override
    public void reconnect(RemoteClient client, String nickname) throws RemoteException {
        receive(client, new ReconnectMessage(nickname));
    }

    @Override
    public void setPlayersCount(RemoteClient client, int count) throws RemoteException {
        receive(client, new SetPlayersCountMessage(count));
    }

    @Override
    public void setReady(RemoteClient client, boolean ready) throws RemoteException {
        receive(client, new ReadyMessage(ready));
    }

    @Override
    public void performAction(RemoteClient client, GameAction action) throws RemoteException {
        receive(client, new GameActionMessage(action));
    }

    @Override
    public void forceEndGame(RemoteClient client) throws RemoteException {
        receive(client, new ForceEndGameMessage());
    }

    @Override
    public void getLeaderboard(RemoteClient client, int playersCount, int limit) throws RemoteException {
        receive(client, new GetLeaderboardMessage(playersCount, limit));
    }

    @Override
    public void quit(RemoteClient client) throws RemoteException {
        receive(client, new QuitMessage());
    }

    @Override
    public void pong(RemoteClient client) throws RemoteException {
        receive(client, new PongMessage());
    }

    @Override
    public void disconnect(RemoteClient client) {
        if (client == null) {
            return;
        }

        // Explicit disconnect is the RMI equivalent of closing a socket.
        RMIServerConnection connection = connections.remove(client);
        if (connection == null) {
            return;
        }

        connection.close();
        lobbyManager.handleConnectionClosed(connection);
    }

    @Override
    public void ping() {
        // No-op: clients use this only to verify the server is reachable.
    }

    private void receive(RemoteClient client, ClientMessage message) {
        // From this point on, RMI and socket clients share the same dispatcher path (RMIServerConnnection).
        connectionFor(client).receive(message);
    }

    private RMIServerConnection connectionFor(RemoteClient client) {
        if (client == null) {
            throw new IllegalArgumentException("Remote client cannot be null");
        }
        return getOrCreateConnection(client);
    }

    private RMIServerConnection getOrCreateConnection(RemoteClient client) {
        // One exported client callback corresponds to one logical connection.
        return connections.computeIfAbsent(client, this::createConnection);
    }

    private RMIServerConnection createConnection(RemoteClient client) {
        RMIServerConnection created = new RMIServerConnection(client);
        // Wire the RMI adapter to the same lobby and dispatcher used by sockets.
        created.setOnMessage(message -> dispatcher.dispatch(created, message));
        created.setOnDisconnect(() -> {
            connections.remove(client, created);
            lobbyManager.handleConnectionClosed(created);
        });

        System.out.println("New RMI connection: " + client);
        lobbyManager.registerPendingConnection(created);
        return created;
    }

}
