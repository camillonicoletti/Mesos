package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;


import java.util.ArrayList;
import java.util.List;

public class Tribe {
    private List<CharacterCard> characterCards;
    private List<BuildingCard> buildingCards;

    public Tribe() {
        this.characterCards = new ArrayList<>();
        this.buildingCards = new ArrayList<>();
    }

    public void addCharacterCard(CharacterCard card) {
        this.characterCards.add(card);
    }

    public void addBuildingCard(BuildingCard card) {
        this.buildingCards.add(card);
    }

    public int countCharacterCards() {
        return this.characterCards.size();
    }

    public int countCharactersByType(Class<? extends CharacterCard> type) {
        int count = 0;
        for (CharacterCard card : characterCards) {
            if (type.isInstance(card)) {
                count++;
            }
        }
            return count;
    }

    public int getBuildingDiscount() {
        // TODO
        return 0;
    }

    public List<CharacterCard> getCharacterCards() {
        return characterCards;
    }

    public List<BuildingCard> getBuildingCards() {
        return buildingCards;
    }
}