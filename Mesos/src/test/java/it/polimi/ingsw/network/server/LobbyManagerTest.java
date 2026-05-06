package it.polimi.ingsw.network.server;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.GameEndedMessage;
import it.polimi.ingsw.network.protocol.clientbound.GameStartedMessage;
import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;
import it.polimi.ingsw.network.protocol.clientbound.StateChangedMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.network.transport.ServerSideConnection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
