package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.model.Game;

import java.io.Serial;

public class EndGameState extends AbstractGameState {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public void onEnter(GameController ctx) {
        Game game = ctx.getGame();
        game.calculateFinalScores();
        // TODO : Notify winner
        game.end();
    }

    @Override
    public void handleAction(GameController ctx, String playerNickname, GameAction action) {
        rejectAction(action);
    }

}