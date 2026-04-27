package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;

import java.io.Serial;
import java.util.Map;

public class EndGameState extends AbstractGameState {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public void onEnter(GameController ctx) {
        Game game = ctx.getGame();
        game.calculateFinalScores();
        Player winner = ctx.consumeForcedWinner();
        if (winner == null) {
            winner = game.getWinner();
        }
        Player finalWinner = winner;
        Map<Player, Integer> scores = game.getFinalScores();
        ctx.notifyListeners(l -> l.onGameEnded(finalWinner, scores));
        game.end();
    }

    @Override
    public void handleAction(GameController ctx, String playerNickname, GameAction action) {
        rejectAction(action);
    }

    @Override
    public void onPlayerDisconnected(GameController ctx, String playerNickname) {
        if (!ctx.containsGamePlayer(playerNickname)) {
            return;
        }

        ctx.markPlayerDisconnected(playerNickname);
    }

    @Override
    public void onPlayerReconnected(GameController ctx, String playerNickname) {
        if (!ctx.containsGamePlayer(playerNickname)) {
            return;
        }
    }
}