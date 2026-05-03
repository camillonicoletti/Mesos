package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;

public class ReadyCheckState extends AbstractGameState {

    private static final long serialVersionUID = 1L;

    @Override
    public void onEnter(GameController ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Controller context cannot be null");
        }

        if (!ctx.isLobbyFull()) {
            throw new IllegalStateException("Cannot enter ReadyCheckState if the lobby is not full");
        }
        if (ctx.areAllPlayersReady()) {
            ctx.changeState(new StartGameState());
        }
    }

    @Override
    public void handleAction(GameController ctx, String playerNickname, GameAction action) {
        if (action instanceof ReadyAction readyAction) {
            handleReadyAction(ctx, playerNickname, readyAction);
            return;
        }

        if (action instanceof SetPlayerCountAction setPlayersCountAction) {
            handleSetPlayersCount(ctx, playerNickname, setPlayersCountAction);
            return;
        }

        rejectAction(action);
    }

    @Override
    public void onPlayerDisconnected(GameController ctx, String playerNickname) {
        if (!ctx.containsLobbyPlayer(playerNickname)) {
            return;
        }

        ctx.markPlayerNotReady(playerNickname);
        ctx.removePlayerFromLobby(playerNickname);
        ctx.changeState(new LobbyState());
    }

    private void handleReadyAction(GameController ctx, String playerNickname, ReadyAction action) {
        if (playerNickname == null || playerNickname.isBlank()) {
            throw new IllegalArgumentException("Player nickname cannot be null or blank");
        }
        if (!ctx.containsLobbyPlayer(playerNickname)) {
            throw new IllegalArgumentException("Player is not in the lobby");
        }

        if (action.isReady()) {
            ctx.markPlayerReady(playerNickname);
        } else {
            ctx.markPlayerNotReady(playerNickname);
        }

        if (ctx.areAllPlayersReady()) {
            ctx.changeState(new StartGameState());
        }
    }

    private void handleSetPlayersCount(GameController ctx,
                                       String playerNickname,
                                       SetPlayerCountAction action) {
        if (!ctx.isFirstPlayer(playerNickname)) {
            throw new IllegalStateException(
                    "Only the first player can choose the number of players"
            );
        }

        ctx.setChosenPlayersCount(action.getPlayersCount());
        ctx.resetReadyPlayers();
        ctx.notifyListeners(l -> l.onPlayersCountSet(action.getPlayersCount()));

        if (!ctx.isLobbyFull()) {
            ctx.changeState(new LobbyState());
        }
    }
}
