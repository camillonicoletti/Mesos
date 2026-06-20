package it.polimi.ingsw.network.server;

import it.polimi.ingsw.network.protocol.clientbound.ErrorMessage;
import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReadyMessage;
import it.polimi.ingsw.network.transport.ServerSideConnection;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Unit tests for MessageDispatcher (network.server).
 *
 * Verifies: routing of client messages to the LobbyManager.
 */
class MessageDispatcherTest {

    @Test
    void registeredHandlerRoutesMessageToLobbyManager() {
        RecordingLobby lobby = new RecordingLobby();
        MessageDispatcher dispatcher = new MessageDispatcher(lobby);
        FakeConnection connection = new FakeConnection("milo");

        dispatcher.dispatch(connection, new ReadyMessage(true));

        assertEquals("milo", lobby.readyNickname);
        assertEquals(true, lobby.readyValue);
    }

    @Test
    void handlerExceptionIsReportedToClient() {
        RecordingLobby lobby = new RecordingLobby();
        lobby.throwOnReady = true;
        MessageDispatcher dispatcher = new MessageDispatcher(lobby);
        FakeConnection connection = new FakeConnection("milo");

        dispatcher.dispatch(connection, new ReadyMessage(true));

        ErrorMessage error = assertInstanceOf(ErrorMessage.class, connection.lastSent);
        assertEquals("boom", error.getReason());
    }

    private static final class RecordingLobby extends LobbyManager {
        private String readyNickname;
        private Boolean readyValue;
        private boolean throwOnReady;

        @Override
        public synchronized void handleReady(String nickname, boolean ready) {
            if (throwOnReady) {
                throw new IllegalStateException("boom");
            }
            readyNickname = nickname;
            readyValue = ready;
        }
    }

    private static final class FakeConnection implements ServerSideConnection {
        private String nickname;
        private ServerMessage lastSent;

        private FakeConnection(String nickname) {
            this.nickname = nickname;
        }

        @Override
        public void send(ServerMessage message) {
            lastSent = message;
        }

        @Override
        public void close() {
            // Nothing to release in tests.
        }

        @Override
        public boolean isAlive() {
            return true;
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
            // Tests call dispatch directly.
        }

        @Override
        public void setOnDisconnect(Runnable onDisconnect) {
            // Tests call dispatch directly.
        }
    }
}
