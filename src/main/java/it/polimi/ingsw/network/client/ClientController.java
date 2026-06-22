package it.polimi.ingsw.network.client;

import it.polimi.ingsw.controller.action.DebugSkipRoundAction;
import it.polimi.ingsw.controller.action.DebugSkipToEndAction;
import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.action.SkipExtraPickAction;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.*;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.DisconnectMessage;
import it.polimi.ingsw.network.protocol.serverbound.ForceEndGameMessage;
import it.polimi.ingsw.network.protocol.serverbound.GameActionMessage;
import it.polimi.ingsw.network.protocol.serverbound.GetLeaderboardMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.PongMessage;
import it.polimi.ingsw.network.protocol.serverbound.QuitMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReadyMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReconnectMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.network.transport.ClientSideConnection;
import it.polimi.ingsw.view.View;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Client-side coordinator between the view, the local model and the server connection.
 *
 * Views call this class to send commands. Incoming server messages update
 * {@link ClientModel} first and then notify the current {@link View}.
 */
public class ClientController {

    private static final int LEADERBOARD_FETCH_LIMIT = 10;

    private final ClientModel model;
    private ClientSideConnection connection;
    private final ClientConnectionSupervisor connectionSupervisor;
    private final Map<Class<? extends ServerMessage>, Consumer<ServerMessage>> immediateMessageHandlers;
    private final Map<Class<? extends ServerMessage>, Consumer<ServerMessage>> messageHandlers;
    private final Map<Class<? extends ServerMessage>, Consumer<ServerMessage>> viewNotifications;
    private Consumer<Runnable> dispatcher = Runnable::run;
    private Consumer<Runnable> outboundDispatcher = Runnable::run;
    private View view;
    private volatile boolean voluntaryClose;

    public ClientController(ClientModel model, ClientSideConnection connection) {
        this(model, connection, null);
    }

    public ClientController(ClientModel model, ClientSideConnection connection,
                            ClientConnectionFactory connectionFactory) {
        this.model = model;
        this.connection = connection;
        this.connectionSupervisor = connectionFactory == null
                ? null
                : new ClientConnectionSupervisor(connectionFactory, this::installRecoveredConnection);
        this.immediateMessageHandlers = createImmediateMessageHandlers();
        this.messageHandlers = createMessageHandlers();
        this.viewNotifications = createViewNotifications();
        installConnection(connection);
    }

    /**
     * Sets the executor used to apply incoming server messages to the model.
     * The GUI sets this to Platform::runLater so all model writes happen on
     * the JavaFX thread and can never race with concurrent reads in render().
     */
    public void setDispatcher(Consumer<Runnable> dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Sets the executor used for outgoing client messages. The GUI uses a
     * background executor so RMI calls cannot block the JavaFX thread while the
     * server is processing a long action such as event resolution.
     */
    public void setOutboundDispatcher(Consumer<Runnable> outboundDispatcher) {
        this.outboundDispatcher = outboundDispatcher != null ? outboundDispatcher : Runnable::run;
    }

    /**
     * Registers the view the controller drives. After applying each incoming
     * server message to the model, the controller calls the matching
     * {@link View} method so the view re-renders the new state. This is the
     * single notification path: views no longer observe the model directly.
     */
    public void setView(View view) {
        this.view = view;
    }


    public void send(ClientMessage message) {
        // Outgoing messages may run on a background executor in the GUI.
        outboundDispatcher.accept(() -> sendNow(message));
    }

    private void sendNow(ClientMessage message) {
        if (!model.isConnected()) {
            dispatcher.accept(() -> handleRecoverableConnectionLoss("Server unavailable. Reconnecting..."));
            return;
        }
        try {
            connection.send(message);
        } catch (RuntimeException exception) {
            // A failed send is treated like a recoverable connection loss.
            dispatcher.accept(() -> handleRecoverableConnectionLoss("Connection to server lost. Reconnecting..."));
        }
    }

    // ── Shared player actions ──────────────────────────────────────────────────
    // Concrete helpers shared by both views: the TUI command parser and the GUI
    // buttons build the very same messages, so the construction lives here once.

    /** Attempts a fresh login with the chosen nickname and totem color. */
    public void login(String nickname, TotemColor color) {
        send(new LoginMessage(nickname, color));
    }

    /** Attempts to reconnect to an existing match under the given nickname. */
    public void reconnect(String nickname) {
        send(new ReconnectMessage(nickname));
    }

    /** Sets the local player's ready flag in the lobby. */
    public void setReady(boolean ready) {
        send(new ReadyMessage(ready));
    }

    /** Lobby leader: sets the expected number of participants. */
    public void setPlayersCount(int count) {
        send(new SetPlayersCountMessage(count));
    }

    /** Places this player's totem on the given offer tile. */
    public void placeTotem(char tile) {
        send(new GameActionMessage(new PlaceTotemAction(tile)));
    }

    /** Takes a character/event card from the upper or lower row. */
    public void pickCharacterCard(String cardId, boolean fromUpperRow) {
        send(new GameActionMessage(new PickCardAction(cardId, fromUpperRow)));
    }

    /** Takes a building card from the upper or lower row. */
    public void pickBuildingCard(String cardId, boolean fromUpperRow) {
        send(new GameActionMessage(new PickBuildingAction(cardId, fromUpperRow)));
    }

    /** Declines the optional extra pick (building #13). */
    public void skipExtraPick() {
        send(new GameActionMessage(new SkipExtraPickAction()));
    }

    /** Debug: skips the rest of the current round. */
    public void skipRound() {
        send(new GameActionMessage(new DebugSkipRoundAction()));
    }

    /** Debug: jumps straight to the end-of-match scoring. */
    public void skipToEnd() {
        send(new GameActionMessage(new DebugSkipToEndAction()));
    }

    /** Debug: forces the match to end immediately. */
    public void forceEndGame() {
        send(new ForceEndGameMessage());
    }

    /**
     * Sends a leaderboard request to the server. The result will be delivered
     * asynchronously through {@link View#showLeaderboard(int, java.util.List)}.
     *
     * @param playersCount the player-count bucket to query (e.g. 2, 3, 4)
     * @param limit        maximum number of rows to return
     */
    public void requestLeaderboard(int playersCount, int limit) {
        send(new GetLeaderboardMessage(playersCount, limit));
    }

    public void disconnect() {
        voluntaryClose = true;
        stopReconnectSupervisor();
        try {
            connection.send(new DisconnectMessage());
        } catch (RuntimeException ignored) {
            // If the socket is already down, we still want the client to terminate cleanly.
        } finally {
            connection.close();
            model.applyConnectionLost("Connection closed.");
        }
    }

    /**
     * Voluntary quit from the match. Tells the server this is an explicit exit
     * (irreversible, terminates the game) rather than a recoverable network drop.
     */
    public void quit() {
        voluntaryClose = true;
        stopReconnectSupervisor();
        try {
            connection.send(new QuitMessage());
        } catch (RuntimeException ignored) {
            // Socket already down: terminate locally anyway.
        } finally {
            connection.close();
            model.applyConnectionLost("You left the match.");
        }
    }

    public void onMessage(ServerMessage message) {
        // Some transport messages must be answered immediately and should not wait for the UI thread.
        Consumer<ServerMessage> immediateHandler = immediateMessageHandlers.get(message.getClass());
        if (immediateHandler != null) {
            immediateHandler.accept(message);
            return;
        }
        // Normal game messages are dispatched through the configured model executor.
        dispatcher.accept(() -> dispatch(message));
    }

    private void dispatch(ServerMessage message) {
        // First mutate the client model.
        Consumer<ServerMessage> handler = messageHandlers.get(message.getClass());
        if (handler != null) {
            handler.accept(message);
        }
        // Then tell the view what kind of refresh is needed.
        notifyView(message);
    }

    /**
     * Option A wiring: after the model has been updated, route the message to
     * the matching {@link View} reaction. The catch-all {@code onBoardChanged}
     * guarantees a re-render for any state message not explicitly mapped.
     */
    private void notifyView(ServerMessage message) {
        if (view == null) {
            return;
        }
        viewNotifications.getOrDefault(message.getClass(), ignored -> view.onBoardChanged())
                .accept(message);
    }

    private Map<Class<? extends ServerMessage>, Consumer<ServerMessage>> createImmediateMessageHandlers() {
        Map<Class<? extends ServerMessage>, Consumer<ServerMessage>> handlers = new HashMap<>();
        // Ping is transport-level: the controller answers with Pong without touching the model.
        register(handlers, PingMessage.class, ignored -> connection.send(new PongMessage()));
        return Map.copyOf(handlers);
    }

    private Map<Class<? extends ServerMessage>, Consumer<ServerMessage>> createMessageHandlers() {
        // This map replaces a long if/else chain with one handler per server message type.
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
        // LeaderboardMessage / LeaderboardRankMessage carry no model state; they
        // are forwarded straight to the view in notifyView().
        return Map.copyOf(handlers);
    }

    private Map<Class<? extends ServerMessage>, Consumer<ServerMessage>> createViewNotifications() {
        // The view notification map is separate from model updates because not every message redraws the same screen.
        Map<Class<? extends ServerMessage>, Consumer<ServerMessage>> notifications = new HashMap<>();
        register(notifications, LoginAckMessage.class, ignored -> view.onLoginResult());
        register(notifications, LobbyUpdateMessage.class, ignored -> view.onLobbyChanged());
        register(notifications, GameStartedMessage.class, ignored -> view.onGameStarted());
        register(notifications, StateChangedMessage.class, ignored -> view.onGameStateChanged());
        register(notifications, GameEndedMessage.class, ignored -> view.onGameEnded());
        register(notifications, ErrorMessage.class, ignored -> view.onError());
        register(notifications, EventResolvedMessage.class, ignored -> view.onEventResolved());
        register(notifications, GameSuspendedMessage.class, ignored -> view.onGameSuspended());
        register(notifications, GameResumedMessage.class, ignored -> view.onGameResumed());
        register(notifications, PlayerDisconnectedMessage.class, ignored -> view.onPlayerConnectionChanged());
        register(notifications, PlayerReconnectedMessage.class, ignored -> view.onPlayerConnectionChanged());
        register(notifications, LeaderboardMessage.class,
                leaderboard -> view.showLeaderboard(leaderboard.playersCount(), leaderboard.entries()));
        register(notifications, LeaderboardRankMessage.class, this::notifyLeaderboardRank);
        return Map.copyOf(notifications);
    }

    private void notifyLeaderboardRank(LeaderboardRankMessage rank) {
        view.showMyRank(rank.rank(), rank.playersCount());
        // Auto-fetch the full leaderboard so it can be shown next to the rank.
        requestLeaderboard(rank.playersCount(), LEADERBOARD_FETCH_LIMIT);
    }

    private <T extends ServerMessage> void register(
            Map<Class<? extends ServerMessage>, Consumer<ServerMessage>> handlers,
            Class<T> type,
            Consumer<T> handler) {
        handlers.put(type, message -> handler.accept(type.cast(message)));
    }

    private void onDisconnect() {
        if (voluntaryClose) {
            return;
        }
        // Unexpected drops are recoverable: the supervisor will try to reconnect.
        dispatcher.accept(() -> handleRecoverableConnectionLoss("Connection to server lost. Reconnecting..."));
    }

    private synchronized void installConnection(ClientSideConnection connection) {
        // Every new or recovered connection uses the same inbound callbacks.
        connection.setOnMessage(this::onMessage);
        connection.setOnDisconnect(this::onDisconnect);
    }

    private synchronized void installRecoveredConnection(ClientSideConnection recoveredConnection) {
        // After reconnect succeeds, future sends must use the fresh connection.
        this.connection = recoveredConnection;
        installConnection(recoveredConnection);
    }

    private void handleRecoverableConnectionLoss(String reason) {
        // The model stores a visible disconnected state while reconnect attempts continue.
        model.applyServerDisconnected(reason);
        if (view != null) {
            view.onConnectionLost();
        }
        startReconnectIfPossible();
    }

    private void startReconnectIfPossible() {
        if (connectionSupervisor == null) {
            return;
        }
        String nickname = model.getMyNickname();
        if (nickname == null || nickname.isBlank() || !model.isLoginSuccess()) {
            return;
        }
        // Only logged-in players can be matched to a suspended server-side session.
        connectionSupervisor.start(nickname);
    }

    private void stopReconnectSupervisor() {
        if (connectionSupervisor != null) {
            connectionSupervisor.stop();
        }
    }
}
