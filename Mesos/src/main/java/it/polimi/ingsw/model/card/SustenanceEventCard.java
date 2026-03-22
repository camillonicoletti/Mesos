package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.effect.SustenanceFoodDiscountEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.EventType;

public class SustenanceEventCard extends EventCard {

    private static final int FOOD_DISCOUNT_PER_GATHERER = 3;

    private final int prestigePointsLostPerUnfedCharacter;

    public SustenanceEventCard(int id, Era era, boolean closeEvent,
                               int prestigePointsLostPerUnfedCharacter) {
        super(id, era, closeEvent, EventType.SUSTENANCE);
        this.prestigePointsLostPerUnfedCharacter = prestigePointsLostPerUnfedCharacter;
    }

    public int getPrestigePointsLostPerUnfedCharacter() {
        return prestigePointsLostPerUnfedCharacter;
    }

    @Override
    public void resolve(Game game) {
        for (Player player : game.getPlayers()) {
            int totalCharacters = player.getTribe().countCharacterCards();
            int gatherers = player.getTribe().countCharactersByType(GathererCard.class);

            int foodDiscount = gatherers * FOOD_DISCOUNT_PER_GATHERER;

            //building effect
            for (BuildingCard building : player.getTribe().getBuildingCards()) {
                if (building.getEffect() instanceof SustenanceFoodDiscountEffect effect) {
                    int count = player.getTribe().countCharactersByType(effect.getTargetType());
                    foodDiscount += count;
                }
            }

            int foodRequired = Math.max(0, totalCharacters - foodDiscount);

            int availableFood = player.getFood();
            int foodPaid = Math.min(availableFood, foodRequired);

            if (foodPaid > 0) {
                player.spendFood(foodPaid);
            }

            int missingFood = foodRequired - foodPaid;

            if (missingFood > 0) {
                player.removePrestigePoints(missingFood * prestigePointsLostPerUnfedCharacter);
            }
        }
    }

}