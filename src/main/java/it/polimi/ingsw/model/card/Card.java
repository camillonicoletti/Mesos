package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

import java.io.Serializable;

/**
 * Root of the card hierarchy. Carries the immutable identity (id) and the era
 * a card belongs to. Subclasses are dispatched through {@link CardVisitor}
 * (Visitor pattern) so that callers avoid {@code instanceof} chains when
 * specialising behaviour per concrete card type.
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

    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitCard(this);
    }

}
