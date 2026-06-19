package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.model.Player;

// State responsible for lobby management: players can join, and the first player can choose the player count.
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

        if (action instanceof ReadyAction readyAction) {
            handleReadyAction(ctx, playerNickname, readyAction);
            return;
        }

        rejectAction(action);
    }

    @Override
    public void onPlayerDisconnected(GameController ctx, String playerNickname) {
        if (!ctx.containsLobbyPlayer(playerNickname)) {
            return;
        }

        ctx.removePlayerFromLobby(playerNickname);
    }

    private void handleJoinGame(GameController ctx, String playerNickname, JoinGameAction action) {
        if (ctx.hasChosenPlayersCount() && ctx.isLobbyFull()) {
            throw new IllegalStateException("Lobby is already full");
        }

        if (action.getTotem() == null || action.getTotem().getColor() == null) {
            throw new IllegalArgumentException("Totem color must be valid");
        }

        if (!ctx.isNicknameAvailable(playerNickname)) {
            throw new IllegalArgumentException("Nickname already in use");
        }

        if (ctx.isTotemAlreadyTaken(action.getTotem())) {
            throw new IllegalArgumentException("Totem already taken");
        }

        Player player = new Player(playerNickname, action.getTotem());
        ctx.addLobbyPlayer(player);
        ctx.notifyListeners(l -> l.onPlayerJoined(playerNickname));
        // Auto-advance to ready check as soon as the lobby reaches the chosen player count.
        if (ctx.hasChosenPlayersCount() && ctx.isLobbyFull()) {
            ctx.changeState(new ReadyCheckState());
        }
    }

    private void handleSetPlayersCount(GameController ctx,
                                       String playerNickname,
                                       SetPlayerCountAction action) {
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
        ctx.notifyListeners(l -> l.onPlayersCountSet(action.getPlayersCount()));

        if (ctx.isLobbyFull()) {
            if (ctx.areAllPlayersReady()) {
                ctx.changeState(new StartGameState());
            } else {
                ctx.changeState(new ReadyCheckState());
            }
        }
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
    }
}
