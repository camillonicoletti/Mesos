package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.CharacterCard;

public class EndCharacterCountPPEffect implements EndGameEffect {
    private static final long serialVersionUID = 1L;
    private final Class<? extends CharacterCard> targetType;
    private final int pointsPerCard;

    public EndCharacterCountPPEffect(int pointsPerCard, Class<? extends CharacterCard> targetType) {
        this.targetType = targetType;
        this.pointsPerCard = pointsPerCard;
    }

    public Class<? extends CharacterCard> getTargetType() {
        return targetType;
    }

    public int getPointsPerCard() {
        return pointsPerCard;
    }

    @Override
    public int calculatePoints(Player player) {
        int count = player.getTribe().countCharactersByType(targetType);
        return count * pointsPerCard;
    }

    @Override
    public void apply(Player player) {
        player.addPrestigePoints(calculatePoints(player));
    }
}
