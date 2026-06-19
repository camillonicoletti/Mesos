package it.polimi.ingsw.controller.action;

import it.polimi.ingsw.model.Totem;

public class JoinGameAction implements GameAction {

    private static final long serialVersionUID = 1L;

    private final Totem totem;

    public JoinGameAction(Totem totem) {
        this.totem = totem;
    }

    public Totem getTotem() {
        return totem;
    }
}
