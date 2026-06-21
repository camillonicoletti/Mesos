package it.polimi.ingsw.network.transport;

import it.polimi.ingsw.network.protocol.clientbound.PingMessage;
import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.PongMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Socket implementation of the client connection.
 *
 * <p>This class owns the socket streams, a reader thread for incoming messages
 * and a small heartbeat check to notice when the server is no longer reachable.</p>
 */
public class SocketClientConnection implements ClientSideConnection {

    private static final int HEARTBEAT_TIMEOUT_SECONDS = 60;
    private static final int HEARTBEAT_CHECK_INTERVAL_SECONDS = 5;

    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private final Thread readerThread;
    private final ScheduledExecutorService heartbeatExecutor;

    private volatile boolean alive;
    private volatile boolean disconnectNotified;
    private volatile long lastReceivedAtMillis;
    // The server can send messages immediately after accepting the socket.
    // If the controller has not installed its handler yet, we keep them here
    // and deliver them as soon as setOnMessage is called.
    private final Object handlerLock = new Object();
    private final List<ServerMessage> pendingMessages = new ArrayList<>();
    private boolean handlerInstalled;
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
        this.handlerInstalled = false;
        this.onMessage = this::bufferUntilHandlerInstalled;
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
            // Only one thread at a time can write on the ObjectOutputStream.
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

        // A normal close does not notify the controller as an unexpected loss.
        alive = false;
        heartbeatExecutor.shutdownNow();
        closeSocketQuietly();
    }

    @Override
    public void setOnMessage(Consumer<ServerMessage> onMessage) {
        Consumer<ServerMessage> handler = onMessage != null ? onMessage : message -> { };
        List<ServerMessage> drain;
        synchronized (handlerLock) {
            // After this point new messages go directly to the real handler.
            drain = new ArrayList<>(pendingMessages);
            pendingMessages.clear();
            this.onMessage = handler;
            handlerInstalled = true;
        }
        for (ServerMessage queued : drain) {
            handler.accept(queued);
        }
    }

    private void bufferUntilHandlerInstalled(ServerMessage message) {
        synchronized (handlerLock) {
            if (!handlerInstalled) {
                pendingMessages.add(message);
                return;
            }
        }
        // setOnMessage won the race while we were taking the lock.
        onMessage.accept(message);
    }

    @Override
    public void setOnDisconnect(Runnable onDisconnect) {
        this.onDisconnect = onDisconnect != null ? onDisconnect : () -> { };
    }

    private void readerLoop() {
        while (alive) {
            try {
                // This thread waits here until the server sends the next object.
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
        // Ping is a transport-level message: it is answered here, not by the controller.
        if (inboundObject instanceof PingMessage) {
            send(new PongMessage());
            return;
        }

        // Real game messages are passed to ClientController.
        if (inboundObject instanceof ServerMessage serverMessage) {
            onMessage.accept(serverMessage);
        }
    }

    private void heartbeatTick() {
        if (!alive) {
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

        // Every unexpected loss passes through here so the callback runs once.
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
}
