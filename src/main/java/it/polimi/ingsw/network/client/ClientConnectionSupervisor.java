package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.protocol.serverbound.ReconnectMessage;
import it.polimi.ingsw.network.transport.ClientSideConnection;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Periodically recreates the client connection after a recoverable server/network failure. */
final class ClientConnectionSupervisor {
    private static final int RETRY_DELAY_SECONDS = 2;

    private final ClientConnectionFactory connectionFactory;
    private final Consumer<ClientSideConnection> connectionInstaller;
    private ScheduledExecutorService executor;

    ClientConnectionSupervisor(ClientConnectionFactory connectionFactory,
                               Consumer<ClientSideConnection> connectionInstaller) {
        this.connectionFactory = connectionFactory;
        this.connectionInstaller = connectionInstaller;
    }

    synchronized void start(String nickname) {
        if (connectionFactory == null || nickname == null || nickname.isBlank() || isRunning()) {
            return;
        }

        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "client-reconnect-supervisor");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(
                () -> attemptReconnect(nickname),
                0,
                RETRY_DELAY_SECONDS,
                TimeUnit.SECONDS
        );
    }

    synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private synchronized boolean isRunning() {
        return executor != null && !executor.isShutdown();
    }

    private void attemptReconnect(String nickname) {
        ClientSideConnection freshConnection = null;
        try {
            freshConnection = connectionFactory.create();
            connectionInstaller.accept(freshConnection);
            freshConnection.send(new ReconnectMessage(nickname));
            stop();
        } catch (Exception ignored) {
            if (freshConnection != null) {
                freshConnection.close();
            }
        }
    }
}
