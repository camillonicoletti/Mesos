package it.polimi.ingsw.controller.action;

public class SetPlayerCountAction implements GameAction {

    private static final long serialVersionUID = 1L;

    private final int playersCount;

    public SetPlayerCountAction(int playersCount) {
        this.playersCount = playersCount;
    }

    public int getPlayersCount() {
        return playersCount;
    }
}
