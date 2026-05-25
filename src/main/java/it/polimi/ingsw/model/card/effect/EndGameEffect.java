package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;

public interface EndGameEffect extends Effect{
    int calculatePoints(Player player);
}
