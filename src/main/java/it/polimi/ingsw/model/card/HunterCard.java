package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

/**
 * Character that contributes to Hunt event payouts (food and PP per hunter).
 * The {@code foodIcon} flag marks the Hunter cards that grant immediate food
 * on acquisition, scaled by the number of hunters already in the tribe.
 */
public class    HunterCard extends CharacterCard{
    private static final long serialVersionUID = 1L;
    private final boolean foodIcon;

    public HunterCard(int id, Era era, int minPlayers, boolean foodIcon) {

        super(id, era, minPlayers);
        this.foodIcon = foodIcon;

    }

    public boolean hasFoodIcon() {
        return foodIcon;
    }

    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitHunter(this);
    }

}
