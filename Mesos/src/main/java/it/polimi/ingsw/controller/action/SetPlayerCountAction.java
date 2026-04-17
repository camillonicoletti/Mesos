package it.polimi.ingsw.controller.action;

/**
 * Action used to set the expected number of players in the lobby.
 */
public class SetPlayerCountAction implements GameAction {

    private static final long serialVersionUID = 1L;

    private final int playersCount;

    public SetPlayerCountAction(int playersCount) {
        this.playersCount = playersCount;
    }

    public int getPlayersCount() {
        return playersCount;
    }

    @Override
    public String getType() {
        return "";
    }
}