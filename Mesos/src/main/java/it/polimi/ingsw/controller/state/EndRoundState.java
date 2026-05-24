package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Round;

import java.io.Serial;

public class EndRoundState extends AbstractGameState {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final long END_ROUND_DISPLAY_MILLIS = 5_000L;

    @Override
    public void onEnter(GameController ctx) {
        Game game = ctx.getGame();
        Round round = game.getCurrentRound();
        game.getBoard().resetOfferTiles();


        if (game.isLastRound()) {
            game.end();
            ctx.changeState(new EndGameState());
        } else {
            int finishedRound = round.getNumber();
            // Order matters: discard lower → shift upper to lower → refill upper → check era transition.
            round.discardLowerRow();
            round.shiftUpperToLowerRow();
            round.refillUpperRow();
            round.checkEraTransition();
            game.nextRound();

            ctx.notifyListeners(l -> l.onRoundEnded(finishedRound));
            ctx.notifyListeners(GameListener::onGameSnapshotChanged);

            // Give clients time to display the end-of-round screen before transitioning.
            try {
                Thread.sleep(END_ROUND_DISPLAY_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            ctx.changeState(new TotemPlacementState());
        }
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
