package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.EventType;

public class EventCard extends Card {

    private EventType type;
    private boolean closeEvent;
    private int priorityValue;

    public EventCard(Era era, EventType type, boolean closeEvent, int priorityValue) {
        super(era);
        this.type = type;
        this.closeEvent = closeEvent;
        this.priorityValue = priorityValue;
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
        switch (type) {
            case HUNT -> applyHunt(game);
            case SHAMANIC_RITUAL -> applyShamanicRitual(game);
            case ROCK_PAINTINGS -> applyRockPaintings(game);
            case SUSTENANCE -> applySustenance(game);
        }
    }
    private void applyHunt(Game game) {
        // TODO
    }

    private void applyShamanicRitual(Game game) {
        // TODO
    }

    private void applyRockPaintings(Game game) {
        // TODO
    }

    private void applySustenance(Game game) {
        // TODO
    }
}