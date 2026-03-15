package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;

public class Player {
    private String nickname;
    private int food;
    private int prestigePoints;
    private Tribe tribe;
    private Totem totem;

    public Player(String nickname, Totem totem) {
        this.nickname = nickname;
        this.totem = totem;
        this.food = 0;
        this.prestigePoints = 0;
        this.tribe = new Tribe();
    }

    // TODO : give initial food based on number of players
    public void addFood(int amount) {
        this.food += amount;
    }

    public boolean spendFood(int amount) {
        if (canAfford(amount)) {
            this.food -= amount;
            return true;
        }
        return false;
    }

    public void addPrestigePoints(int amount) {
        this.prestigePoints += amount;
    }

    public void removePrestigePoints(int amount) {
        this.prestigePoints -= amount;
    }

    public boolean canAfford(int cost) {
        return this.food >= cost;
    }

    public void addCharacterCard(CharacterCard card) {
        this.tribe.addCharacterCard(card);
    }

    public void addBuildingCard(BuildingCard card) {
        this.tribe.addBuildingCard(card);
    }

    public Tribe getTribe() {
        return tribe;
    }

    public Totem getTotem() {
        return totem;
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
}