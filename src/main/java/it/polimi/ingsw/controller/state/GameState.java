package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;

import java.io.Serializable;

// State pattern: each concrete state implements the controller behaviour for one phase of the game flow.
public interface GameState extends Serializable {

    void onEnter(GameController ctx);

    void handleAction(GameController ctx, String playerNickname, GameAction action);

    void onPlayerDisconnected(GameController ctx, String playerNickname);

    void onPlayerReconnected(GameController ctx, String playerNickname);

    // Called instead of onEnter() when resuming from SuspendedState, to avoid re-entering the state from scratch.
    void onResume(GameController ctx);

    void onExit(GameController ctx);
}
