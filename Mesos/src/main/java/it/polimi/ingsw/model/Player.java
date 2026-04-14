package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.effect.FullSetFoodEffect;
import it.polimi.ingsw.model.card.effect.InventorPairFoodEffect;

public class Player {

    private final String nickname;
    private int food;
    private int prestigePoints;
    private final Tribe tribe;
    private final Totem totem;

    public Player(String nickname, Totem totem) {
        this.nickname = nickname;
        this.food = 0;
        this.prestigePoints = 0;
        this.tribe = new Tribe();
        this.totem = totem;

        // TODO: nickname uniqueness must be checked by the server/lobby
        // TODO: initial food assignment belongs to game setup, not to Player
    }

    public String getNickname() {
        return nickname;
    }

    public int getFood() {
        return food;
    }

    public int getPrestigePoints() {
        return prestigePoints;
    }

    public Tribe getTribe() {
        return tribe;
    }

    public Totem getTotem() {
        return totem;
    }

    public void addFood(int amount) {
        food += amount;

        // TODO: add validation if needed
    }

    public void spendFood(int amount) {
        if (food >= amount) {
            food -= amount;
        } else {
            int missing = amount - food;
            food = 0;
            removePrestigePoints(missing);
        }
    }

    public boolean canAfford(int cost) {
        return food >= cost;
    }

    public void addPrestigePoints(int amount) {
        prestigePoints += amount;
    }

    public void removePrestigePoints(int amount) {
        prestigePoints -= amount;

        // Prestige points can go below zero
    }

    /**
     * Applies a turn order penalty: spends foodCost food if affordable,
     * otherwise removes prestigePenalty prestige points.
     */
    public void applyTurnPenalty(int foodCost, int prestigePenalty) {
        if (canAfford(foodCost)) {
            spendFood(foodCost);
        } else {
            removePrestigePoints(prestigePenalty);
        }
    }


    public void addCharacterCard(CharacterCard card) {
        tribe.addCharacterCard(card);

        // trigger building effects that activate when a character card is added
        for (BuildingCard building : tribe.getBuildingCards()) {
            if (building.getEffect() instanceof FullSetFoodEffect ||
                    building.getEffect() instanceof InventorPairFoodEffect) {
                building.getEffect().apply(this);
            }
        }
    }


    public void addBuildingCard(BuildingCard card) {
        tribe.addBuildingCard(card);
    }
}

