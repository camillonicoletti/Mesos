package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.card.effect.EffectVisitor;
import it.polimi.ingsw.model.card.effect.SustenanceFoodDiscountEffect;
import it.polimi.ingsw.model.enums.Era;


/**
 * Event that makes each player feed their tribe.
 * Gatherers and some buildings reduce the food required; missing food becomes a point penalty.
 */
public class SustenanceEventCard extends EventCard {

    private static final long serialVersionUID = 1L;
    private static final int FOOD_DISCOUNT_PER_GATHERER = 3;

    private final int ppPenaltyPerUnfed;

    public SustenanceEventCard(int id, Era era, boolean closeEvent,
                               int ppPenaltyPerUnfed) {
        super(id, era, closeEvent);
        this.ppPenaltyPerUnfed = ppPenaltyPerUnfed;
    }

    public int getPpPenaltyPerUnfed() {
        return ppPenaltyPerUnfed;
    }

    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitSustenanceEvent(this);
    }

    /**
     * Makes every player pay food for their tribe or lose points for missing food.
     */
    @Override
    public void resolve(Game game) {
        for (Player player : game.getPlayers()) {
            int totalCharacters = player.getTribe().countCharacterCards();
            int gatherers = player.getTribe().countCharactersByType(GathererCard.class);

            int foodDiscount = gatherers * FOOD_DISCOUNT_PER_GATHERER;

            // Buildings with SustenanceFoodDiscountEffect reduce the food required.
            for (BuildingCard building : player.getTribe().getBuildingCards()) {
                foodDiscount += sustenanceDiscountFrom(player, building);
            }

            int foodRequired = Math.max(0, totalCharacters - foodDiscount);

            int availableFood = player.getFood();
            int foodPaid = Math.min(availableFood, foodRequired);

            if (foodPaid > 0) {
                player.spendFood(foodPaid);
            }

            int missingFood = foodRequired - foodPaid;

            if (missingFood > 0) {
                player.removePrestigePoints(missingFood * ppPenaltyPerUnfed);
            }
        }
    }

    /**
     * Reads the sustenance discount provided by a building effect.
     */
    private int sustenanceDiscountFrom(Player player, BuildingCard building) {
        return building.acceptEffect(new EffectVisitor<>() {
            @Override public Integer visitSustenanceFoodDiscount(SustenanceFoodDiscountEffect effect) {
                return player.getTribe().countCharactersByType(effect.getTargetType());
            }

            @Override public Integer visitEffect(Effect effect) {
                return 0;
            }
        });
    }

}
