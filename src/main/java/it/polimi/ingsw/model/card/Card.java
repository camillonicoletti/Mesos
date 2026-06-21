package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

import java.io.Serializable;

/**
 * Base class for every card in the game.
 * It stores the card id and era, and lets visitors handle cards without long instanceof chains.
 */
public abstract class Card implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int id;
    private final Era era;

    public Card(int id, Era era) {
        this.id = id;
        this.era = era;
    }

    public int getId() {
        return id;
    }

    public Era getEra() {
        return era;
    }

    /**
     * Lets a visitor handle this card.
     * Subclasses override this to call a more specific visit method.
     */
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitCard(this);
    }

}
