package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.EventType;

public class HuntEventCard extends EventCard {

    private final int foodPerHunter;
    private final int ppPerHunter;

    public HuntEventCard(int id, Era era, boolean closeEvent,
                         int foodPerHunter, int ppPerHunter, int minPlayers) {
        super(id, era, closeEvent, EventType.HUNT, minPlayers);
        this.foodPerHunter = foodPerHunter;
        this.ppPerHunter = ppPerHunter;
    }

    public int getFoodPerHunter() {
        return foodPerHunter;
    }

    public int getPpPerHunter() {
        return ppPerHunter;
    }

    @Override
    public void resolve(Game game) {
        for (Player player : game.getPlayers()) {
            int hunters = player.getTribe().countCharactersByType(HunterCard.class);

            int foodGained = hunters * foodPerHunter;
            int prestigePointsGained = hunters * ppPerHunter;

            player.addFood(foodGained);
            player.addPrestigePoints(prestigePointsGained);
        }
    }
}