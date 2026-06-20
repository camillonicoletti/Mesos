package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.controller.action.SkipExtraPickAction;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.network.protocol.clientbound.DummyServerMessage;
import it.polimi.ingsw.network.protocol.clientbound.LoginAckMessage;
import it.polimi.ingsw.network.protocol.clientbound.PingMessage;
import it.polimi.ingsw.network.protocol.serverbound.ForceEndGameMessage;
import it.polimi.ingsw.network.protocol.serverbound.GameActionMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.PongMessage;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for RMI transport (network.rmi).
 *
 * Verifies: round-trip callback exchange between RMIClientConnection and a remote server stub.
 */
class RMIConnectionTest {

    @Test
    void clientReceivesLoginAckThroughRemoteCallback() throws Exception {
        EchoingRemoteServer server = new EchoingRemoteServer();
        RMIClientConnection client = new RMIClientConnection(server);
        CountDownLatch ackReceived = new CountDownLatch(1);
        AtomicReference<LoginAckMessage> ack = new AtomicReference<>();

        try {
            client.setOnMessage(message -> {
                if (message instanceof LoginAckMessage loginAck) {
                    ack.set(loginAck);
                    ackReceived.countDown();
                }
            });

            client.send(new LoginMessage("RmiSmoke", TotemColor.RED));

            assertTrue(ackReceived.await(2, TimeUnit.SECONDS));
            assertEquals(LoginAckMessage.Outcome.LOGIN_OK, ack.get().getOutcome());
        } finally {
            client.close();
        }
    }

    @Test
    void clientCanAnswerPingWhileActionSendIsWaitingForServer() throws Exception {
        PingDuringActionServer server = new PingDuringActionServer();
        RMIClientConnection client = new RMIClientConnection(server);
        CountDownLatch actionReturned = new CountDownLatch(1);

        try {
            client.setOnMessage(message -> {
                if (message instanceof PingMessage) {
                    client.send(new PongMessage());
                }
            });

            Thread actionThread = new Thread(() -> {
                try {
                    client.send(new GameActionMessage(new SkipExtraPickAction()));
                } finally {
                    actionReturned.countDown();
                }
            }, "rmi-client-action-send-test");
            actionThread.start();

            assertTrue(actionReturned.await(1, TimeUnit.SECONDS),
                    "The client must be able to send Pong while an action call is still waiting");
            assertTrue(server.pongReceived.await(1, TimeUnit.SECONDS),
                    "The fake server should receive the Pong produced inside the callback");
            assertTrue(server.pongReceivedBeforeActionReturned.get(),
                    "The Pong must arrive before the action call returns, otherwise the callback was blocked");
            assertTrue(server.callbackReturned.get(),
                    "The callback should not be blocked by the client's action send lock");
        } finally {
            client.close();
        }
    }

    @Test
    void clientHandlerFailureDoesNotPropagateAsRemoteCallbackFailure() throws Exception {
        CallbackErrorServer server = new CallbackErrorServer();
        RMIClientConnection client = new RMIClientConnection(server);

        try {
            client.setOnMessage(message -> {
                throw new IllegalStateException("client-side render failed");
            });

            assertDoesNotThrow(() -> client.send(new LoginMessage("RmiSmoke", TotemColor.RED)));
            assertTrue(server.callbackReturned.await(1, TimeUnit.SECONDS),
                    "The server callback should return even if the local client handler fails");
        } finally {
            client.close();
        }
    }

    @Test
    void clientMapsForceEndGameMessageToRemoteCall() throws Exception {
        ForceEndGameRecordingServer server = new ForceEndGameRecordingServer();
        RMIClientConnection client = new RMIClientConnection(server);

        try {
            client.send(new ForceEndGameMessage());

            assertTrue(server.forceEndGameCalled.get(),
                    "ForceEndGameMessage must be supported by the RMI client adapter");
        } finally {
            client.close();
        }
    }

    private static class EchoingRemoteServer implements RemoteServer {
        @Override
        public void register(RemoteClient client) {
        }

        @Override
        public void login(RemoteClient client, String nickname, TotemColor color) throws RemoteException {
            if ("RmiSmoke".equals(nickname) && color == TotemColor.RED) {
                client.onServerMessage(new LoginAckMessage(LoginAckMessage.Outcome.LOGIN_OK, null));
            }
        }

        @Override
        public void reconnect(RemoteClient client, String nickname) {
        }

        @Override
        public void setPlayersCount(RemoteClient client, int count) {
        }

        @Override
        public void setReady(RemoteClient client, boolean ready) {
        }

        @Override
        public void performAction(RemoteClient client, GameAction action) {
        }

        @Override
        public void forceEndGame(RemoteClient client) {
        }

        @Override
        public void getLeaderboard(RemoteClient client, int playersCount, int limit) {
        }

        @Override
        public void quit(RemoteClient client) {
        }

        @Override
        public void pong(RemoteClient client) {
        }

        @Override
        public void disconnect(RemoteClient client) {
        }

        @Override
        public void ping() {
        }
    }

    private static class PingDuringActionServer extends EchoingRemoteServer {
        private final CountDownLatch pongReceived = new CountDownLatch(1);
        private final AtomicBoolean callbackReturned = new AtomicBoolean(false);
        private final AtomicBoolean pongReceivedBeforeActionReturned = new AtomicBoolean(false);

        @Override
        public void performAction(RemoteClient client, GameAction action) {
            Thread callbackThread = new Thread(() -> {
                try {
                    client.onServerMessage(new PingMessage());
                    callbackReturned.set(true);
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            }, "rmi-server-callback-test");
            callbackThread.start();

            try {
                pongReceivedBeforeActionReturned.set(pongReceived.await(300, TimeUnit.MILLISECONDS));
                callbackThread.join(500);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void pong(RemoteClient client) {
            pongReceived.countDown();
        }
    }

    private static class CallbackErrorServer extends EchoingRemoteServer {
        private final CountDownLatch callbackReturned = new CountDownLatch(1);

        @Override
        public void login(RemoteClient client, String nickname, TotemColor color) throws RemoteException {
            client.onServerMessage(new DummyServerMessage("callback"));
            callbackReturned.countDown();
        }
    }

    private static class ForceEndGameRecordingServer extends EchoingRemoteServer {
        private final AtomicBoolean forceEndGameCalled = new AtomicBoolean(false);

        @Override
        public void forceEndGame(RemoteClient client) {
            forceEndGameCalled.set(true);
        }
    }
}
