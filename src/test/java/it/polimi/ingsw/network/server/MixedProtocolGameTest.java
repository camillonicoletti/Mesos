package it.polimi.ingsw.network.server;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.GameStartedMessage;
import it.polimi.ingsw.network.protocol.clientbound.LoginAckMessage;
import it.polimi.ingsw.network.protocol.clientbound.LoginResult;
import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.network.rmi.RMIServer;
import it.polimi.ingsw.network.rmi.RemoteClient;
import it.polimi.ingsw.network.transport.SocketClientConnection;
import it.polimi.ingsw.network.transport.SocketServerConnection;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the README's claim that a single server instance accepts players
 * connected through different network technologies into the same match.
 * Alice logs in over a real Socket connection; Bob logs in over RMI. Both
 * are routed into one shared LobbyManager, wired the way ServerLauncher
 * wires them in production: one lobby, one RMIServer bound to it, one
 * socket-accept path feeding the same MessageDispatcher. Starting the game
 * must broadcast GameStartedMessage to both, regardless of transport.
 *
 * No real RMI registry is used: the RMIServer object is constructed
 * directly and its methods are called as plain Java calls, equivalent from
 * the lobby's point of view since the registry is only a network lookup
 * step. The Socket side goes through a real loopback socket on an
 * OS-allocated ephemeral port. Deterministic throughout: no Thread.sleep.
 */
class MixedProtocolGameTest {

    private static final long AWAIT_TIMEOUT_SECONDS = 5L;

    @Test
    void socketAndRmiPlayersJoinAndStartTheSameGameTogether() throws Exception {
        LobbyManager lobby = new LobbyManager();
        MessageDispatcher dispatcher = new MessageDispatcher(lobby);
        RMIServer rmiServer = new RMIServer(lobby);

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            ExecutorService acceptExecutor = Executors.newSingleThreadExecutor();
            CountDownLatch socketAccepted = new CountDownLatch(1);
            AtomicReference<SocketServerConnection> socketServerSide = new AtomicReference<>();
            AtomicReference<Throwable> acceptFailure = new AtomicReference<>();

            // Mirrors SocketAcceptLoop.wireConnection exactly: the same wiring
            // ServerLauncher uses for every real socket client.
            acceptExecutor.submit(() -> {
                try {
                    Socket accepted = serverSocket.accept();
                    SocketServerConnection serverConnection = new SocketServerConnection(accepted);
                    serverConnection.setOnMessage(message -> dispatcher.dispatch(serverConnection, message));
                    serverConnection.setOnDisconnect(() -> lobby.handleConnectionClosed(serverConnection));
                    lobby.registerPendingConnection(serverConnection);
                    socketServerSide.set(serverConnection);
                } catch (Throwable t) {
                    acceptFailure.set(t);
                } finally {
                    socketAccepted.countDown();
                }
            });

            CountDownLatch aliceGameStarted = new CountDownLatch(1);
            SocketClientConnection aliceConnection = new SocketClientConnection(
                    new Socket("127.0.0.1", serverSocket.getLocalPort()));
            aliceConnection.setOnMessage(message -> {
                if (message instanceof GameStartedMessage) {
                    aliceGameStarted.countDown();
                }
            });

            try {
                assertTrue(socketAccepted.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                        "Server must accept Alice's socket connection");
                if (acceptFailure.get() != null) {
                    throw new AssertionError(acceptFailure.get());
                }

                // ---- Alice joins over Socket ----
                aliceConnection.send(new LoginMessage("Alice", TotemColor.RED));

                // ---- Bob joins the SAME lobby over RMI ----
                CollectingRemoteClient bob = new CollectingRemoteClient();
                rmiServer.register(bob);
                rmiServer.login(bob, "Bob", TotemColor.BLUE);

                assertTrue(bob.awaitLoginAck(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                        "Bob must receive a login acknowledgement over RMI");
                LoginAckMessage bobAck = bob.firstOfType(LoginAckMessage.class);
                assertInstanceOf(LoginResult.LoginOk.class, bobAck.getResult(),
                        "Bob's RMI login must be accepted into the same lobby Alice joined over Socket");

                // Both players are now in the SAME lobby, regardless of transport.
                // Starting the game must reach both of them.
                lobby.handleSetPlayersCount("Alice", new SetPlayersCountMessage(2));
                lobby.handleReady("Alice", true);
                lobby.handleReady("Bob", true);

                assertTrue(aliceGameStarted.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                        "Alice (Socket) must receive GameStartedMessage");
                assertTrue(bob.awaitGameStarted(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                        "Bob (RMI) must receive GameStartedMessage from the same game Alice just started");
            } finally {
                aliceConnection.close();
                SocketServerConnection serverSide = socketServerSide.get();
                if (serverSide != null) {
                    serverSide.close();
                }
                acceptExecutor.shutdownNow();
                UnicastRemoteObject.unexportObject(rmiServer, true);
                lobby.shutdown();
            }
        }
    }

    /**
     * Local RemoteClient stub: callbacks are plain Java calls (no RMI
     * marshalling), consistent with RMIServerTest's CollectingClient.
     */
    private static class CollectingRemoteClient implements RemoteClient {
        private final List<ServerMessage> received = new ArrayList<>();
        private final CountDownLatch loginAck = new CountDownLatch(1);
        private final CountDownLatch gameStarted = new CountDownLatch(1);

        @Override
        public synchronized void onServerMessage(ServerMessage message) {
            received.add(message);
            if (message instanceof LoginAckMessage) {
                loginAck.countDown();
            }
            if (message instanceof GameStartedMessage) {
                gameStarted.countDown();
            }
        }

        boolean awaitLoginAck(long timeout, TimeUnit unit) throws InterruptedException {
            return loginAck.await(timeout, unit);
        }

        boolean awaitGameStarted(long timeout, TimeUnit unit) throws InterruptedException {
            return gameStarted.await(timeout, unit);
        }

        synchronized <T extends ServerMessage> T firstOfType(Class<T> type) {
            return received.stream()
                    .filter(type::isInstance)
                    .map(type::cast)
                    .findFirst()
                    .orElseThrow();
        }
    }
}
