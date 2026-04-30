package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Era;


public abstract class EventCard extends Card {
    private static final long serialVersionUID = 1L;
    private final boolean closeEvent;

    public EventCard(int id, Era era, boolean closeEvent) {
        super(id, era);
        this.closeEvent = closeEvent;
    }

    public boolean isCloseEvent() {
        return closeEvent;
    }

    public abstract void resolve(Game game);
}