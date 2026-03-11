package it.polimi.ingsw.model;

import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.EventType;

public class Event {
    private Era era;
    private EventType type;
    private boolean closeEvent;
    private int priorityValue;

    public Event(Era era, EventType type, boolean closeEvent, int priorityValue) {
        this.era = era;
        this.type = type;
        this.closeEvent = closeEvent;
        this.priorityValue = priorityValue;
    }

    public Era getEra() {
        return era;
    }

    public EventType getType() {
        return type;
    }

    public boolean isCloseEvent() {
        return closeEvent;
    }

    public int getResolutionPriority() {
        return priorityValue;
    }

    public void apply(Game game) {
        // TODO: implement event logic
    }
}
