package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;

public abstract class CharacterCard extends Card {

    public CharacterCard(int id, Era era, int minPlayers) {
        super(id, era, minPlayers);
    }

}