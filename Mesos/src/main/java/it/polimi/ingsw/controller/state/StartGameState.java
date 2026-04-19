package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StartGameState extends AbstractGameState {

    private static final long serialVersionUID = 1L;

    @Override
    public void onEnter(GameController ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Controller context cannot be null");
        }

        if (!ctx.hasChosenPlayersCount()) {
            throw new IllegalStateException("Cannot start the game before choosing the players count");
        }

        if (!ctx.isLobbyFull()) {
            throw new IllegalStateException("Cannot start the game before the lobby is full");
        }

        if (!ctx.areAllPlayersReady()) {
            throw new IllegalStateException("Cannot start the game until all players are ready");
        }

        List<Player> players = new ArrayList<>(ctx.getLobbyPlayers());

        Game game = new Game(
                players,
                0,
                null,
                null,
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                null
        );

        game.start();
        ctx.setGame(game);
        //listener
        ctx.notifyListeners(l -> l.onGameStarted());

        ctx.changeState(new TotemPlacementState());
    }

    @Override
    public void handleAction(GameController ctx, String playerNickname, GameAction action) {
        rejectAction(action);
    }
}