package it.polimi.ingsw.model.card.effect;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.HunterCard;

public class HuntEffect implements Effect{
    private final int foodPerHunter;
    private final int ppPerHunter;

    public HuntEffect(int foodPerHunter, int ppPerHunter) {
        this.foodPerHunter = foodPerHunter;
        this.ppPerHunter = ppPerHunter;
    }

    @Override
    public void apply(Player player) {
        int hunters = player.getTribe().countCharactersByType(HunterCard.class);
        player.addFood(hunters * foodPerHunter);
        player.addPrestigePoints(hunters * ppPerHunter);
    }
}
