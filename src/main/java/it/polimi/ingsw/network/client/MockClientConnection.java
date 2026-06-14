package it.polimi.ingsw.network.client;

import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.*;
import it.polimi.ingsw.network.protocol.serverbound.*;
import it.polimi.ingsw.network.transport.ClientSideConnection;

import java.util.*;
import java.util.function.Consumer;

public class MockClientConnection implements ClientSideConnection {

    private final Map<Class<? extends ClientMessage>, Consumer<ClientMessage>> messageHandlers;
    private final Map<Class<? extends GameAction>, Consumer<GameAction>> actionHandlers;
    private Consumer<ServerMessage> handler;
    private String myNickname = "Player1";

    private int expectedPlayers = 0;
    private boolean botAdded = false;
    private int totemPlaced = 0;
    private int actionsDone = 0;

    public MockClientConnection() {
        this.messageHandlers = createMessageHandlers();
        this.actionHandlers = createActionHandlers();
    }

    @Override
    public void setOnMessage(Consumer<ServerMessage> handler) {
        this.handler = handler;
    }

    @Override
    public void setOnDisconnect(Runnable onDisconnect) { }

    @Override
    public void send(ClientMessage message) {
        if (handler == null) return;

        Consumer<ClientMessage> messageHandler = messageHandlers.get(message.getClass());
        if (messageHandler != null) messageHandler.accept(message);
    }

    @Override
    public void close() {
        System.out.println("[MOCK] connection closed");
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
        Consumer<GameAction> actionHandler = actionHandlers.get(m.getAction().getClass());
        if (actionHandler != null) actionHandler.accept(m.getAction());
    }

    private void handlePlaceTotem(PlaceTotemAction action) {
        respond(new TotemPlacedMessage(myNickname, action.getTileId()));
        totemPlaced++;
        if (totemPlaced >= 2) {
            respond(new StateChangedMessage("ActionResolutionState"));
        }
    }

    private void handlePickCard(PickCardAction action) {
        int id = parseCardIdOrDefault(action.getCardId(), 1);
        respond(new CharacterCardTakenMessage(myNickname, id, action.isFromUpperRow()));
        actionsDone++;
        checkRoundEnd();
    }

    private void handlePickBuilding(PickBuildingAction action) {
        int id = parseCardIdOrDefault(action.getCardId(), 10);
        respond(new BuildingCardTakenMessage(myNickname, id, action.isFromUpperRow()));
        actionsDone++;
        checkRoundEnd();
    }

    private int parseCardIdOrDefault(String cardId, int fallback) {
        try {
            return Integer.parseInt(cardId);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private Map<Class<? extends ClientMessage>, Consumer<ClientMessage>> createMessageHandlers() {
        Map<Class<? extends ClientMessage>, Consumer<ClientMessage>> handlers = new HashMap<>();
        registerMessage(handlers, LoginMessage.class, this::handleLogin);
        registerMessage(handlers, SetPlayersCountMessage.class, this::handleSetPlayers);
        registerMessage(handlers, ReadyMessage.class, this::handleReady);
        registerMessage(handlers, GameActionMessage.class, this::handleAction);
        registerMessage(handlers, DisconnectMessage.class,
                ignored -> System.out.println("[MOCK] disconnect requested"));
        return Map.copyOf(handlers);
    }

    private Map<Class<? extends GameAction>, Consumer<GameAction>> createActionHandlers() {
        Map<Class<? extends GameAction>, Consumer<GameAction>> handlers = new HashMap<>();
        registerAction(handlers, PlaceTotemAction.class, this::handlePlaceTotem);
        registerAction(handlers, PickCardAction.class, this::handlePickCard);
        registerAction(handlers, PickBuildingAction.class, this::handlePickBuilding);
        return Map.copyOf(handlers);
    }

    private <T extends ClientMessage> void registerMessage(
            Map<Class<? extends ClientMessage>, Consumer<ClientMessage>> handlers,
            Class<T> type,
            Consumer<T> handler) {
        handlers.put(type, message -> handler.accept(type.cast(message)));
    }

    private <T extends GameAction> void registerAction(
            Map<Class<? extends GameAction>, Consumer<GameAction>> handlers,
            Class<T> type,
            Consumer<T> handler) {
        handlers.put(type, action -> handler.accept(type.cast(action)));
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
                        Map.of(myNickname, 42, "Bot", 17),
                        Map.of(myNickname, 3, "Bot", 1)));
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
