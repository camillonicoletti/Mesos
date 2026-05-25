package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.protocol.clientbound.*;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.DisconnectMessage;
import it.polimi.ingsw.network.protocol.serverbound.PongMessage;
import it.polimi.ingsw.network.transport.ClientSideConnection;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ClientController {

    private final ClientModel model;
    private final ClientSideConnection connection;
    private final Map<Class<? extends ServerMessage>, Consumer<ServerMessage>> messageHandlers;
    private Consumer<Runnable> dispatcher = Runnable::run;

    public ClientController(ClientModel model, ClientSideConnection connection) {
        this.model = model;
        this.connection = connection;
        this.messageHandlers = createMessageHandlers();
        connection.setOnMessage(this::onMessage);
        connection.setOnDisconnect(this::onDisconnect);
    }

    /**
     * Sets the executor used to apply incoming server messages to the model.
     * The GUI sets this to Platform::runLater so all model writes happen on
     * the JavaFX thread and can never race with concurrent reads in render().
     */
    public void setDispatcher(Consumer<Runnable> dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void send(ClientMessage message) {
        try {
            connection.send(message);
        } catch (RuntimeException exception) {
            model.applyConnectionLost("Connection to the server lost");
            throw exception;
        }
    }

    public void disconnect() {
        try {
            connection.send(new DisconnectMessage());
        } catch (RuntimeException ignored) {
            // If the socket is already down, we still want the client to terminate cleanly.
        } finally {
            connection.close();
            model.applyConnectionLost("Connection closed.");
        }
    }

    public void onMessage(ServerMessage message) {
        if (message instanceof PingMessage) {
            connection.send(new PongMessage());
            return;
        }
        dispatcher.accept(() -> dispatch(message));
    }

    private void dispatch(ServerMessage message) {
        Consumer<ServerMessage> handler = messageHandlers.get(message.getClass());
        if (handler != null) {
            handler.accept(message);
        }
    }

    private Map<Class<? extends ServerMessage>, Consumer<ServerMessage>> createMessageHandlers() {
        Map<Class<? extends ServerMessage>, Consumer<ServerMessage>> handlers = new HashMap<>();
        register(handlers, LoginAckMessage.class, model::applyLoginAck);
        register(handlers, LobbyUpdateMessage.class, model::applyLobbyUpdate);
        register(handlers, GameStartedMessage.class, model::applyGameStarted);
        register(handlers, GameSyncMessage.class, model::applyGameSync);
        register(handlers, StateChangedMessage.class, model::applyStateChanged);
        register(handlers, TotemPlacedMessage.class, model::applyTotemPlaced);
        register(handlers, EraChangedMessage.class, model::applyEraChanged);
        register(handlers, FoodChangedMessage.class, model::applyFoodChanged);
        register(handlers, PointsChangedMessage.class, model::applyPointsChanged);
        register(handlers, CharacterCardTakenMessage.class, model::applyCharacterCardTaken);
        register(handlers, BuildingCardTakenMessage.class, model::applyBuildingCardTaken);
        register(handlers, RoundEndedMessage.class, model::applyRoundEnded);
        register(handlers, GameEndedMessage.class, model::applyGameEnded);
        register(handlers, ErrorMessage.class, model::applyError);
        register(handlers, EventResolvedMessage.class, model::applyEventResolved);
        register(handlers, CurrentPlayerChangedMessage.class, model::applyCurrentPlayerChanged);
        register(handlers, GameSuspendedMessage.class, model::applyGameSuspended);
        register(handlers, GameResumedMessage.class, model::applyGameResumed);
        register(handlers, PlayerDisconnectedMessage.class, model::applyPlayerDisconnected);
        register(handlers, PlayerReconnectedMessage.class, model::applyPlayerReconnected);
        register(handlers, ExtraCardPickAvailableMessage.class, model::applyExtraCardPickAvailable);
        return Map.copyOf(handlers);
    }

    private <T extends ServerMessage> void register(
            Map<Class<? extends ServerMessage>, Consumer<ServerMessage>> handlers,
            Class<T> type,
            Consumer<T> handler) {
        handlers.put(type, message -> handler.accept(type.cast(message)));
    }

    private void onDisconnect() {
        dispatcher.accept(() -> model.applyConnectionLost("Connection to server lost"));
    }
}
