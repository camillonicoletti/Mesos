package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.EventType;

public abstract class EventCard extends Card {

    private final boolean closeEvent;
    private final EventType eventType;

    public EventCard(int id, Era era, boolean closeEvent, EventType eventType) {
        super(id, era);
        this.closeEvent = closeEvent;
        this.eventType = eventType;
    }

    public boolean isCloseEvent() {
        return closeEvent;
    }

    public EventType getEventType() {
        return eventType;
    }

    public abstract void resolve(Game game);
}