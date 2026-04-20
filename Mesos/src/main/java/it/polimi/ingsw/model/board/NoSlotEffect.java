package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Player;

public class NoSlotEffect implements TurnSlotEffect {

    private static final long serialVersionUID = 1L;

    @Override
    public void apply(Player player) {
        // nessun effetto
    }
}
