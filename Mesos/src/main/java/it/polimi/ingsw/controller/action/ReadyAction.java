package it.polimi.ingsw.controller.action;

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