package it.polimi.ingsw.view.card;

/**
 * Supplies the per-player figures an end-game building effect needs to score
 * itself, so each {@link BuildingEffect} computes its points without the caller
 * switching on the effect.
 */
public interface BuildingScoring {
    int builderCharacterPoints();

    int fullSetCount();

    int characterTypeCount(String characterType);
}
