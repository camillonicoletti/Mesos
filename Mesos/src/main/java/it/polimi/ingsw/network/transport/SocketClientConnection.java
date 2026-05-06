package it.polimi.ingsw.network.transport;

import it.polimi.ingsw.network.protocol.clientbound.PingMessage;
import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.PongMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Socket-based implementation of a client-side connection.
 * It wraps the socket used by the client to communicate with the server.
 */
public class SocketClientConnection implements ClientSideConnection {

    private static final int HEARTBEAT_TIMEOUT_SECONDS = 15;
    private static final int HEARTBEAT_CHECK_INTERVAL_SECONDS = 5;

    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private final Thread readerThread;
    private final ScheduledExecutorService heartbeatExecutor;

    private volatile boolean alive;
    private volatile boolean disconnectNotified;
    private volatile long lastReceivedAtMillis;
    private volatile Consumer<ServerMessage> onMessage;
    private volatile Runnable onDisconnect;

    /**
     * Creates a client-side connection around an already connected socket.
     *
     * @param socket the socket connected to the server
     * @throws IOException if input or output streams cannot be created
     */
    public SocketClientConnection(Socket socket) throws IOException {
        if (socket == null) {
            throw new IllegalArgumentException("Socket cannot be null");
        }

        this.socket = socket;
        this.alive = true;
        this.disconnectNotified = false;
        this.lastReceivedAtMillis = System.currentTimeMillis();
        this.onMessage = message -> { };
        this.onDisconnect = () -> { };
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
        this.readerThread = new Thread(this::readerLoop, "socket-client-reader");
        this.readerThread.setDaemon(true);
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "socket-client-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        this.readerThread.start();
        this.heartbeatExecutor.scheduleAtFixedRate(
                this::heartbeatTick,
                HEARTBEAT_CHECK_INTERVAL_SECONDS,
                HEARTBEAT_CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
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
            out.writeObject(message);
            out.flush();
            out.reset();
        } catch (IOException exception) {
            alive = false;
            heartbeatExecutor.shutdownNow();
            notifyDisconnect();
        }
    }

    @Override
    public void close() {
        if (!alive) {
            return;
        }

        alive = false;
        heartbeatExecutor.shutdownNow();
        try {
            socket.close();
        } catch (IOException ignored) {
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

    private void readerLoop() {
        while (alive) {
            try {
                Object inboundObject = in.readObject();
                lastReceivedAtMillis = System.currentTimeMillis();
                if (inboundObject instanceof PingMessage) {
                    send(new PongMessage());
                    continue;
                }
                if (inboundObject instanceof ServerMessage serverMessage) {
                    onMessage.accept(serverMessage);
                }
            } catch (IOException | ClassNotFoundException exception) {
                if (!alive) {
                    return;
                }

                alive = false;
                heartbeatExecutor.shutdownNow();
                notifyDisconnect();
                return;
            }
        }
    }

    private void heartbeatTick() {
        if (!alive) {
            return;
        }

        long silenceMillis = System.currentTimeMillis() - lastReceivedAtMillis;
        if (silenceMillis >= HEARTBEAT_TIMEOUT_SECONDS * 1000L) {
            alive = false;
            heartbeatExecutor.shutdownNow();
            try {
                socket.close();
            } catch (IOException ignored) {
            }
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
}
