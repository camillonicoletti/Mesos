package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.serverbound.GameActionMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.PongMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReadyMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReconnectMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.network.server.LobbyManager;
import it.polimi.ingsw.network.server.MessageDispatcher;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

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
        this.connections = new HashMap<>();
    }

    @Override
    public synchronized void register(RemoteClient client) throws RemoteException {
        connectionFor(client);
    }

    @Override
    public synchronized void login(RemoteClient client, String nickname, TotemColor color) throws RemoteException {
        receive(client, new LoginMessage(nickname, color));
    }

    @Override
    public synchronized void reconnect(RemoteClient client, String nickname) throws RemoteException {
        receive(client, new ReconnectMessage(nickname));
    }

    @Override
    public synchronized void setPlayersCount(RemoteClient client, int count) throws RemoteException {
        receive(client, new SetPlayersCountMessage(count));
    }

    @Override
    public synchronized void setReady(RemoteClient client, boolean ready) throws RemoteException {
        receive(client, new ReadyMessage(ready));
    }

    @Override
    public synchronized void performAction(RemoteClient client, GameAction action) throws RemoteException {
        receive(client, new GameActionMessage(action));
    }

    @Override
    public synchronized void pong(RemoteClient client) throws RemoteException {
        receive(client, new PongMessage());
    }

    @Override
    public synchronized void disconnect(RemoteClient client) {
        if (client == null) {
            return;
        }

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
        connectionFor(client).receive(message);
    }

    private RMIServerConnection connectionFor(RemoteClient client) {
        if (client == null) {
            throw new IllegalArgumentException("Remote client cannot be null");
        }
        return getOrCreateConnection(client);
    }

    private RMIServerConnection getOrCreateConnection(RemoteClient client) {
        RMIServerConnection existing = connections.get(client);
        if (existing != null) {
            return existing;
        }

        RMIServerConnection created = new RMIServerConnection(client);
        created.setOnMessage(message -> dispatcher.dispatch(created, message));
        created.setOnDisconnect(() -> {
            synchronized (RMIServer.this) {
                connections.remove(client);
            }
            lobbyManager.handleConnectionClosed(created);
        });

        System.out.println("New RMI connection: " + client);
        lobbyManager.registerPendingConnection(created);
        connections.put(client, created);
        return created;
    }

}
