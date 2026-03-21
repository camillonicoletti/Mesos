package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.EventType;

public abstract class EventCard extends Card {

    private final boolean closeEvent;
    private final int priorityValue;
    private final EventType eventType;

    public EventCard(int id, Era era, boolean closeEvent, int priorityValue,
                     EventType eventType, int minPlayers) {
        super(id, era, minPlayers);
        this.closeEvent = closeEvent;
        this.priorityValue = priorityValue;
        this.eventType = eventType;
    }

    public boolean isCloseEvent() {
        return closeEvent;
    }

    public int getResolutionPriority() {
        return priorityValue;
    }

    public EventType getEventType() {
        return eventType;
    }

    public abstract void resolve(Game game);
}