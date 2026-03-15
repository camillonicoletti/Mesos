package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

import it.polimi.ingsw.model.card.CharacterCard;

public class EndCharacterCountPPEffect implements Effect {
    private final int pointsPerCard;
    private final Class<? extends CharacterCard> targetType;

    public EndCharacterCountPPEffect(int pointsPerCard, Class<? extends CharacterCard> targetType) {
        this.pointsPerCard = pointsPerCard;
        this.targetType = targetType;
    }

    @Override
    public void apply(Player player)
    {
        int count = player.getTribe().countCharactersByType(targetType);
        player.addPrestigePoints(count * pointsPerCard);
    }
}
