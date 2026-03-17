package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Era;

public abstract class EventCard extends Card {

    private final boolean closeEvent;
    private final int priorityValue;

    public EventCard(Era era, boolean closeEvent, int priorityValue) {
        super(era);
        this.closeEvent = closeEvent;
        this.priorityValue = priorityValue;
    }

    public boolean isCloseEvent() {
        return closeEvent;
    }

    public int getResolutionPriority() {
        return priorityValue;
    }

    public abstract void apply(Game game);
}