package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

/**
 * Base class for character cards.
 * It stores the minimum player count needed for the card to be included in the deck.
 */
public abstract class CharacterCard extends Card {

    private static final long serialVersionUID = 1L;
    private final int minPlayers;

    public CharacterCard(int id, Era era, int minPlayers) {
        super(id, era);
        this.minPlayers = minPlayers;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    /**
     * Checks if this card can be used with the current number of players.
     */
    public boolean isAvailableFor(int numPlayers) {
        return numPlayers >= minPlayers;
    }

    /**
     * Sends generic character cards to the character branch of the visitor.
     */
    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitCharacter(this);
    }

}
