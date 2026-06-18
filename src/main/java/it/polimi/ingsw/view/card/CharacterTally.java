package it.polimi.ingsw.view.card;

/**
 * Receives one character's contribution to a tribe summary, so each
 * {@link CharacterType} can record itself without the caller switching on type.
 */
public interface CharacterTally {
    void addHunter();

    void addGatherer();

    void addBuilder(int foodDiscount);

    void addShaman(int stars);

    void addArtist();

    void addInventor(String inventionIcon);
}
