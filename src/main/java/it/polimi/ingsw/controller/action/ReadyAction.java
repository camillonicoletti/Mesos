package it.polimi.ingsw.controller.action;

/**
 * Command: the player toggles their ready flag in the lobby's ready-check
 * phase. The match starts once every player is ready.
 */
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
