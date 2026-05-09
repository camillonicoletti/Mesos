package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
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
        if (client == null) {
            throw new IllegalArgumentException("Remote client cannot be null");
        }
        getOrCreateConnection(client);
    }

    @Override
    public synchronized void send(RemoteClient client, ClientMessage message) throws RemoteException {
        if (client == null) {
            throw new IllegalArgumentException("Remote client cannot be null");
        }
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }

        RMIServerConnection connection = getOrCreateConnection(client);
        connection.receive(message);
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
        if (connection.getNickname() != null) {
            lobbyManager.handleDisconnect(connection.getNickname());
        } else {
            lobbyManager.unregisterPendingConnection(connection);
        }
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
            if (created.getNickname() != null) {
                lobbyManager.handleDisconnect(created.getNickname());
            } else {
                lobbyManager.unregisterPendingConnection(created);
            }
        });

        System.out.println("New RMI connection: " + client);
        lobbyManager.registerPendingConnection(created);
        connections.put(client, created);
        return created;
    }
}
