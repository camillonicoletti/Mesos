package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * Effect used during final scoring.
 * It calculates how many points a building adds at the end of the match.
 */
public interface EndGameEffect extends Effect{
    /**
     * Returns the end-game points given to the player.
     */
    int calculatePoints(Player player);
}
