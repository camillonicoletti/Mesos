package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.CardVisitor;
import it.polimi.ingsw.model.card.CharacterCard;

public class EndBuilderPPEffect implements EndGameEffect {
    private static final long serialVersionUID = 1L;
    @Override
    public int calculatePoints(Player player) {
        int points = 0;
        CardVisitor<Integer> builderPoints = new CardVisitor<>() {
            @Override public Integer visitBuilder(BuilderCard builder) {
                return builder.getPointsEndOfMatch();
            }

            @Override public Integer visitCard(Card card) {
                return 0;
            }
        };
        for (CharacterCard card : player.getTribe().getCharacterCards()) {
            points += card.accept(builderPoints);
        }
        return points; // double the builder points
    }


    @Override
    public void apply(Player player) {
        player.addPrestigePoints(calculatePoints(player));
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitEndBuilderPP(this);
    }
}
