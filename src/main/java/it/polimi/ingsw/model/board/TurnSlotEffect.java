package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Player;
import java.io.Serializable;

/**
 * Common behavior for effects printed on turn-order slots.
 */
public interface TurnSlotEffect extends Serializable {
    /**
     * Applies the turn-order effect to the player.
     */
    void apply(Player player);

    /**
     * Tells if this slot can trigger the extra food bonus from a building.
     */
    default boolean triggersTotemFoodBonus() {
        return false;
    }
}
