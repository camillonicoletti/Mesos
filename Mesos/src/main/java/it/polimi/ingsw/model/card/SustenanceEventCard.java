package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.enums.Era;

public class SustenanceEventCard extends EventCard {

    private static final int FOOD_DISCOUNT_PER_GATHERER = 3;

    private final int prestigePointsLostPerUnfedCharacter;

    public SustenanceEventCard(Era era, boolean closeEvent, int priorityValue, int prestigePointsLostPerUnfedCharacter) {
        super(era, closeEvent, priorityValue);
        this.prestigePointsLostPerUnfedCharacter = prestigePointsLostPerUnfedCharacter;
    }

    public int getPrestigePointsLostPerUnfedCharacter() {
        return prestigePointsLostPerUnfedCharacter;
    }

    @Override
    public void apply(Game game) {
        for (Player player : game.getPlayers()) {
            int totalCharacters = player.getTribe().countCharacterCards();
            int gatherers = player.getTribe().countCharactersByType(GathererCard.class);

            int discountedCost = totalCharacters - (gatherers * FOOD_DISCOUNT_PER_GATHERER);
            int foodToPay = Math.max(0, discountedCost);

            int availableFood = player.getFood();
            int foodActuallyPaid = Math.min(availableFood, foodToPay);

            if (foodActuallyPaid > 0) {
                player.spendFood(foodActuallyPaid);
            }

            int unfedCharacters = foodToPay - foodActuallyPaid;
            if (unfedCharacters > 0) {
                player.removePrestigePoints(unfedCharacters * prestigePointsLostPerUnfedCharacter);
            }
        }
    }
}