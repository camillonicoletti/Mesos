package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.network.protocol.clientbound.PingMessage;
import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.PongMessage;
import it.polimi.ingsw.network.transport.ServerSideConnection;

import java.rmi.RemoteException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Server-side connection adapter used by the RMI transport.
 */
public class RMIServerConnection implements ServerSideConnection {

    private static final int HEARTBEAT_INTERVAL_SECONDS = 5;
    private static final int HEARTBEAT_TIMEOUT_SECONDS = 60;

    private final RemoteClient remoteClient;
    private final ScheduledExecutorService heartbeatExecutor;

    private volatile boolean alive;
    private volatile boolean disconnectNotified;
    private volatile long lastReceivedAtMillis;
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
        this.lastReceivedAtMillis = System.currentTimeMillis();
        this.nickname = null;
        this.onMessage = message -> { };
        this.onDisconnect = () -> { };
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "rmi-server-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        this.heartbeatExecutor.scheduleAtFixedRate(
                this::heartbeatTick,
                HEARTBEAT_INTERVAL_SECONDS,
                HEARTBEAT_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
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
            heartbeatExecutor.shutdownNow();
            notifyDisconnect();
            throw new IllegalStateException("Unable to reach the remote client", exception);
        }
    }

    @Override
    public void close() {
        alive = false;
        heartbeatExecutor.shutdownNow();
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
        lastReceivedAtMillis = System.currentTimeMillis();
        if (message instanceof PongMessage) {
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

    private void heartbeatTick() {
        if (!alive) {
            return;
        }

        long silenceMillis = System.currentTimeMillis() - lastReceivedAtMillis;
        if (silenceMillis >= HEARTBEAT_TIMEOUT_SECONDS * 1000L) {
            alive = false;
            heartbeatExecutor.shutdownNow();
            notifyDisconnect();
            return;
        }

        try {
            send(new PingMessage());
        } catch (IllegalStateException ignored) {
        }
    }
}
