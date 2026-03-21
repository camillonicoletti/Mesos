package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.CharacterCard;

public class EndBuilderPPEffect implements EndGameEffect{

    @Override
    public void apply(Player player){
        for(CharacterCard card : player.getTribe().getCharacterCards()){
            if (card instanceof BuilderCard builder){
                //Adding points another time totaling double points for each builder
                player.addPrestigePoints(builder.getPointsEndOfMatch());
            }
        }
    }
}

