package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Era;


/**
 * Base type for cards drawn into the event rows (Hunt, Sustenance, Shamanic
 * Ritual, Cave Paintings). Subclasses implement {@link #resolve(Game)} with
 * the rule-specific behaviour applied to every player. The {@code closeEvent}
 * flag marks the bottom-of-deck events that signal end of game.
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

    public abstract void resolve(Game game);

    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitEvent(this);
    }
}
