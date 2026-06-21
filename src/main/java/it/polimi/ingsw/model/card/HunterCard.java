package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

/**
 * Character used during Hunt events.
 * Some Hunter cards also give immediate food when taken.
 */
public class    HunterCard extends CharacterCard{
    private static final long serialVersionUID = 1L;
    private final boolean foodIcon;

    public HunterCard(int id, Era era, int minPlayers, boolean foodIcon) {

        super(id, era, minPlayers);
        this.foodIcon = foodIcon;

    }

    /**
     * Tells if this Hunter has the immediate food icon.
     */
    public boolean hasFoodIcon() {
        return foodIcon;
    }

    /**
     * Sends this card to the hunter branch of the visitor.
     */
    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitHunter(this);
    }

}
