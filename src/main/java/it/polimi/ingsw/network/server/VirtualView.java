package it.polimi.ingsw.network.server;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.network.protocol.clientbound.*;
import it.polimi.ingsw.network.transport.ServerSideConnection;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Server-side view attached to one remote client.
 *
 * The GameController emits listener callbacks; this adapter converts them
 * into clientbound DTOs and sends them through the player's connection.
 */
public class VirtualView implements GameListener {
    private final ServerSideConnection connection;
    private final String nickname;
    private final GameController controller;

    public VirtualView(ServerSideConnection connection, String nickname, GameController controller) {
        this.connection = connection;
        this.nickname = nickname;
        this.controller = controller;
    }

    public String getNickname() { return nickname; }
    public ServerSideConnection getConnection() { return connection; }

    @Override
    public void onPlayerJoined(String nick) {
        // Notification handled by LobbyManager through LobbyUpdateMessage; nothing is needed here.
    }

    @Override
    public void onPlayersCountSet(int count) {
        // Notification handled by LobbyManager through LobbyUpdateMessage; nothing is needed here.
    }

    @Override
    public void onGameStarted() {
        // LobbyManager sends the GameStartedMessage with the snapshot after building it.
    }

    @Override
    public void onStateChanged(GameState newState) {
        // State changes drive which screen/actions the client should show.
        connection.send(new StateChangedMessage(newState.getClass().getSimpleName()));
        sendCurrentPlayerIfAvailable();
    }

    @Override
    public void onTotemPlaced(String nick, char tileId) {
        connection.send(new TotemPlacedMessage(nick, tileId));
    }

    @Override
    public void onCharacterCardTaken(String nick, String cardId, boolean fromUpper) {
        connection.send(new CharacterCardTakenMessage(nick, Integer.parseInt(cardId), fromUpper));
    }

    @Override
    public void onBuildingCardTaken(String nick, String cardId, boolean fromUpper) {
        connection.send(new BuildingCardTakenMessage(nick, Integer.parseInt(cardId), fromUpper));
    }

    @Override
    public void onFoodChanged(String nick, int newAmount, int delta) {
        connection.send(new FoodChangedMessage(nick, newAmount, delta));
    }

    @Override
    public void onPointsChanged(String nick, int newPoints, int delta) {
        connection.send(new PointsChangedMessage(nick, newPoints, delta));
    }

    @Override
    public void onEraChanged(Era newEra) {
        connection.send(new EraChangedMessage(newEra));
    }

    @Override
    public void onRoundEnded(int roundNumber) {
        connection.send(new RoundEndedMessage(roundNumber));
    }

    @Override
    public void onGameEnded(Player winner, Map<Player, Integer> finalScores) {
        // Convert server-side Player keys into simple nickname-based DTO data.
        Map<String, Integer> scoresByNickname = finalScores.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().getNickname(), Map.Entry::getValue));
        // Read food directly from server-side Player objects so the client receives
        // the authoritative final value (after Sustenance and end-round penalties),
        // not a stale value cached in PlayerView.
        Map<String, Integer> foodByNickname = finalScores.keySet().stream()
                .collect(Collectors.toMap(Player::getNickname, Player::getFood));
        String winnerNickname = winner != null ? winner.getNickname() : null;
        connection.send(new GameEndedMessage(winnerNickname, scoresByNickname, foodByNickname));
    }
    @Override
    public void onPlayerDisconnected(String nick) {
        connection.send(new PlayerDisconnectedMessage(nick));
    }

    @Override
    public void onPlayerReconnected(String nick) {
        connection.send(new PlayerReconnectedMessage(nick));
    }

    @Override
    public void onGameSuspended() {
        // The remaining time lets the client show a suspension countdown.
        connection.send(new GameSuspendedMessage(controller.getSuspensionRemainingMillis()));
    }

    @Override
    public void onGameResumed() {
        connection.send(new GameResumedMessage());
    }

    @Override
    public void onExtraCardPickAvailable(String nick, int picks) {
        connection.send(new ExtraCardPickAvailableMessage(nick, picks));
    }

    @Override
    public void onCurrentPlayerChanged(String nick) {
        connection.send(new CurrentPlayerChangedMessage(nick));
    }

    private void sendCurrentPlayerIfAvailable() {
        // Some states need the current actor immediately after the state name.
        String currentPlayer = controller.getCurrentTurnPlayerNickname();
        if (currentPlayer != null) {
            connection.send(new CurrentPlayerChangedMessage(currentPlayer));
        }
    }
    @Override
    public void onGameSnapshotChanged() {
        // A full sync repairs or refreshes the whole client-side board.
        connection.send(new GameSyncMessage(SnapshotBuilder.build(controller)));
    }
    @Override
    public void onEventResolved(int cardId, String title, String description,
                                List<EventResolvedMessage.PlayerEffect> effects) {
        connection.send(new EventResolvedMessage(cardId, title, description, effects));
    }
    @Override
    public void onLeaderboardRank(String nick, int rank, int playersCount) {
        if (this.nickname.equals(nick)) {
            connection.send(new LeaderboardRankMessage(nick, rank, playersCount));
        }
    }
}
