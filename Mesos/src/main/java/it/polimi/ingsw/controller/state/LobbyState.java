package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.model.Player;

/**
 * State responsible for lobby management.
 * In this state players can join and the first player can choose
 * the number of participants.
 */
public class LobbyState extends AbstractGameState {

    private static final long serialVersionUID = 1L;

    @Override
    public void handleAction(GameController ctx, String playerNickname, GameAction action) {
        if (action instanceof JoinGameAction joinGameAction) {
            handleJoinGame(ctx, playerNickname, joinGameAction);
            return;
        }

        if (action instanceof SetPlayerCountAction setPlayersCountAction) {
            handleSetPlayersCount(ctx, playerNickname, setPlayersCountAction);
            return;
        }

        rejectAction(action);
    }

    private void handleJoinGame(GameController ctx, String playerNickname, JoinGameAction action) {
        if (ctx.hasChosenPlayersCount() && ctx.isLobbyFull()) {
            throw new IllegalStateException("Lobby is already full");
        }

        if (!ctx.isNicknameAvailable(playerNickname)) {
            throw new IllegalArgumentException("Nickname already in use");
        }

        if (ctx.isTotemAlreadyTaken(action.getTotem())) {
            throw new IllegalArgumentException("Totem already taken");
        }

        Player player = new Player(playerNickname, action.getTotem());
        ctx.addLobbyPlayer(player);

        if (ctx.hasChosenPlayersCount() && ctx.isLobbyFull()) {
            ctx.changeState(new StartGameState());
        }
    }

    private void handleSetPlayersCount(GameController ctx,
                                       String playerNickname,
                                       SetPlayerCountAction action) {

        if (ctx.hasChosenPlayersCount()) {
            throw new IllegalStateException("Players count has already been chosen");
        }

        if (ctx.getLobbyPlayers().isEmpty()) {
            throw new IllegalStateException(
                    "At least one player must join before setting the players count"
            );
        }

        if (!ctx.isFirstPlayer(playerNickname)) {
            throw new IllegalStateException(
                    "Only the first player can choose the number of players"
            );
        }

        ctx.setChosenPlayersCount(action.getPlayersCount());

        if (ctx.isLobbyFull()) {
            ctx.changeState(new StartGameState());
        }
    }
}