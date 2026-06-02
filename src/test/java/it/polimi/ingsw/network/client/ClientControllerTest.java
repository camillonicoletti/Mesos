package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.protocol.clientbound.ErrorMessage;
import it.polimi.ingsw.network.protocol.clientbound.LoginAckMessage;
import it.polimi.ingsw.network.protocol.clientbound.PingMessage;
import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.PongMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReadyMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReconnectMessage;
import it.polimi.ingsw.network.transport.ClientSideConnection;
import it.polimi.ingsw.view.View;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ClientController (network.client).
 *
 * <p>Verifies: client-side handling of server messages and user commands.
 */
class ClientControllerTest {

    @Test
    void registeredHandlerAppliesServerMessageToModel() {
        ClientModel model = new ClientModel();
        FakeConnection connection = new FakeConnection();
        ClientController controller = new ClientController(model, connection);

        controller.onMessage(new ErrorMessage("azione non valida"));

        assertEquals("azione non valida", model.getLastError());
    }

    @Test
    void pingStillSendsPongWithoutTouchingModelDispatcher() {
        ClientModel model = new ClientModel();
        FakeConnection connection = new FakeConnection();
        ClientController controller = new ClientController(model, connection);

        controller.onMessage(new PingMessage());

        assertInstanceOf(PongMessage.class, connection.lastSent);
    }

    @Test
    void failedSendStartsReconnectAndSendsReconnectMessageOnFreshConnection() throws Exception {
        ClientModel model = new ClientModel();
        model.applyLoginAck(new LoginAckMessage(LoginAckMessage.Outcome.LOGIN_OK, null));
        model.setMyNickname("Alice");

        FakeConnection brokenConnection = new FakeConnection();
        brokenConnection.failSend = true;
        FakeConnection recoveredConnection = new FakeConnection();
        CountDownLatch reconnectSent = new CountDownLatch(1);
        recoveredConnection.afterSend = reconnectSent::countDown;

        ClientController controller = new ClientController(
                model,
                brokenConnection,
                () -> recoveredConnection
        );
        RecordingView view = new RecordingView();
        controller.setView(view);

        controller.send(new ReadyMessage(true));

        assertTrue(model.isReconnecting());
        assertEquals(1, view.connectionLostCount);
        assertTrue(reconnectSent.await(2, TimeUnit.SECONDS));
        assertInstanceOf(ReconnectMessage.class, recoveredConnection.lastSent);
        assertEquals("Alice", ((ReconnectMessage) recoveredConnection.lastSent).getNickname());
    }

    @Test
    void serverDisconnectMarksClientAsReconnectingAndNotifiesView() {
        ClientModel model = new ClientModel();
        model.applyLoginAck(new LoginAckMessage(LoginAckMessage.Outcome.LOGIN_OK, null));
        model.setMyNickname("Alice");

        FakeConnection connection = new FakeConnection();
        ClientController controller = new ClientController(model, connection, FakeConnection::new);
        RecordingView view = new RecordingView();
        controller.setView(view);

        connection.simulateServerDown();

        assertTrue(model.isReconnecting());
        assertEquals(1, view.connectionLostCount);
    }

    private static final class FakeConnection implements ClientSideConnection {
        private volatile ClientMessage lastSent;
        private boolean failSend;
        private Runnable afterSend = () -> { };
        private Runnable onDisconnect = () -> { };

        @Override
        public void send(ClientMessage message) {
            if (failSend) {
                throw new IllegalStateException("connection down");
            }
            lastSent = message;
            afterSend.run();
        }

        @Override
        public void close() {
            // Nothing to release in tests.
        }

        @Override
        public void setOnMessage(Consumer<ServerMessage> onMessage) {
            // Tests invoke ClientController.onMessage directly.
        }

        @Override
        public void setOnDisconnect(Runnable onDisconnect) {
            this.onDisconnect = onDisconnect != null ? onDisconnect : () -> { };
        }

        private void simulateServerDown() {
            onDisconnect.run();
        }
    }

    private static final class RecordingView implements View {
        private int connectionLostCount;

        @Override public void run() { }
        @Override public void onLoginResult() { }
        @Override public void onLobbyChanged() { }
        @Override public void onGameStarted() { }
        @Override public void onGameStateChanged() { }
        @Override public void onBoardChanged() { }
        @Override public void onEventResolved() { }
        @Override public void onGameEnded() { }
        @Override public void onError() { }
        @Override public void onConnectionLost() { connectionLostCount++; }
        @Override public void onGameSuspended() { }
        @Override public void onGameResumed() { }
        @Override public void onPlayerConnectionChanged() { }
        @Override public void showLeaderboard(int playersCount, List<it.polimi.ingsw.controller.leaderboard.LeaderboardEntry> entries) { }
        @Override public void showMyRank(int rank, int playersCount) { }
    }
}
