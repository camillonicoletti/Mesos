package it.polimi.ingsw.network.protocol.serverbound;
import it.polimi.ingsw.controller.action.GameAction;

public class GameActionMessage implements ClientMessage {
    private final GameAction action;
    public GameActionMessage(GameAction action) { this.action = action; }
    public GameAction getAction() { return action; }
}