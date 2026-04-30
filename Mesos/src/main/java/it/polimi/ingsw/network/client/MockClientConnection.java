package it.polimi.ingsw.network.client;

import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.*;
import it.polimi.ingsw.network.protocol.serverbound.*;
import it.polimi.ingsw.network.transport.ClientSideConnection;

import java.util.*;
import java.util.function.Consumer;

public class MockClientConnection implements ClientSideConnection {

    private Consumer<ServerMessage> handler;
    private String myNickname = "Player1";

    private int expectedPlayers = 0;
    private boolean botAdded = false;
    private int totemPlaced = 0;
    private int actionsDone = 0;

    @Override
    public void setOnMessage(Consumer<ServerMessage> handler) {
        this.handler = handler;
    }

    @Override
    public void setOnDisconnect(Runnable onDisconnect) { }

    @Override
    public void send(ClientMessage message) {
        if (handler == null) return;

        if (message instanceof LoginMessage m) {
            handleLogin(m);
        } else if (message instanceof SetPlayersCountMessage m) {
            handleSetPlayers(m);
        } else if (message instanceof ReadyMessage m) {
            handleReady(m);
        } else if (message instanceof GameActionMessage m) {
            handleAction(m);
        } else if (message instanceof DisconnectMessage) {
            System.out.println("[MOCK] disconnessione richiesta");
        }
    }

    @Override
    public void close() {
        System.out.println("[MOCK] connessione chiusa");
    }

    // -------------------------------------------------------

    private void handleLogin(LoginMessage m) {
        myNickname = m.getNickname();
        expectedPlayers = 0;
        botAdded = false;
        totemPlaced = 0;
        actionsDone = 0;

        respond(new LoginAckMessage(LoginAckMessage.Outcome.LOGIN_OK, null));
        respond(new LobbyUpdateMessage(buildLobbySnapshots(), expectedPlayers));
    }

    private void handleSetPlayers(SetPlayersCountMessage m) {
        expectedPlayers = m.getCount();
        botAdded = true;
        respond(new LobbyUpdateMessage(buildLobbySnapshots(), expectedPlayers));
    }

    private void handleReady(ReadyMessage m) {
        if (!m.isReady()) return;

        respond(new StateChangedMessage("ReadyCheckState"));

        new Thread(() -> {
            sleep(500);
            respond(new GameStartedMessage(buildSnapshot()));
            respond(new StateChangedMessage("TotemPlacementState"));
        }).start();
    }

    private void handleAction(GameActionMessage m) {
        if (m.getAction() instanceof PlaceTotemAction a) {
            respond(new TotemPlacedMessage(myNickname, a.getTileId()));
            totemPlaced++;
            if (totemPlaced >= 2) {
                respond(new StateChangedMessage("ActionResolutionState"));
            }

        } else if (m.getAction() instanceof PickCardAction a) {
            int id;
            try { id = Integer.parseInt(a.getCardId()); } catch (NumberFormatException e) { id = 1; }
            respond(new CharacterCardTakenMessage(myNickname, id, a.isFromUpperRow()));
            actionsDone++;
            checkRoundEnd();

        } else if (m.getAction() instanceof PickBuildingAction a) {
            int id;
            try { id = Integer.parseInt(a.getCardId()); } catch (NumberFormatException e) { id = 10; }
            respond(new BuildingCardTakenMessage(myNickname, id, a.isFromUpperRow()));
            actionsDone++;
            checkRoundEnd();
        }
    }

    private void checkRoundEnd() {
        if (actionsDone >= 2) {
            actionsDone = 0;
            totemPlaced = 0;
            new Thread(() -> {
                sleep(300);
                respond(new RoundEndedMessage(2));
                sleep(300);
                respond(new GameEndedMessage(myNickname,
                        Map.of(myNickname, 42, "Bot", 17)));
            }).start();
        }
    }

    // -------------------------------------------------------

    private List<LobbyPlayerSnapshot> buildLobbySnapshots() {
        List<LobbyPlayerSnapshot> snaps = new ArrayList<>();
        snaps.add(new LobbyPlayerSnapshot(myNickname, TotemColor.RED, false, true));
        if (botAdded) {
            snaps.add(new LobbyPlayerSnapshot("Bot", TotemColor.BLUE, false, true));
        }
        return snaps;
    }

    private GameSnapshot buildSnapshot() {
        List<PlayerSnapshot> players = List.of(
                new PlayerSnapshot(myNickname, TotemColor.RED, 10, 0, new ArrayList<>(), new ArrayList<>()),
                new PlayerSnapshot("Bot", TotemColor.BLUE, 10, 0, new ArrayList<>(), new ArrayList<>())
        );
        return new GameSnapshot(
                players,
                List.of(1, 2, 3),
                List.of(4, 5, 6),
                List.of(10, 11),
                List.of(12, 13),
                Era.I, 1,
                new HashMap<>(),
                List.of(myNickname, "Bot")
        );
    }

    private void respond(ServerMessage msg) {
        if (handler != null) handler.accept(msg);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}