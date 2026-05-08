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
        // Notifica gestita dal LobbyManager con LobbyUpdateMessage; qui non serve
    }

    @Override
    public void onPlayersCountSet(int count) {
        // Idem
    }

    @Override
    public void onGameStarted() {
        // Il GameStartedMessage con snapshot lo manda LobbyManager dopo aver costruito lo snapshot
    }

    @Override
    public void onStateChanged(GameState newState) {
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
        Map<String, Integer> scoresByNickname = finalScores.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().getNickname(), Map.Entry::getValue));
        connection.send(new GameEndedMessage(winner.getNickname(), scoresByNickname));
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
        String currentPlayer = controller.getCurrentTurnPlayerNickname();
        if (currentPlayer != null) {
            connection.send(new CurrentPlayerChangedMessage(currentPlayer));
        }
    }
    @Override
    public void onGameSnapshotChanged() {
        connection.send(new GameSyncMessage(SnapshotBuilder.build(controller)));
    }
}
