package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.CardVisitor;
import it.polimi.ingsw.model.card.CharacterCard;

/**
 * End-game effect that grants PP equal to the sum of {@code pointsEndOfMatch}
 * of every Builder card in the tribe.
 */
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
        return points; // Sum of each Builder card's end-of-match point value.
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
