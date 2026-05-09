package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.transport.ClientSideConnection;

import java.rmi.RemoteException;
import java.util.function.Consumer;

/**
 * RMI-based implementation of {@link ClientSideConnection}.
 */
public class RMIClientConnection implements ClientSideConnection {

    private final RemoteServer remoteServer;
    private final RMIClientCallback callback;

    private volatile boolean alive;
    private volatile boolean disconnectNotified;
    private volatile Consumer<ServerMessage> onMessage;
    private volatile Runnable onDisconnect;

    public RMIClientConnection(RemoteServer remoteServer) throws RemoteException {
        if (remoteServer == null) {
            throw new IllegalArgumentException("Remote server cannot be null");
        }

        this.remoteServer = remoteServer;
        this.callback = new RMIClientCallback(this);
        this.alive = true;
        this.disconnectNotified = false;
        this.onMessage = message -> { };
        this.onDisconnect = () -> { };

    }
    public void register() throws RemoteException {
        remoteServer.register(callback);
    }

    @Override
    public synchronized void send(ClientMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        if (!alive) {
            throw new IllegalStateException("Connection is closed");
        }

        try {
            remoteServer.send(callback, message);
        } catch (RemoteException exception) {
            alive = false;
            notifyDisconnect();
            throw new IllegalStateException("Unable to reach the RMI server", exception);
        }
    }

    @Override
    public void close() {
        if (!alive) {
            callback.shutdown();
            return;
        }

        alive = false;
        try {
            remoteServer.disconnect(callback);
        } catch (RemoteException ignored) {
            // The server is already unreachable; local shutdown must still complete.
        } finally {
            callback.shutdown();
        }
    }

    @Override
    public void setOnMessage(Consumer<ServerMessage> onMessage) {
        this.onMessage = onMessage != null ? onMessage : message -> { };
    }

    @Override
    public void setOnDisconnect(Runnable onDisconnect) {
        this.onDisconnect = onDisconnect != null ? onDisconnect : () -> { };
    }

    void handleInbound(ServerMessage message) {
        if (!alive) {
            return;
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
