package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;

import java.io.Serial;

public class TotemPlacementState implements GameState {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public void onEnter(GameController ctx) { }

    @Override
    public void handleAction(GameController ctx, String playerNickname, GameAction action) { }

    @Override
    public void onExit(GameController ctx) { }
}