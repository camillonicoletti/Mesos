package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public class ExtraCardUpperRowEffect implements Effect{
    private static final long serialVersionUID = 1L;
    @Override
    public void apply(Player player){
        // Managed at the end of round in Game
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitExtraCardUpperRow(this);
    }

}
