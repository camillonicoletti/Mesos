package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.CharacterCard;

public class EndBuilderPPEffect implements EndGameEffect {
    private static final long serialVersionUID = 1L;
    @Override
    public int calculatePoints(Player player) {
        int points = 0;
        for (CharacterCard card : player.getTribe().getCharacterCards()) {
            if (card instanceof BuilderCard builder) {
                points += builder.getPointsEndOfMatch();
            }
        }
        return points; // double the builder points
    }


    @Override
    public void apply(Player player) {
        player.addPrestigePoints(calculatePoints(player));
    }
}
