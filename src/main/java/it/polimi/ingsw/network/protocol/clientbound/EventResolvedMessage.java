package it.polimi.ingsw.network.protocol.clientbound;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Describes the result of an event card so the client can show the effects.
 */
public class EventResolvedMessage implements ServerMessage, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Effect applied to a single player during the event resolution.
     */
    public static final class PlayerEffect implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private final String nickname;
        private final int foodDelta;
        private final int newFood;
        private final int pointsDelta;
        private final int newPoints;

        public PlayerEffect(String nickname, int foodDelta, int newFood,
                            int pointsDelta, int newPoints) {
            this.nickname = nickname;
            this.foodDelta = foodDelta;
            this.newFood = newFood;
            this.pointsDelta = pointsDelta;
            this.newPoints = newPoints;
        }

        public String getNickname()  { return nickname; }
        public int getFoodDelta()    { return foodDelta; }
        public int getNewFood()      { return newFood; }
        public int getPointsDelta()  { return pointsDelta; }
        public int getNewPoints()    { return newPoints; }
    }

    private final int cardId;
    private final String title;
    private final String description;
    private final List<PlayerEffect> effects;

    public EventResolvedMessage(int cardId, String title, String description,
                                List<PlayerEffect> effects) {
        this.cardId = cardId;
        this.title = title;
        this.description = description;
        this.effects = effects;
    }

    public int getCardId()              { return cardId; }
    public String getTitle()            { return title; }
    public String getDescription()      { return description; }
    public List<PlayerEffect> getEffects() { return effects; }
}
