package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.action.ReadyAction;

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

        ctx.resetReadyPlayers();
    }

    @Override
    public void handleAction(GameController ctx, String playerNickname, GameAction action) {
        if (action instanceof ReadyAction readyAction) {
            handleReadyAction(ctx, playerNickname, readyAction);
            return;
        }

        rejectAction(action);
    }

    private void handleReadyAction(GameController ctx, String playerNickname, ReadyAction action) {
        if (playerNickname == null || playerNickname.isBlank()) {
            throw new IllegalArgumentException("Player nickname cannot be null or blank");
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
}