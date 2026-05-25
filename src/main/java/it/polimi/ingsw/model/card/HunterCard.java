package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

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
