package it.polimi.ingsw.network.protocol.serverbound;
import it.polimi.ingsw.controller.action.GameAction;

/**
 * Wraps a real game action so the server can execute it through the controller.
 */
public class GameActionMessage implements ClientMessage {
    private final GameAction action;
    public GameActionMessage(GameAction action) { this.action = action; }
    public GameAction getAction() { return action; }
}
