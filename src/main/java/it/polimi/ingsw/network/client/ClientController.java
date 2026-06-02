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


public class ClientController {

    private static final int LEADERBOARD_FETCH_LIMIT = 10;

    private final ClientModel model;
    private final ClientSideConnection connection;
    private final Map<Class<? extends ServerMessage>, Consumer<ServerMessage>> messageHandlers;
    private Consumer<Runnable> dispatcher = Runnable::run;
    private View view;

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
        try {
            connection.send(message);
        } catch (RuntimeException exception) {
            model.applyConnectionLost("Connection to the server lost");
            if (view != null) {
                view.onConnectionLost();
            }
            throw exception;
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
        if (message instanceof LoginAckMessage) {
            view.onLoginResult();
        } else if (message instanceof LobbyUpdateMessage) {
            view.onLobbyChanged();
        } else if (message instanceof GameStartedMessage) {
            view.onGameStarted();
        } else if (message instanceof StateChangedMessage) {
            view.onGameStateChanged();
        } else if (message instanceof GameEndedMessage) {
            view.onGameEnded();
        } else if (message instanceof ErrorMessage) {
            view.onError();
        } else if (message instanceof EventResolvedMessage) {
            view.onEventResolved();
        } else if (message instanceof GameSuspendedMessage) {
            view.onGameSuspended();
        } else if (message instanceof GameResumedMessage) {
            view.onGameResumed();
        } else if (message instanceof PlayerDisconnectedMessage
                || message instanceof PlayerReconnectedMessage) {
            view.onPlayerConnectionChanged();
        } else if (message instanceof LeaderboardMessage leaderboard) {
            view.showLeaderboard(leaderboard.playersCount(), leaderboard.entries());
        } else if (message instanceof LeaderboardRankMessage rank) {
            view.showMyRank(rank.rank(), rank.playersCount());
            // Auto-fetch the full leaderboard so it can be shown next to the rank.
            requestLeaderboard(rank.playersCount(), LEADERBOARD_FETCH_LIMIT);
        } else {
            // Every remaining state message (sync, era, food, points, cards,
            // current player, totem, round, extra pick) is a board update.
            view.onBoardChanged();
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
        // LeaderboardMessage / LeaderboardRankMessage carry no model state; they
        // are forwarded straight to the view in notifyView().
        return Map.copyOf(handlers);
    }

    private <T extends ServerMessage> void register(
            Map<Class<? extends ServerMessage>, Consumer<ServerMessage>> handlers,
            Class<T> type,
            Consumer<T> handler) {
        handlers.put(type, message -> handler.accept(type.cast(message)));
    }

    private void onDisconnect() {
        dispatcher.accept(() -> {
            model.applyConnectionLost("Connection to server lost");
            if (view != null) {
                view.onConnectionLost();
            }
        });
    }
}
