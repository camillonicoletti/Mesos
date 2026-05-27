package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

/**
 * Marker sub-type for effects that contribute to end-of-match scoring.
 * {@link #calculatePoints(Player)} returns the PP added to the player's final
 * score; concrete implementations apply themselves by adding those PP.
 */
public interface EndGameEffect extends Effect{
    int calculatePoints(Player player);
}
