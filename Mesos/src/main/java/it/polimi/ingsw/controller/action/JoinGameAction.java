package it.polimi.ingsw.controller.action;

import it.polimi.ingsw.model.Totem;

/**
 * Action sent when a player wants to join the lobby.
 */
public class JoinGameAction implements GameAction {

    private static final long serialVersionUID = 1L;

    private final Totem totem;

    public JoinGameAction(Totem totem) {
        this.totem = totem;
    }

    @Override
    public String getType() {
        return "JOIN_GAME";
    }

    public Totem getTotem() {
        return totem;
    }
}