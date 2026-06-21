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
 * Socket implementation of the server connection for one client.
 *
 * <p>Each accepted socket gets its own reader thread. The heartbeat sends Ping
 * messages and closes the connection if the client stops answering.</p>
 */
public class SocketServerConnection implements ServerSideConnection {

    private static final int HEARTBEAT_INTERVAL_SECONDS = 5;
    private static final int HEARTBEAT_TIMEOUT_SECONDS = 60;

    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private final Thread readerThread;
    private final ScheduledExecutorService heartbeatExecutor;

    private volatile boolean alive;
    private volatile boolean disconnectNotified;
    private volatile long lastReceivedAtMillis;
    private volatile String nickname;
    private volatile Consumer<ClientMessage> onMessage;
    private volatile Runnable onDisconnect;

    /**
     * Creates a server-side connection around an already accepted socket.
     *
     * @param socket the connected client socket
     * @throws IOException if input or output streams cannot be created
     */
    public SocketServerConnection(Socket socket) throws IOException {
        if (socket == null) {
            throw new IllegalArgumentException("Socket cannot be null");
        }

        this.socket = socket;
        this.alive = true;
        this.disconnectNotified = false;
        this.lastReceivedAtMillis = System.currentTimeMillis();
        this.nickname = null;
        this.onMessage = message -> { };
        this.onDisconnect = () -> { };
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
        this.readerThread = new Thread(this::readerLoop, "socket-server-reader");
        this.readerThread.setDaemon(true);
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "socket-server-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        this.readerThread.start();
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
            // The output stream is shared by game messages and heartbeat pings.
            out.writeObject(message);
            out.flush();
            out.reset();
        } catch (IOException exception) {
            handleConnectionLoss();
        }
    }

    @Override
    public void close() {
        if (!alive) {
            return;
        }

        // This is an intentional close, so no disconnect callback is fired here.
        alive = false;
        heartbeatExecutor.shutdownNow();
        closeSocketQuietly();
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

    private void readerLoop() {
        while (alive) {
            try {
                // This blocks until the client sends the next object.
                Object inboundObject = in.readObject();
                lastReceivedAtMillis = System.currentTimeMillis();
                handleInboundObject(inboundObject);
            } catch (IOException | ClassNotFoundException exception) {
                if (!alive) {
                    return;
                }

                handleConnectionLoss();
                return;
            }
        }
    }

    private void handleInboundObject(Object inboundObject) {
        // Pong only confirms that the client is alive.
        if (inboundObject instanceof PongMessage) {
            return;
        }

        // Real client commands are forwarded to MessageDispatcher.
        if (inboundObject instanceof ClientMessage clientMessage) {
            onMessage.accept(clientMessage);
        }
    }

    private void handleConnectionLoss() {
        if (!alive) {
            return;
        }

        // Unexpected socket failures are centralized here.
        alive = false;
        heartbeatExecutor.shutdownNow();
        closeSocketQuietly();
        notifyDisconnect();
    }

    private void closeSocketQuietly() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
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

        // If no object arrives for too long, the client is considered gone.
        long silenceMillis = System.currentTimeMillis() - lastReceivedAtMillis;
        if (silenceMillis >= HEARTBEAT_TIMEOUT_SECONDS * 1000L) {
            handleConnectionLoss();
            return;
        }

        try {
            send(new PingMessage());
        } catch (IllegalStateException ignored) {
        }
    }
}
