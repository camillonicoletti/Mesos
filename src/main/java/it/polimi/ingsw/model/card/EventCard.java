package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Era;


/**
 * Base class for event cards.
 * Each event knows how to resolve its rule on the whole game.
 * Close events are special bottom-deck events used to mark the end section of the deck.
 */
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

    /**
     * Applies this event to the game.
     */
    public abstract void resolve(Game game);

    /**
     * Sends generic event cards to the event branch of the visitor.
     */
    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitEvent(this);
    }
}
