package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.effect.FullSetFoodEffect;
import it.polimi.ingsw.model.card.effect.InventorPairFoodEffect;

import java.io.Serializable;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String nickname;
    private int food;
    private int prestigePoints;
    private final Tribe tribe;
    private final Totem totem;
    private transient Game game;

    public Player(String nickname, Totem totem) {
        this.nickname = nickname;
        this.food = 0;
        this.prestigePoints = 0;
        this.tribe = new Tribe();
        this.totem = totem;

        // TODO: nickname uniqueness must be checked by the server/lobby
        // TODO: initial food assignment belongs to game setup, not to Player
    }
    public void setGame(Game game) {
        this.game = game;
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
        if (amount == 0) return;
        food += amount;
        if (game != null) game.notifyListeners
                (l -> l.onFoodChanged(nickname, food, amount));
    }

    public void spendFood(int amount) {
        if (amount == 0) return;
        if (amount < 0) throw new IllegalArgumentException("Cannot spend negative food");
        if (amount > food) throw new IllegalStateException("Not enough food");
        food -= amount;
        if (game != null) game.notifyListeners
                (l -> l.onFoodChanged(nickname, food, -amount));
    }

    public boolean canAfford(int cost) {
        return food >= cost;
    }

    public void addPrestigePoints(int amount) {
        if (amount == 0) return;
        prestigePoints += amount;
        if (game != null) game.notifyListeners
                (l -> l.onPointsChanged(nickname, prestigePoints, amount));
    }

    public void removePrestigePoints(int amount) {
        if (amount == 0) return;
        prestigePoints -= amount;
        if (game != null) game.notifyListeners
                (l -> l.onPointsChanged(nickname, prestigePoints, -amount));
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

