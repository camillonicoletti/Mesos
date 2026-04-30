package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

import java.io.Serializable;

public interface Effect extends Serializable {

    void apply(Player player);

}
