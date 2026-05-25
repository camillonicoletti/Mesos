package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.DisconnectMessage;
import it.polimi.ingsw.network.protocol.serverbound.GameActionMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.PongMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReadyMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReconnectMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.network.transport.ClientSideConnection;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * RMI-based implementation of {@link ClientSideConnection}.
 */
public class RMIClientConnection implements ClientSideConnection {

    private static final int HEARTBEAT_TIMEOUT_SECONDS = 60;
    private static final int HEARTBEAT_CHECK_INTERVAL_SECONDS = 5;

    private final RemoteServer remoteServer;
    private final RMIClientCallback callback;
    private final ScheduledExecutorService heartbeatExecutor;
    private final Map<Class<? extends ClientMessage>, RemoteOperation<ClientMessage>> remoteOperations;

    private volatile boolean alive;
    private volatile boolean disconnectNotified;
    private volatile long lastReceivedAtMillis;
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
        this.lastReceivedAtMillis = System.currentTimeMillis();
        this.onMessage = message -> { };
        this.onDisconnect = () -> { };
        this.remoteOperations = createRemoteOperations();
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "rmi-client-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        this.heartbeatExecutor.scheduleAtFixedRate(
                this::heartbeatTick,
                HEARTBEAT_CHECK_INTERVAL_SECONDS,
                HEARTBEAT_CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
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
            invokeRemoteOperation(message);
        } catch (RemoteException exception) {
            alive = false;
            heartbeatExecutor.shutdownNow();
            notifyDisconnect();
            throw new IllegalStateException("Unable to reach the RMI server", exception);
        }
    }

    private void invokeRemoteOperation(ClientMessage message) throws RemoteException {
        RemoteOperation<ClientMessage> operation = remoteOperations.get(message.getClass());
        if (operation == null) {
            throw new IllegalArgumentException("Unsupported RMI operation message: "
                    + message.getClass().getSimpleName());
        }
        operation.invoke(message);
    }

    private Map<Class<? extends ClientMessage>, RemoteOperation<ClientMessage>> createRemoteOperations() {
        Map<Class<? extends ClientMessage>, RemoteOperation<ClientMessage>> operations = new HashMap<>();
        register(operations, LoginMessage.class,
                message -> remoteServer.login(callback, message.getNickname(), message.getTotemColor()));
        register(operations, ReconnectMessage.class,
                message -> remoteServer.reconnect(callback, message.getNickname()));
        register(operations, SetPlayersCountMessage.class,
                message -> remoteServer.setPlayersCount(callback, message.getCount()));
        register(operations, ReadyMessage.class,
                message -> remoteServer.setReady(callback, message.isReady()));
        register(operations, GameActionMessage.class,
                message -> remoteServer.performAction(callback, message.getAction()));
        register(operations, PongMessage.class,
                message -> remoteServer.pong(callback));
        register(operations, DisconnectMessage.class,
                message -> remoteServer.disconnect(callback));
        return Map.copyOf(operations);
    }

    private <T extends ClientMessage> void register(
            Map<Class<? extends ClientMessage>, RemoteOperation<ClientMessage>> operations,
            Class<T> type,
            RemoteOperation<T> operation) {
        operations.put(type, message -> operation.invoke(type.cast(message)));
    }

    @Override
    public void close() {
        if (!alive) {
            heartbeatExecutor.shutdownNow();
            callback.shutdown();
            return;
        }

        alive = false;
        heartbeatExecutor.shutdownNow();
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
        lastReceivedAtMillis = System.currentTimeMillis();
        onMessage.accept(message);
    }

    private void heartbeatTick() {
        if (!alive) {
            return;
        }

        try {
            remoteServer.ping();
            lastReceivedAtMillis = System.currentTimeMillis();
        } catch (RemoteException exception) {
            alive = false;
            heartbeatExecutor.shutdownNow();
            notifyDisconnect();
            return;
        }

        long silenceMillis = System.currentTimeMillis() - lastReceivedAtMillis;
        if (silenceMillis >= HEARTBEAT_TIMEOUT_SECONDS * 1000L) {
            alive = false;
            heartbeatExecutor.shutdownNow();
            notifyDisconnect();
        }
    }

    private void notifyDisconnect() {
        if (disconnectNotified) {
            return;
        }

        disconnectNotified = true;
        onDisconnect.run();
    }

    @FunctionalInterface
    private interface RemoteOperation<T extends ClientMessage> {
        void invoke(T message) throws RemoteException;
    }
}
