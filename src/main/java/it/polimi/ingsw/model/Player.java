package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.card.effect.EffectVisitor;
import it.polimi.ingsw.model.card.effect.FullSetFoodEffect;
import it.polimi.ingsw.model.card.effect.InventorPairFoodEffect;

import java.io.Serializable;

/**
 * Represents one player in the match.
 * It stores food, points, tribe cards and the player's totem.
 */
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

    /**
     * Adds food to the player.
     */
    public void addFood(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Food amount cannot be negative.");
        }
        this.food += amount;
    }

    /**
     * Spends food from the player's personal reserve.
     */
    public void spendFood(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Food amount cannot be negative.");
        }
        if (amount > food) {
            throw new IllegalStateException("Not enough food.");
        }
        this.food -= amount;
    }
    public boolean canAfford(int cost) {
        return food >= cost;
    }

    /**
     * Adds prestige points to the player.
     */
    public void addPrestigePoints(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Prestige point amount cannot be negative.");
        }
        this.prestigePoints += amount;
    }

    /**
     * Removes prestige points from the player.
     */
    public void removePrestigePoints(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Prestige point amount cannot be negative.");
        }
        this.prestigePoints -= amount;
    }

    /**
     * Applies a penalty by spending food first, or points if food is missing.
     */
    public void applyTurnPenalty(int foodCost, int prestigePenalty) {
        if (canAfford(foodCost)) {
            spendFood(foodCost);
        } else {
            removePrestigePoints(prestigePenalty);
        }
    }

    /**
     * Adds a character card and checks if owned buildings react to it.
     */
    public void addCharacterCard(CharacterCard card) {
        tribe.addCharacterCard(card);

        // Some buildings give rewards when the tribe composition changes.
        for (BuildingCard building : tribe.getBuildingCards()) {
            applyCharacterAddedEffect(building);
        }
    }

    /**
     * Adds a building card and stores its starting progress.
     */
    public void addBuildingCard(BuildingCard card) {
        tribe.addBuildingCard(card);
        initializeStoredProgress(card);
    }

    private void applyCharacterAddedEffect(BuildingCard building) {
        building.acceptEffect(new EffectVisitor<Void>() {
            @Override public Void visitFullSetFood(FullSetFoodEffect effect) {
                effect.apply(Player.this);
                return null;
            }

            @Override public Void visitInventorPairFood(InventorPairFoodEffect effect) {
                effect.apply(Player.this);
                return null;
            }

            @Override public Void visitEffect(Effect effect) {
                return null;
            }
        });
    }

    private void initializeStoredProgress(BuildingCard card) {
        card.acceptEffect(new EffectVisitor<Void>() {
            @Override public Void visitFullSetFood(FullSetFoodEffect effect) {
                effect.setRewardedSets(tribe.countFullSets());
                return null;
            }

            @Override public Void visitInventorPairFood(InventorPairFoodEffect effect) {
                effect.setRewardedPairs(tribe.countInventorPairs());
                return null;
            }

            @Override public Void visitEffect(Effect effect) {
                return null;
            }
        });
    }
}
