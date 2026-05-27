package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

/**
 * Base type for the six character archetypes (Hunter, Gatherer, Shaman,
 * Builder, Artist, Inventor). Adds the minimum player count required for the
 * card to be playable, used by {@link CardFactory} when assembling the deck.
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

    public boolean isAvailableFor(int numPlayers) {
        return numPlayers >= minPlayers;
    }

    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitCharacter(this);
    }

}
