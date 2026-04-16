package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Round;

import java.io.Serial;

public class EndRoundState implements GameState {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public void onEnter(GameController ctx) {
        Game game = ctx.getGame();
        Round round = game.getCurrentRound();

        if (game.isLastRound()) {
            ctx.changeState(new EndGameState());
        } else {
            round.discardLowerRow();
            round.shiftUpperToLowerRow();
            round.refillUpperRow();
            round.checkEraTransition();
            game.nextRound();
            ctx.changeState(new TotemPlacementState());
        }
    }

    @Override
    public void handleAction(GameController ctx, String playerNickname, GameAction action) { }

    @Override
    public void onExit(GameController ctx) { }
}