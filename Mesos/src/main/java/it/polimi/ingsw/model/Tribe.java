package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.enums.InventorIcons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

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

    // Method used for FullSetFoodEffect and EndFullSetPPEffect
    public int countFullSets() {

        int[] counts = {
                countCharactersByType(ArtistCard.class),
                countCharactersByType(BuilderCard.class),
                countCharactersByType(GathererCard.class),
                countCharactersByType(HunterCard.class),
                countCharactersByType(InventorCard.class),
                countCharactersByType(ShamanCard.class)
        };

        int sets = counts[0];
        for (int count : counts) {
            sets = Math.min(sets, count);
        }

        return sets;

    }

    // Method for ShamanicRitualEvent
    public int countShamanStars() {
        int totalStars = 0;
        for (CharacterCard card : characterCards) {
            if (card instanceof ShamanCard shaman) {
                totalStars += shaman.getStars();
            }
        }
        return totalStars;
    }

    /**
     * Counts how many different invention icons are present among the Inventor cards in the tribe.
     * @return the number of distinct invention icons owned by the player
     */
    public int countDistinctInventorIcons() {
        Set<InventorIcons> distinctIcons = new HashSet<>();

        for (CharacterCard card : characterCards) {
            if (card instanceof InventorCard inventor) {
                distinctIcons.add(inventor.getInventionIcon());
            }
        }

        return distinctIcons.size();
    }



    // Method used for InventorPairFoodEffect
    public int countInventorPairs() {
        Map<InventorIcons, Integer> iconCounts = new HashMap<>();

        for (CharacterCard card : characterCards) {
            if (card instanceof InventorCard inventor) {
                InventorIcons icon = inventor.getInventionIcon();
                iconCounts.put(icon, iconCounts.getOrDefault(icon, 0) + 1);
            }
        }

        int pairs = 0;
        for (int count : iconCounts.values()) {
            pairs += count / 2;
        }
        return pairs;
    }
}