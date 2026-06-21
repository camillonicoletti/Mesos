package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.CharacterCard;

/**
 * End-game effect that gives points for each owned character of one target type.
 */
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

    /**
     * Counts target characters and multiplies them by the configured point value.
     */
    @Override
    public int calculatePoints(Player player) {
        int count = player.getTribe().countCharactersByType(targetType);
        return count * pointsPerCard;
    }

    /**
     * Adds the calculated end-game points to the player.
     */
    @Override
    public void apply(Player player) {
        player.addPrestigePoints(calculatePoints(player));
    }

    @Override
    public <R> R accept(EffectVisitor<R> visitor) {
        return visitor.visitEndCharacterCountPP(this);
    }
}
