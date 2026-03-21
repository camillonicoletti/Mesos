package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

import it.polimi.ingsw.model.card.CharacterCard;

public class EndCharacterCountPPEffect implements EndGameEffect {

    private final Class<? extends CharacterCard> targetType;

    public EndCharacterCountPPEffect(int pointsPerCard, Class<? extends CharacterCard> targetType) {
        this.targetType = targetType;
    }

    @Override
    public void apply(Player player)
    {
        int count = player.getTribe().countCharactersByType(targetType);
        player.addPrestigePoints(count * 3);
    }
}
