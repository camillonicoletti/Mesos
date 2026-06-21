package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.enums.InventorIcons;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;

/**
 * Stores the cards owned by a player.
 * It also counts useful groups of cards for effects and final scoring.
 */
public class Tribe implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Map<Class<? extends CharacterCard>, CardVisitor<Boolean>> CHARACTER_TYPE_MATCHERS = Map.of(
            ArtistCard.class, new CardVisitor<>() {
                @Override public Boolean visitArtist(ArtistCard card) { return true; }
                @Override public Boolean visitCard(Card card) { return false; }
            },
            BuilderCard.class, new CardVisitor<>() {
                @Override public Boolean visitBuilder(BuilderCard card) { return true; }
                @Override public Boolean visitCard(Card card) { return false; }
            },
            GathererCard.class, new CardVisitor<>() {
                @Override public Boolean visitGatherer(GathererCard card) { return true; }
                @Override public Boolean visitCard(Card card) { return false; }
            },
            HunterCard.class, new CardVisitor<>() {
                @Override public Boolean visitHunter(HunterCard card) { return true; }
                @Override public Boolean visitCard(Card card) { return false; }
            },
            InventorCard.class, new CardVisitor<>() {
                @Override public Boolean visitInventor(InventorCard card) { return true; }
                @Override public Boolean visitCard(Card card) { return false; }
            },
            ShamanCard.class, new CardVisitor<>() {
                @Override public Boolean visitShaman(ShamanCard card) { return true; }
                @Override public Boolean visitCard(Card card) { return false; }
            }
    );

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

    /**
     * Counts how many characters of a specific type the tribe has.
     */
    public int countCharactersByType(Class<? extends CharacterCard> type) {
        CardVisitor<Boolean> matcher = CHARACTER_TYPE_MATCHERS.get(type);
        if (matcher == null) {
            throw new IllegalArgumentException("Unsupported character type: " + type.getSimpleName());
        }
        return countMatching(matcher);
    }

    /**
     * Sums all food discounts given by builder cards.
     */
    public int getBuildingDiscount() {
        return sumValues(new CardVisitor<>() {
            @Override public Integer visitBuilder(BuilderCard builder) {
                return builder.getFoodDiscount();
            }

            @Override public Integer visitCard(Card card) {
                return 0;
            }
        });
    }

    public List<CharacterCard> getCharacterCards() {
        return Collections.unmodifiableList(characterCards);
    }

    public List<BuildingCard> getBuildingCards() {
        return Collections.unmodifiableList(buildingCards);
    }

    /**
     * Counts complete sets made of one card for each character type.
     */
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

    /**
     * Counts stars printed on Shaman cards.
     */
    public int countShamanStars() {
        return sumValues(new CardVisitor<>() {
            @Override public Integer visitShaman(ShamanCard shaman) {
                return shaman.getStars();
            }

            @Override public Integer visitCard(Card card) {
                return 0;
            }
        });
    }

    /**
     * Counts how many different invention icons the player owns.
     */
    public int countDistinctInventorIcons() {
        Set<InventorIcons> distinctIcons = new HashSet<>();
        CardVisitor<Void> collector = new CardVisitor<>() {
            @Override public Void visitInventor(InventorCard inventor) {
                distinctIcons.add(inventor.getInventionIcon());
                return null;
            }

            @Override public Void visitCard(Card card) {
                return null;
            }
        };
        for (CharacterCard card : characterCards) {
            card.accept(collector);
        }

        return distinctIcons.size();
    }

    /**
     * Counts pairs of Inventor cards with the same icon.
     */
    public int countInventorPairs() {
        Map<InventorIcons, Integer> iconCounts = new HashMap<>();
        CardVisitor<Void> collector = new CardVisitor<>() {
            @Override public Void visitInventor(InventorCard inventor) {
                InventorIcons icon = inventor.getInventionIcon();
                iconCounts.put(icon, iconCounts.getOrDefault(icon, 0) + 1);
                return null;
            }

            @Override public Void visitCard(Card card) {
                return null;
            }
        };
        for (CharacterCard card : characterCards) {
            card.accept(collector);
        }

        int pairs = 0;
        for (int count : iconCounts.values()) {
            pairs += count / 2;
        }
        return pairs;
    }

    /**
     * Counts pairs of Artist cards.
     */
    public int countArtistPairs() {
        int artists = countCharactersByType(ArtistCard.class);
        return artists / 2;
    }

    private int countMatching(CardVisitor<Boolean> matcher) {
        int count = 0;
        for (CharacterCard card : characterCards) {
            if (Boolean.TRUE.equals(card.accept(matcher))) {
                count++;
            }
        }
        return count;
    }

    private int sumValues(CardVisitor<Integer> visitor) {
        int total = 0;
        for (CharacterCard card : characterCards) {
            total += card.accept(visitor);
        }
        return total;
    }
}
