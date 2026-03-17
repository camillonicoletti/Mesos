package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.effect.HuntEffect;
import it.polimi.ingsw.model.enums.Era;

public class HuntEventCard extends EventCard {

    private final HuntEffect effect;

    public HuntEventCard(Era era, boolean closeEvent, int priorityValue, int foodPerHunter, int ppPerHunter) {
        super(era, closeEvent, priorityValue);
        this.effect = new HuntEffect(foodPerHunter, ppPerHunter);
    }

    public HuntEffect getEffect() {
        return effect;
    }

    @Override
    public void apply(Game game) {
        for (Player player : game.getPlayers()) {
            effect.apply(player);
        }
    }
}