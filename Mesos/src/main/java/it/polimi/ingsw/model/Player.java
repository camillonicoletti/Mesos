package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;

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
    // TODO : give initial food based on number of players
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

    public boolean spendFood(int amount) {
        if (food < amount) {
            return false;
        }

        food -= amount;
        return true;
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

    public void addCharacterCard(CharacterCard card) {
        tribe.addCharacterCard(card);

        // TODO: handle immediate character effects if needed
    }

    public void addBuildingCard(BuildingCard card) {
        tribe.addBuildingCard(card);

        // TODO: check building food cost
        // TODO: resolve Builder discount
        // TODO: prevent purchase if the player cannot afford it
    }
}

