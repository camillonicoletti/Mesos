package it.polimi.ingsw.controller.action;

// Toggles the player's ready flag during the lobby's ready-check phase; the match starts once everyone is ready.
public class ReadyAction implements GameAction {

    private static final long serialVersionUID = 1L;

    private final boolean ready;

    public ReadyAction(boolean ready) {
        this.ready = ready;
    }

    public boolean isReady() {
        return ready;
    }
}
