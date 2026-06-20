package it.polimi.ingsw.network.transport;

import it.polimi.ingsw.network.protocol.clientbound.DummyServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.DummyClientMessage;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Lifecycle tests for the Socket transport (SocketClientConnection,
 * SocketServerConnection): socket round-trip ordering across multiple
 * messages, and the setOnDisconnect notification path that fires when
 * either side closes its socket.
 *
 * All tests run on a local loopback connection bound to an OS-allocated
 * ephemeral port (ServerSocket(0)). Synchronization is deterministic via
 * CountDownLatch — no Thread.sleep, no fixed ports, no external network,
 * no reflection.
 */
class SocketConnectionLifecycleTest {

    private static final long AWAIT_TIMEOUT_SECONDS = 5L;

    /**
     * Three messages sent in sequence by the client must be received in the
     * same order on the server side. Guards the assumption that
     * ObjectOutputStream/ObjectInputStream preserve FIFO order on a single
     * socket.
     */
    @Test
    void socketExchangesMultipleMessagesInOrder() throws Exception {
        CountDownLatch accepted = new CountDownLatch(1);
        CountDownLatch allReceived = new CountDownLatch(3);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<SocketServerConnection> serverConnectionRef = new AtomicReference<>();
        List<String> receivedOrder = new ArrayList<>();

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            ExecutorService acceptExecutor = Executors.newSingleThreadExecutor();
            try {
                acceptExecutor.submit(() -> {
                    try {
                        Socket acceptedSocket = serverSocket.accept();
                        SocketServerConnection serverConnection = new SocketServerConnection(acceptedSocket);
                        serverConnectionRef.set(serverConnection);
                        serverConnection.setOnMessage(message -> {
                            if (message instanceof DummyClientMessage dummy) {
                                synchronized (receivedOrder) {
                                    receivedOrder.add(dummy.getPayload());
                                }
                                allReceived.countDown();
                            }
                        });
                        accepted.countDown();
                    } catch (Throwable throwable) {
                        failure.set(throwable);
                        accepted.countDown();
                    }
                });

                SocketClientConnection client = new SocketClientConnection(
                        new Socket("127.0.0.1", serverSocket.getLocalPort()));
                try {
                    client.setOnMessage(message -> { });

                    assertTrue(accepted.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                            "Server must accept the connection.");
                    if (failure.get() != null) {
                        fail(failure.get());
                    }

                    client.send(new DummyClientMessage("msg-1"));
                    client.send(new DummyClientMessage("msg-2"));
                    client.send(new DummyClientMessage("msg-3"));

                    assertTrue(allReceived.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                            "All three messages must be received by the server.");
                    synchronized (receivedOrder) {
                        assertEquals(List.of("msg-1", "msg-2", "msg-3"), receivedOrder,
                                "Messages must be delivered in the order they were sent.");
                    }
                } finally {
                    client.close();
                    SocketServerConnection serverConnection = serverConnectionRef.get();
                    if (serverConnection != null) {
                        serverConnection.close();
                    }
                }
            } finally {
                acceptExecutor.shutdownNow();
            }
        }
    }

    /**
     * When the client closes its socket, the server-side reader detects the
     * end of the stream and must invoke the onDisconnect callback previously
     * registered via setOnDisconnect. This is the server-side hook used by
     * the lobby to mark a player as disconnected.
     */
    @Test
    void serverIsNotifiedWhenClientClosesSocket() throws Exception {
        CountDownLatch accepted = new CountDownLatch(1);
        CountDownLatch serverDisconnected = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<SocketServerConnection> serverConnectionRef = new AtomicReference<>();

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            ExecutorService acceptExecutor = Executors.newSingleThreadExecutor();
            try {
                acceptExecutor.submit(() -> {
                    try {
                        Socket acceptedSocket = serverSocket.accept();
                        SocketServerConnection serverConnection = new SocketServerConnection(acceptedSocket);
                        serverConnectionRef.set(serverConnection);
                        serverConnection.setOnDisconnect(serverDisconnected::countDown);
                        accepted.countDown();
                    } catch (Throwable throwable) {
                        failure.set(throwable);
                        accepted.countDown();
                    }
                });

                SocketClientConnection client = new SocketClientConnection(
                        new Socket("127.0.0.1", serverSocket.getLocalPort()));
                try {
                    client.setOnMessage(message -> { });

                    assertTrue(accepted.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                            "Server must accept the connection before the client closes it.");
                    if (failure.get() != null) {
                        fail(failure.get());
                    }
                } finally {
                    client.close();
                }

                assertTrue(serverDisconnected.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                        "Server must receive an onDisconnect notification when the client closes.");
            } finally {
                SocketServerConnection serverConnection = serverConnectionRef.get();
                if (serverConnection != null) {
                    serverConnection.close();
                }
                acceptExecutor.shutdownNow();
            }
        }
    }

    /**
     * Symmetric to the previous test: when the server closes its socket, the
     * client-side reader detects the end of the stream and must invoke the
     * onDisconnect callback previously registered via setOnDisconnect. This
     * is the client-side hook used by the UI to surface "connection lost"
     * feedback.
     */
    @Test
    void clientIsNotifiedWhenServerClosesSocket() throws Exception {
        CountDownLatch accepted = new CountDownLatch(1);
        CountDownLatch clientDisconnected = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<SocketServerConnection> serverConnectionRef = new AtomicReference<>();

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            ExecutorService acceptExecutor = Executors.newSingleThreadExecutor();
            try {
                acceptExecutor.submit(() -> {
                    try {
                        Socket acceptedSocket = serverSocket.accept();
                        SocketServerConnection serverConnection = new SocketServerConnection(acceptedSocket);
                        serverConnectionRef.set(serverConnection);
                        accepted.countDown();
                    } catch (Throwable throwable) {
                        failure.set(throwable);
                        accepted.countDown();
                    }
                });

                SocketClientConnection client = new SocketClientConnection(
                        new Socket("127.0.0.1", serverSocket.getLocalPort()));
                try {
                    client.setOnMessage(message -> { });
                    client.setOnDisconnect(clientDisconnected::countDown);

                    assertTrue(accepted.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                            "Server must accept the connection before it closes.");
                    if (failure.get() != null) {
                        fail(failure.get());
                    }

                    SocketServerConnection serverConnection = serverConnectionRef.get();
                    if (serverConnection != null) {
                        serverConnection.close();
                    }

                    assertTrue(clientDisconnected.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                            "Client must receive an onDisconnect notification when the server closes.");
                } finally {
                    client.close();
                }
            } finally {
                acceptExecutor.shutdownNow();
            }
        }
    }
}
