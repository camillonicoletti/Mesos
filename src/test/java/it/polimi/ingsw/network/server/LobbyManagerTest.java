package it.polimi.ingsw.network.server;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.GameEndedMessage;
import it.polimi.ingsw.network.protocol.clientbound.GameStartedMessage;
import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.clientbound.StateChangedMessage;
import it.polimi.ingsw.network.protocol.clientbound.LobbyUpdateMessage;
import it.polimi.ingsw.network.protocol.clientbound.LoginAckMessage;
import it.polimi.ingsw.network.protocol.clientbound.LoginResult;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReconnectMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.network.transport.ServerSideConnection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for LobbyManager (network.server).
 *
 * Verifies: login, set-player-count, ready, reconnect handling and broadcast contents.
 */
class LobbyManagerTest {

    @Test
    void setPlayersCountThatStartsGameBroadcastsFinalCurrentState() {
        LobbyManager lobby = new LobbyManager();
        RecordingConnection alice = new RecordingConnection();
        RecordingConnection bob = new RecordingConnection();
        RecordingConnection charlie = new RecordingConnection();

        try {
            lobby.handleLogin(alice, new LoginMessage("Alice", TotemColor.RED));
            lobby.handleLogin(bob, new LoginMessage("Bob", TotemColor.BLUE));
            lobby.handleLogin(charlie, new LoginMessage("Charlie", TotemColor.PURPLE));
            lobby.handleSetPlayersCount("Alice", new SetPlayersCountMessage(3));

            lobby.handleReady("Alice", true);
            lobby.handleReady("Bob", true);
            lobby.handleDisconnect("Charlie");
            lobby.handleSetPlayersCount("Alice", new SetPlayersCountMessage(2));

            assertTrue(alice.messages.stream().anyMatch(GameStartedMessage.class::isInstance));
            ServerMessage lastMessage = alice.messages.get(alice.messages.size() - 1);
            StateChangedMessage stateChanged = assertInstanceOf(StateChangedMessage.class, lastMessage);
            assertEquals("TotemPlacementState", stateChanged.getStateName());
        } finally {
            lobby.shutdown();
        }
    }

    @Test
    void suspendedGameTimeoutIsCheckedAutomatically() throws Exception {
        LobbyManager lobby = new LobbyManager();
        RecordingConnection alice = new RecordingConnection();
        RecordingConnection bob = new RecordingConnection();

        try {
            lobby.handleLogin(alice, new LoginMessage("Alice", TotemColor.RED));
            lobby.handleLogin(bob, new LoginMessage("Bob", TotemColor.BLUE));
            lobby.handleSetPlayersCount("Alice", new SetPlayersCountMessage(2));
            lobby.handleReady("Alice", true);
            lobby.handleReady("Bob", true);

            lobby.getGameController().setSuspensionTimeoutMillis(10L);
            lobby.handleDisconnect("Bob");

            long deadline = System.currentTimeMillis() + 2_000L;
            while (System.currentTimeMillis() < deadline
                    && alice.messages.stream().noneMatch(GameEndedMessage.class::isInstance)) {
                Thread.sleep(25L);
            }

            GameEndedMessage gameEnded = assertInstanceOf(
                    GameEndedMessage.class,
                    alice.messages.stream()
                            .filter(GameEndedMessage.class::isInstance)
                            .findFirst()
                            .orElseThrow()
            );
            assertEquals("Alice", gameEnded.getWinnerNickname());
        } finally {
            lobby.shutdown();
        }
    }

    @Test
    void voluntaryQuitDuringGameEndsMatchForEveryone() throws Exception {
        // A voluntary quit (QuitMessage) terminates the match for all players;
        // it does NOT enter / stay in the suspended state waiting for reconnection.
        LobbyManager lobby = new LobbyManager();
        RecordingConnection alice = new RecordingConnection();
        RecordingConnection bob = new RecordingConnection();

        try {
            lobby.handleLogin(alice, new LoginMessage("Alice", TotemColor.RED));
            lobby.handleLogin(bob, new LoginMessage("Bob", TotemColor.BLUE));
            lobby.handleSetPlayersCount("Alice", new SetPlayersCountMessage(2));
            lobby.handleReady("Alice", true);
            lobby.handleReady("Bob", true);
            assertTrue(lobby.getGameController().getGame() != null,
                    "precondition: game must be started");

            lobby.handleQuit("Alice");

            assertTrue(lobby.getGameController().getGame().isGameOver(),
                    "voluntary quit must terminate the running match");
            assertTrue(bob.messages.stream().anyMatch(GameEndedMessage.class::isInstance),
                    "remaining player must be notified the match ended");
        } finally {
            lobby.shutdown();
        }
    }

    @Test
    void voluntaryQuitInLobbyJustRemovesPlayer() {
        // Pre-game quit must not crash and should not break the lobby for the other players.
        LobbyManager lobby = new LobbyManager();
        RecordingConnection alice = new RecordingConnection();
        RecordingConnection bob = new RecordingConnection();

        try {
            lobby.handleLogin(alice, new LoginMessage("Alice", TotemColor.RED));
            lobby.handleLogin(bob, new LoginMessage("Bob", TotemColor.BLUE));

            lobby.handleQuit("Bob");

            assertTrue(lobby.getGameController().getGame() == null,
                    "no game should have been created by a lobby-phase quit");
            assertTrue(lobby.getGameController().containsLobbyPlayer("Alice"),
                    "Alice must still be in the lobby after Bob's quit");
            assertTrue(!lobby.getGameController().containsLobbyPlayer("Bob"),
                    "Bob must no longer be in the lobby");
        } finally {
            lobby.shutdown();
        }
    }

    @Test
    void reconnectAfterForcedWinnerTimeoutIsRejected() throws Exception {
        // Regression: when a suspension timeout has already declared a forced winner,
        // a late reconnect from the disconnected player must NOT resurrect the match.
        LobbyManager lobby = new LobbyManager();
        RecordingConnection alice = new RecordingConnection();
        RecordingConnection bob = new RecordingConnection();

        try {
            lobby.handleLogin(alice, new LoginMessage("Alice", TotemColor.RED));
            lobby.handleLogin(bob, new LoginMessage("Bob", TotemColor.BLUE));
            lobby.handleSetPlayersCount("Alice", new SetPlayersCountMessage(2));
            lobby.handleReady("Alice", true);
            lobby.handleReady("Bob", true);

            lobby.getGameController().setSuspensionTimeoutMillis(10L);
            lobby.handleDisconnect("Bob");

            long deadline = System.currentTimeMillis() + 2_000L;
            while (System.currentTimeMillis() < deadline
                    && alice.messages.stream().noneMatch(GameEndedMessage.class::isInstance)) {
                Thread.sleep(25L);
            }
            assertTrue(lobby.getGameController().getGame().isGameOver(),
                    "precondition: forced-winner timeout must have ended the game");

            RecordingConnection bobReconnect = new RecordingConnection();
            lobby.handleReconnect(bobReconnect, new ReconnectMessage("Bob"));

            LoginAckMessage ack = assertInstanceOf(
                    LoginAckMessage.class,
                    bobReconnect.messages.stream()
                            .filter(LoginAckMessage.class::isInstance)
                            .findFirst()
                            .orElseThrow()
            );
            assertInstanceOf(LoginResult.Rejected.class, ack.getResult());
            assertEquals("Match has already ended", ack.getErrorReason());
        } finally {
            lobby.shutdown();
        }
    }

    @Test
    void pendingConnectionsReceiveLobbyUpdatesAfterAnotherPlayerLogsIn() {
        // Regression: a client still on the login screen must see totem/nickname picks
        // happening in real time, otherwise it offers already-taken options.
        LobbyManager lobby = new LobbyManager();
        RecordingConnection pending = new RecordingConnection();
        RecordingConnection alice = new RecordingConnection();

        try {
            lobby.registerPendingConnection(pending);
            int messagesBeforeAliceLogin = pending.messages.size();

            lobby.handleLogin(alice, new LoginMessage("Alice", TotemColor.RED));

            long updatesAfter = pending.messages.stream()
                    .skip(messagesBeforeAliceLogin)
                    .filter(LobbyUpdateMessage.class::isInstance)
                    .count();
            assertTrue(updatesAfter >= 1,
                    "pending client must receive a LobbyUpdate after another player logs in");
            LobbyUpdateMessage last = (LobbyUpdateMessage) pending.messages.stream()
                    .filter(LobbyUpdateMessage.class::isInstance)
                    .reduce((a, b) -> b)
                    .orElseThrow();
            assertTrue(last.getPlayers().stream()
                            .anyMatch(p -> "Alice".equals(p.getNickname())
                                    && p.getTotemColor() == TotemColor.RED),
                    "pending client's latest lobby snapshot must reflect Alice's RED totem");
        } finally {
            lobby.shutdown();
        }
    }

    private static class RecordingConnection implements ServerSideConnection {
        private final List<ServerMessage> messages = new CopyOnWriteArrayList<>();
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
