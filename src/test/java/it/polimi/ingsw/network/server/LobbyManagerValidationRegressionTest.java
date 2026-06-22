package it.polimi.ingsw.network.server;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.LoginAckMessage;
import it.polimi.ingsw.network.protocol.clientbound.LoginResult;
import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.clientbound.StateChangedMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.transport.ServerSideConnection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validation tests for LobbyManager login handling: the lobby must reject
 * null, blank and whitespace-only nicknames, reject duplicate nicknames
 * (including those differing only by leading/trailing whitespace), and not
 * deliver lobby/state messages to a rejected connection.
 */
class LobbyManagerValidationRegressionTest {

    /**
     * Null nickname must be rejected and must not crash the lobby.
     */
    @Test
    void loginWithNullNicknameIsRejected() {
        LobbyManager lobby = new LobbyManager();
        RecordingConnection connection = new RecordingConnection();

        try {
            lobby.handleLogin(connection, new LoginMessage(null, TotemColor.RED));

            LoginAckMessage ack = firstMessageAs(connection, LoginAckMessage.class);
            assertInstanceOf(LoginResult.Rejected.class, ack.getResult());
        } finally {
            lobby.shutdown();
        }
    }

    /**
     * Blank nickname must be rejected and must not register a player.
     */
    @Test
    void loginWithBlankNicknameIsRejected() {
        LobbyManager lobby = new LobbyManager();
        RecordingConnection connection = new RecordingConnection();

        try {
            lobby.handleLogin(connection, new LoginMessage("", TotemColor.RED));

            LoginAckMessage ack = firstMessageAs(connection, LoginAckMessage.class);
            assertInstanceOf(LoginResult.Rejected.class, ack.getResult());
        } finally {
            lobby.shutdown();
        }
    }

    /**
     * Spaces-only nickname must be rejected and must not register a player.
     */
    @Test
    void loginWithSpacesOnlyNicknameIsRejected() {
        LobbyManager lobby = new LobbyManager();
        RecordingConnection connection = new RecordingConnection();

        try {
            lobby.handleLogin(connection, new LoginMessage("     ", TotemColor.RED));

            LoginAckMessage ack = firstMessageAs(connection, LoginAckMessage.class);
            assertInstanceOf(LoginResult.Rejected.class, ack.getResult());
        } finally {
            lobby.shutdown();
        }
    }

    /**
     * A nickname that only differs by leading/trailing spaces must be rejected as duplicate.
     */
    @Test
    void loginWithTrimmedDuplicateNicknameIsRejected() {
        LobbyManager lobby = new LobbyManager();
        RecordingConnection alice = new RecordingConnection();
        RecordingConnection duplicate = new RecordingConnection();

        try {
            lobby.handleLogin(alice, new LoginMessage("Alice", TotemColor.RED));
            lobby.handleLogin(duplicate, new LoginMessage(" Alice ", TotemColor.BLUE));

            LoginAckMessage firstAck = firstMessageAs(alice, LoginAckMessage.class);
            LoginAckMessage duplicateAck = firstMessageAs(duplicate, LoginAckMessage.class);

            assertInstanceOf(LoginResult.LoginOk.class, firstAck.getResult());
            assertInstanceOf(LoginResult.Rejected.class, duplicateAck.getResult());
        } finally {
            lobby.shutdown();
        }
    }

    /**
     * A rejected duplicate login must not receive lobby/game state messages as if it were accepted.
     */
    @Test
    void rejectedDuplicateLoginDoesNotReceiveStateChangedMessage() {
        LobbyManager lobby = new LobbyManager();
        RecordingConnection alice = new RecordingConnection();
        RecordingConnection duplicate = new RecordingConnection();

        try {
            lobby.handleLogin(alice, new LoginMessage("Alice", TotemColor.RED));
            lobby.handleLogin(duplicate, new LoginMessage(" Alice ", TotemColor.BLUE));

            LoginAckMessage duplicateAck = firstMessageAs(duplicate, LoginAckMessage.class);

            assertInstanceOf(LoginResult.Rejected.class, duplicateAck.getResult());
            assertTrue(duplicate.messages.stream().noneMatch(StateChangedMessage.class::isInstance));
        } finally {
            lobby.shutdown();
        }
    }

    /**
     * The same exact nickname cannot be used by two active connections.
     */
    @Test
    void loginWithExactDuplicateNicknameIsRejected() {
        LobbyManager lobby = new LobbyManager();
        RecordingConnection alice = new RecordingConnection();
        RecordingConnection duplicate = new RecordingConnection();

        try {
            lobby.handleLogin(alice, new LoginMessage("Alice", TotemColor.RED));
            lobby.handleLogin(duplicate, new LoginMessage("Alice", TotemColor.BLUE));

            LoginAckMessage firstAck = firstMessageAs(alice, LoginAckMessage.class);
            LoginAckMessage duplicateAck = firstMessageAs(duplicate, LoginAckMessage.class);

            assertInstanceOf(LoginResult.LoginOk.class, firstAck.getResult());
            assertInstanceOf(LoginResult.Rejected.class, duplicateAck.getResult());
        } finally {
            lobby.shutdown();
        }
    }

    private <T extends ServerMessage> T firstMessageAs(RecordingConnection connection, Class<T> type) {
        return assertInstanceOf(type, connection.messages.get(0));
    }

    private static class RecordingConnection implements ServerSideConnection {
        private final List<ServerMessage> messages = new ArrayList<>();
        private String nickname;
        private boolean alive = true;

        @Override
        public void send(ServerMessage message) {
            messages.add(message);
        }

        @Override
        public void close() {
            alive = false;
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
        }

        @Override
        public void setOnDisconnect(Runnable onDisconnect) {
        }
    }
}