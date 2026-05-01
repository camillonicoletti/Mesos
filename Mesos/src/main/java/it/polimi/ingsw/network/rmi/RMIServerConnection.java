package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.transport.ServerSideConnection;

import java.rmi.RemoteException;
import java.util.function.Consumer;

/**
 * Server-side connection adapter used by the RMI transport.
 */
public class RMIServerConnection implements ServerSideConnection {

    private final RemoteClient remoteClient;

    private volatile boolean alive;
    private volatile boolean disconnectNotified;
    private volatile String nickname;
    private volatile Consumer<ClientMessage> onMessage;
    private volatile Runnable onDisconnect;

    public RMIServerConnection(RemoteClient remoteClient) {
        if (remoteClient == null) {
            throw new IllegalArgumentException("Remote client cannot be null");
        }

        this.remoteClient = remoteClient;
        this.alive = true;
        this.disconnectNotified = false;
        this.nickname = null;
        this.onMessage = message -> { };
        this.onDisconnect = () -> { };
    }

    @Override
    public synchronized void send(ServerMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        if (!alive) {
            throw new IllegalStateException("Connection is closed");
        }

        try {
            remoteClient.onServerMessage(message);
        } catch (RemoteException exception) {
            alive = false;
            notifyDisconnect();
            throw new IllegalStateException("Unable to reach the remote client", exception);
        }
    }

    @Override
    public void close() {
        alive = false;
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public String getNickname() {
        return nickname;
    }

    @Override
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public void setOnMessage(Consumer<ClientMessage> onMessage) {
        this.onMessage = onMessage != null ? onMessage : message -> { };
    }

    @Override
    public void setOnDisconnect(Runnable onDisconnect) {
        this.onDisconnect = onDisconnect != null ? onDisconnect : () -> { };
    }

    public void receive(ClientMessage message) {
        if (!alive) {
            throw new IllegalStateException("Connection is closed");
        }
        onMessage.accept(message);
    }

    private void notifyDisconnect() {
        if (disconnectNotified) {
            return;
        }

        disconnectNotified = true;
        onDisconnect.run();
    }
}
