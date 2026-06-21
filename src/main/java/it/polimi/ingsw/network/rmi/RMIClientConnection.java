package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.DisconnectMessage;
import it.polimi.ingsw.network.protocol.serverbound.ForceEndGameMessage;
import it.polimi.ingsw.network.protocol.serverbound.GameActionMessage;
import it.polimi.ingsw.network.protocol.serverbound.GetLeaderboardMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.PongMessage;
import it.polimi.ingsw.network.protocol.serverbound.QuitMessage;
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
 *
 * <p>The rest of the client still speaks in {@link ClientMessage} objects.
 * This adapter translates each message into the matching remote method call.</p>
 */
public class RMIClientConnection implements ClientSideConnection {

    private static final int HEARTBEAT_TIMEOUT_SECONDS = 60;
    private static final int HEARTBEAT_CHECK_INTERVAL_SECONDS = 5;

    private final RemoteServer remoteServer;
    private final RMIClientCallback callback;
    private final ScheduledExecutorService heartbeatExecutor;
    private final Map<Class<? extends ClientMessage>, RemoteCall<ClientMessage>> remoteCalls;

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
        this.remoteCalls = createRemoteCalls();
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

    /**
     * Exports this client callback to the server before the first game message.
     */
    public void register() throws RemoteException {
        remoteServer.register(callback);
    }

    @Override
    public void send(ClientMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        if (!alive) {
            throw new IllegalStateException("Connection is closed");
        }

        try {
            invokeRemoteOperation(message);
        } catch (RemoteException exception) {
            handleConnectionLoss();
            throw new IllegalStateException("Unable to reach the RMI server", exception);
        }
    }

    private void invokeRemoteOperation(ClientMessage message) throws RemoteException {
        RemoteCall<ClientMessage> call = remoteCalls.get(message.getClass());
        if (call == null) {
            throw new IllegalArgumentException("Unsupported RMI operation message: "
                    + message.getClass().getSimpleName());
        }
        call.invoke(message);
    }

    private Map<Class<? extends ClientMessage>, RemoteCall<ClientMessage>> createRemoteCalls() {
        // This map avoids a long if/else chain when a client message must become an RMI call.
        Map<Class<? extends ClientMessage>, RemoteCall<ClientMessage>> calls = new HashMap<>();
        register(calls, LoginMessage.class,
                message -> remoteServer.login(callback, message.getNickname(), message.getTotemColor()));
        register(calls, ReconnectMessage.class,
                message -> remoteServer.reconnect(callback, message.getNickname()));
        register(calls, SetPlayersCountMessage.class,
                message -> remoteServer.setPlayersCount(callback, message.getCount()));
        register(calls, ReadyMessage.class,
                message -> remoteServer.setReady(callback, message.isReady()));
        register(calls, GameActionMessage.class,
                message -> remoteServer.performAction(callback, message.getAction()));
        register(calls, ForceEndGameMessage.class,
                message -> remoteServer.forceEndGame(callback));
        register(calls, GetLeaderboardMessage.class,
                message -> remoteServer.getLeaderboard(callback, message.playersCount(), message.limit()));
        register(calls, QuitMessage.class,
                message -> remoteServer.quit(callback));
        register(calls, PongMessage.class,
                message -> remoteServer.pong(callback));
        register(calls, DisconnectMessage.class,
                message -> remoteServer.disconnect(callback));
        return Map.copyOf(calls);
    }

    private <T extends ClientMessage> void register(
            Map<Class<? extends ClientMessage>, RemoteCall<ClientMessage>> calls,
            Class<T> type,
            RemoteCall<T> call) {
        calls.put(type, message -> call.invoke(type.cast(message)));
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
            // On a normal close we tell the server to remove this callback.
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
        // Server callbacks arrive here and then follow the same path as socket messages.
        lastReceivedAtMillis = System.currentTimeMillis();
        onMessage.accept(message);
    }

    private void heartbeatTick() {
        if (!alive) {
            return;
        }

        try {
            // The client actively calls the server to check that the RMI registry object is alive.
            remoteServer.ping();
            lastReceivedAtMillis = System.currentTimeMillis();
        } catch (RemoteException exception) {
            handleConnectionLoss();
            return;
        }

        long silenceMillis = System.currentTimeMillis() - lastReceivedAtMillis;
        if (silenceMillis >= HEARTBEAT_TIMEOUT_SECONDS * 1000L) {
            handleConnectionLoss();
        }
    }

    private void handleConnectionLoss() {
        if (!alive) {
            return;
        }

        alive = false;
        heartbeatExecutor.shutdownNow();
        // Once the remote server is lost, only local cleanup is possible.
        callback.shutdown();
        notifyDisconnect();
    }

    private void notifyDisconnect() {
        if (disconnectNotified) {
            return;
        }

        disconnectNotified = true;
        onDisconnect.run();
    }

    @FunctionalInterface
    private interface RemoteCall<T extends ClientMessage> {
        void invoke(T message) throws RemoteException;
    }
}
