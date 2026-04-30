package it.polimi.ingsw.network.server;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.EventCard;
import it.polimi.ingsw.network.protocol.clientbound.ErrorMessage;
import it.polimi.ingsw.network.protocol.clientbound.GameSnapshot;
import it.polimi.ingsw.network.protocol.clientbound.GameStartedMessage;
import it.polimi.ingsw.network.protocol.clientbound.GameSuspendedMessage;
import it.polimi.ingsw.network.protocol.clientbound.LobbyUpdateMessage;
import it.polimi.ingsw.network.protocol.clientbound.LoginAckMessage;
import it.polimi.ingsw.network.protocol.clientbound.PlayerSnapshot;
import it.polimi.ingsw.network.protocol.clientbound.StateChangedMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.network.transport.ServerSideConnection;

import java.util.*;
import java.util.stream.Collectors;

public class LobbyManager {

    private final GameController gameController = new GameController();
    private final Map<String, ServerSideConnection> connections = new HashMap<>();
    private final Map<String, VirtualView> views = new HashMap<>();

    public synchronized void handleLogin(ServerSideConnection conn, LoginMessage msg) {
        String nick = msg.getNickname();

        if (nick == null || nick.isBlank()) {
            conn.send(new LoginAckMessage(false, "Nickname non valido"));
            return;
        }

        if (connections.containsKey(nick) && !gameController.isPlayerDisconnected(nick)) {
            conn.send(new LoginAckMessage(false, "Nickname già in uso"));
            return;
        }

        if (gameController.containsGamePlayer(nick) && gameController.isPlayerDisconnected(nick)) {
            handleReconnection(conn, msg);
            return;
        }

        if (gameController.getGame() != null) {
            conn.send(new LoginAckMessage(false, "La partita è già iniziata"));
            return;
        }

        Totem totem = new Totem(msg.getTotemColor(), null);
        try {
            gameController.onMessageReceived(nick, new JoinGameAction(totem));
        } catch (Exception e) {
            conn.send(new LoginAckMessage(false, e.getMessage()));
            return;
        }

        registerConnection(nick, conn);

        conn.send(new LoginAckMessage(true, null));
        broadcastLobbyUpdate();

        String currentState = gameController.getCurrentState().getClass().getSimpleName();
        conn.send(new StateChangedMessage(currentState));
    }

    public synchronized void handleSetPlayersCount(String nick, SetPlayersCountMessage msg) {
        try {
            gameController.onMessageReceived(nick, new SetPlayerCountAction(msg.getCount()));
            broadcastLobbyUpdate();
        } catch (Exception e) {
            ServerSideConnection c = connections.get(nick);
            if (c != null) c.send(new ErrorMessage(e.getMessage()));
        }
    }

    public synchronized void handleReady(String nick, boolean ready) {
        try {
            boolean gameWasNotStarted = gameController.getGame() == null;
            gameController.onMessageReceived(nick, new ReadyAction(ready));
            if (gameWasNotStarted && gameController.getGame() != null) {
                broadcastGameStarted();
            } else {
                broadcastLobbyUpdate();
            }
        } catch (Exception e) {
            ServerSideConnection c = connections.get(nick);
            if (c != null) c.send(new ErrorMessage(e.getMessage()));
        }
    }

    public synchronized void handleDisconnect(String nick) {
        if (nick == null || nick.isBlank()) {
            return;
        }

        unregisterConnection(nick);

        if (!gameController.containsLobbyPlayer(nick) && !gameController.containsGamePlayer(nick)) {
            return;
        }

        gameController.onPlayerDisconnection(nick);

        if (gameController.getGame() == null) {
            broadcastLobbyUpdate();
        }
    }

    private void broadcastLobbyUpdate() {
        List<String> players = gameController.getLobbyPlayers().stream()
                .map(Player::getNickname)
                .collect(Collectors.toList());
        int expected = gameController.getChosenPlayersCount();
        List<String> ready = new ArrayList<>(gameController.getReadyPlayers());
        LobbyUpdateMessage msg = new LobbyUpdateMessage(players, expected, ready);
        for (ServerSideConnection c : connections.values()) {
            c.send(msg);
        }
    }

    private void handleReconnection(ServerSideConnection conn, LoginMessage msg) {
        Player existingPlayer = findGamePlayer(msg.getNickname());
        if (existingPlayer == null) {
            conn.send(new LoginAckMessage(false, "Player sconosciuto"));
            return;
        }
        if (existingPlayer.getTotem() == null
                || existingPlayer.getTotem().getColor() != msg.getTotemColor()) {
            conn.send(new LoginAckMessage(false, "Totem non coerente con il player esistente"));
            return;
        }

        try {
            gameController.onPlayerReconnection(msg.getNickname());
        } catch (Exception e) {
            conn.send(new LoginAckMessage(false, e.getMessage()));
            return;
        }

        registerConnection(msg.getNickname(), conn);
        conn.send(new LoginAckMessage(true, null));
        conn.send(new GameStartedMessage(buildGameSnapshot()));
        conn.send(new StateChangedMessage(gameController.getCurrentState().getClass().getSimpleName()));
        if (gameController.isSuspended()) {
            conn.send(new GameSuspendedMessage());
        }
    }

    private void broadcastGameStarted() {
        GameStartedMessage message = new GameStartedMessage(buildGameSnapshot());
        for (ServerSideConnection connection : connections.values()) {
            connection.send(message);
        }
    }

    private GameSnapshot buildGameSnapshot() {
        Game game = gameController.getGame();
        if (game == null) {
            throw new IllegalStateException("Cannot build a game snapshot before the game starts");
        }

        Round round = game.getCurrentRound();

        List<PlayerSnapshot> players = game.getPlayers().stream()
                .map(this::toPlayerSnapshot)
                .collect(Collectors.toList());

        List<Integer> upperRowCharacterAndEventIds = round == null
                ? List.of()
                : mergeCardIds(round.getUpperRowTribute(), round.getUpperRowEvents());
        List<Integer> lowerRowCharacterAndEventIds = round == null
                ? List.of()
                : mergeCardIds(round.getLowerRowTribute(), round.getLowerRowEvents());
        List<Integer> upperRowBuildingIds = round == null
                ? List.of()
                : extractCardIds(round.getUpperRowBuildings());
        List<Integer> lowerRowBuildingIds = round == null
                ? List.of()
                : extractCardIds(round.getLowerRowBuildings());

        Map<Character, String> totemOnTiles = new LinkedHashMap<>();
        if (game.getBoard() != null) {
            for (OfferTile tile : game.getBoard().getOfferTiles()) {
                if (tile.getTotemSlot() == null) {
                    continue;
                }
                String ownerNickname = findNicknameByTotem(game, tile.getTotemSlot());
                if (ownerNickname != null) {
                    totemOnTiles.put(tile.getLetter(), ownerNickname);
                }
            }
        }

        List<String> turnOrder = new ArrayList<>();
        if (game.getBoard() != null && game.getBoard().getTurnOrderTile() != null) {
            for (Totem totem : game.getBoard().getTurnOrderTile().getTurnOrder()) {
                String ownerNickname = findNicknameByTotem(game, totem);
                if (ownerNickname != null) {
                    turnOrder.add(ownerNickname);
                }
            }
        }

        return new GameSnapshot(
                players,
                upperRowCharacterAndEventIds,
                lowerRowCharacterAndEventIds,
                upperRowBuildingIds,
                lowerRowBuildingIds,
                game.getCurrentEra(),
                round != null ? round.getNumber() : 0,
                totemOnTiles,
                turnOrder
        );
    }

    private PlayerSnapshot toPlayerSnapshot(Player player) {
        return new PlayerSnapshot(
                player.getNickname(),
                player.getTotem().getColor().name(),
                player.getFood(),
                player.getPrestigePoints(),
                extractCardIds(player.getTribe().getCharacterCards()),
                extractCardIds(player.getTribe().getBuildingCards())
        );
    }

    private List<Integer> mergeCardIds(List<? extends CharacterCard> characterCards,
                                       List<? extends EventCard> eventCards) {
        List<Integer> ids = new ArrayList<>();
        ids.addAll(extractCardIds(characterCards));
        ids.addAll(extractCardIds(eventCards));
        return ids;
    }

    private List<Integer> extractCardIds(List<? extends Card> cards) {
        return cards.stream()
                .map(Card::getId)
                .collect(Collectors.toList());
    }

    private String findNicknameByTotem(Game game, Totem totem) {
        if (game == null || totem == null || totem.getColor() == null) {
            return null;
        }

        return game.getPlayers().stream()
                .filter(player -> player.getTotem() != null
                        && player.getTotem().getColor() == totem.getColor())
                .map(Player::getNickname)
                .findFirst()
                .orElse(null);
    }

    private Player findGamePlayer(String nickname) {
        Game game = gameController.getGame();
        if (game == null) {
            return null;
        }

        return game.getPlayers().stream()
                .filter(player -> player.getNickname().equals(nickname))
                .findFirst()
                .orElse(null);
    }

    private void registerConnection(String nickname, ServerSideConnection connection) {
        unregisterConnection(nickname);
        connections.put(nickname, connection);
        VirtualView view = new VirtualView(connection, nickname);
        views.put(nickname, view);
        gameController.addListener(view);
        connection.setNickname(nickname);
    }

    private void unregisterConnection(String nickname) {
        ServerSideConnection removedConnection = connections.remove(nickname);
        VirtualView removedView = views.remove(nickname);
        if (removedView != null) {
            gameController.removeListener(removedView);
        }
        if (removedConnection != null && removedConnection.isAlive()) {
            removedConnection.close();
        }
    }

    public GameController getGameController() { return gameController; }
}
