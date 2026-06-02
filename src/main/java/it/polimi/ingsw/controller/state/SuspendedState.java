package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;

import java.io.Serial;

/**
 * Wrapper state activated when too few players are connected (FA4). Holds a
 * reference to the interrupted state so that it can be resumed unchanged once
 * enough players reconnect. Rejects every gameplay action while suspended.
 */
public class SuspendedState extends AbstractGameState {

    @Serial
    private static final long serialVersionUID = 1L;

    private final GameState suspendedState;

    public SuspendedState(GameState suspendedState) {
        if (suspendedState == null) {
            throw new IllegalArgumentException("Suspended state cannot be null");
        }

        this.suspendedState = suspendedState;
    }

    public GameState getSuspendedState() {
        return suspendedState;
    }

    @Override
    public void handleAction(GameController ctx, String playerNickname, GameAction action) {
        throw new IllegalStateException("Game is suspended while waiting for another player to reconnect");
    }

    @Override
    public void onPlayerDisconnected(GameController ctx, String playerNickname) {
        if (!ctx.containsGamePlayer(playerNickname)) {
            return;
        }

        suspendedState.onPlayerDisconnected(ctx, playerNickname);
    }

    @Override
    public void onPlayerReconnected(GameController ctx, String playerNickname) {
        if (!ctx.containsGamePlayer(playerNickname)) {
            return;
        }

        suspendedState.onPlayerReconnected(ctx, playerNickname);
    }
}
