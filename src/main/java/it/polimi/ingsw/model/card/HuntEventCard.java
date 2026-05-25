package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.card.effect.EffectVisitor;
import it.polimi.ingsw.model.card.effect.Hunt1Food1PPEffect;
import it.polimi.ingsw.model.enums.Era;


public class HuntEventCard extends EventCard {
    private static final long serialVersionUID = 1L;
    private final int foodPerHunter;
    private final int ppPerHunter;

    public HuntEventCard(int id, Era era, boolean closeEvent,
                         int foodPerHunter, int ppPerHunter) {
        super(id, era, closeEvent);
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

            player.addFood(hunters * foodPerHunter);
            player.addPrestigePoints(hunters * ppPerHunter);

            // Apply building bonuses triggered by the Hunt event.
            for (BuildingCard building : player.getTribe().getBuildingCards()) {
                applyHuntEffect(player, building);
            }
        }
    }

    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitHuntEvent(this);
    }

    private void applyHuntEffect(Player player, BuildingCard building) {
        building.acceptEffect(new EffectVisitor<Void>() {
            @Override public Void visitHunt1Food1PP(Hunt1Food1PPEffect effect) {
                effect.apply(player);
                return null;
            }

            @Override public Void visitEffect(Effect effect) {
                return null;
            }
        });
    }

}
