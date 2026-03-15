package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;

public interface Effect {

    default void apply(Game game, Player player) {
        apply(player);
    }
    void apply(Player player);

}
