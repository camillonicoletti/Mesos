package it.polimi.ingsw.view.card;

import com.google.gson.JsonElement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The building effects plus an unknown fallback. Each instance owns its own rule
 * text, end-game scoring, summary icon and label; the base class provides the
 * defaults used by effects that do not override a given aspect. Callers dispatch
 * through polymorphic methods instead of switching on the effect name.
 */
public abstract class BuildingEffect {
    private static final String PRESTIGE_BADGE_PATH = "assets/icons/endgame_prestige_badge.png";

    public static final BuildingEffect FULL_SET_FOOD = new BuildingEffect("FullSetFood") {
        @Override public List<String> ruleLines(JsonElement effect) {
            return List.of(
                    "While you own this Building, gain 5 food each time you complete a new set of all 6 different Character types.",
                    "Sets completed before taking this Building do not grant the bonus.");
        }
    };

    public static final BuildingEffect SUSTENANCE_FOOD_DISCOUNT = new BuildingEffect("SustenanceFoodDiscount") {
        @Override public List<String> ruleLines(JsonElement effect) {
            return List.of(
                    "During the Sustenance Event, reduce total food paid by 1 for each "
                            + CharacterType.from(target(effect)).singular() + " in your tribe.");
        }
    };

    public static final BuildingEffect SHAMANIC_RITUAL_IMMUNITY = new BuildingEffect("ShamanicRitualImmunity") {
        @Override public List<String> ruleLines(JsonElement effect) {
            return List.of(
                    "During the Shamanic Ritual, you do not lose PP even if you have fewer stars than others.");
        }
    };

    public static final BuildingEffect TOTEM_SLOT_FOOD_BONUS = new BuildingEffect("TotemSlotFoodBonus") {
        @Override public List<String> ruleLines(JsonElement effect) {
            return List.of(
                    "When your totem returns to the Turn Order Tile, gain 1 extra food if the chosen slot already has a food bonus.",
                    "If you end up in the last slot, pay 1 food normally or lose 2 PP if you cannot.");
        }
    };

    public static final BuildingEffect INVENTOR_PAIR_FOOD = new BuildingEffect("InventorPairFood") {
        @Override public List<String> ruleLines(JsonElement effect) {
            return List.of(
                    "While you own this Building, gain 3 food each time you complete a new pair of matching Inventors.",
                    "Inventors with the same invention icon count as matching; pairs already present before taking this Building do not grant the bonus.");
        }
    };

    public static final BuildingEffect SHAMANIC_RITUAL_DOUBLE_PP = new BuildingEffect("ShamanicRitualDoublePP") {
        @Override public List<String> ruleLines(JsonElement effect) {
            return List.of(
                    "During the Shamanic Ritual, if you have more stars than all others, gain double the PP shown on the event card.",
                    "If tied for the lead, you still gain the PP shown on the event card.");
        }
    };

    public static final BuildingEffect SHAMAN_3_EXTRA_STARS = new BuildingEffect("Shaman3ExtraStars") {
        @Override public List<String> ruleLines(JsonElement effect) {
            return List.of("During the Shamanic Ritual, your tribe counts 3 additional stars.");
        }
    };

    public static final BuildingEffect HUNT_1_FOOD_1_PP = new BuildingEffect("Hunt1Food1PP") {
        @Override public List<String> ruleLines(JsonElement effect) {
            return List.of("During the Hunt Event, gain 1 extra food and 1 extra PP for each Hunter in your tribe.");
        }
    };

    public static final BuildingEffect CAVE_PAINTINGS_FOOD = new BuildingEffect("CavePaintingsFood") {
        @Override public List<String> ruleLines(JsonElement effect) {
            return List.of("During the Cave Paintings Event, gain 1 food for each Artist in your tribe.");
        }
    };

    public static final BuildingEffect EXTRA_CARD_UPPER_ROW = new BuildingEffect("ExtraCardUpperRow") {
        @Override public List<String> ruleLines(JsonElement effect) {
            return List.of(
                    "After all players resolve actions and before End of Round, take 1 card from the upper row.",
                    "If you choose a Building, you still pay its cost, applying any Builder discounts.");
        }
    };

    public static final BuildingEffect END_BUILDER_PP = new BuildingEffect("EndBuilderPP") {
        @Override public List<String> ruleLines(JsonElement effect) {
            return List.of("At end of game, double the printed PP on all your Builder cards.");
        }

        @Override public int endGamePoints(JsonElement effect, BuildingScoring scoring) {
            return scoring.builderCharacterPoints();
        }

        @Override public String label(JsonElement effect, int cardId) {
            return "Builders building #" + cardId;
        }
    };

    public static final BuildingEffect END_FULL_SET_PP = new BuildingEffect("EndFullSetPP") {
        @Override public List<String> ruleLines(JsonElement effect) {
            return List.of("At end of game, gain 6 PP for each complete set of all 6 different Character types in your tribe.");
        }

        @Override public int endGamePoints(JsonElement effect, BuildingScoring scoring) {
            return scoring.fullSetCount() * 6;
        }

        @Override public String iconPath(JsonElement effect) {
            return CharacterType.INVENTOR.iconPath();
        }

        @Override public String label(JsonElement effect, int cardId) {
            return "Full-set building #" + cardId;
        }
    };

    public static final BuildingEffect END_PP = new BuildingEffect("EndPP") {
        @Override public List<String> ruleLines(JsonElement effect) {
            return List.of("Gain 25 PP at end of game from this effect.");
        }

        @Override public int endGamePoints(JsonElement effect, BuildingScoring scoring) {
            return 25;
        }

        @Override public String iconPath(JsonElement effect) {
            return PRESTIGE_BADGE_PATH;
        }

        @Override public String label(JsonElement effect, int cardId) {
            return "Prestige building #" + cardId;
        }
    };

    public static final BuildingEffect END_CHARACTER_COUNT_PP = new BuildingEffect("EndCharacterCountPP") {
        @Override public List<String> ruleLines(JsonElement effect) {
            return List.of(
                    "At end of game, gain " + amount(effect, "pointsPerCard") + " PP for each "
                            + CharacterType.from(target(effect)).singular() + " in your tribe.");
        }

        @Override public int endGamePoints(JsonElement effect, BuildingScoring scoring) {
            return scoring.characterTypeCount(target(effect)) * amount(effect, "pointsPerCard");
        }

        @Override public String iconPath(JsonElement effect) {
            return CharacterType.from(target(effect)).iconPath();
        }

        @Override public String label(JsonElement effect, int cardId) {
            return CharacterType.from(target(effect)).plural() + " building #" + cardId;
        }
    };

    public static final BuildingEffect UNKNOWN = new BuildingEffect("") {
    };

    private static final List<BuildingEffect> KNOWN = List.of(
            FULL_SET_FOOD, SUSTENANCE_FOOD_DISCOUNT, SHAMANIC_RITUAL_IMMUNITY, TOTEM_SLOT_FOOD_BONUS,
            INVENTOR_PAIR_FOOD, SHAMANIC_RITUAL_DOUBLE_PP, SHAMAN_3_EXTRA_STARS, HUNT_1_FOOD_1_PP,
            CAVE_PAINTINGS_FOOD, EXTRA_CARD_UPPER_ROW, END_BUILDER_PP, END_FULL_SET_PP,
            END_PP, END_CHARACTER_COUNT_PP);
    private static final Map<String, BuildingEffect> BY_TOKEN = new LinkedHashMap<>();

    static {
        for (BuildingEffect effect : KNOWN) {
            BY_TOKEN.put(effect.token, effect);
        }
    }

    private final String token;

    protected BuildingEffect(String token) {
        this.token = token;
    }

    public String token() {
        return token;
    }

    public List<String> ruleLines(JsonElement effect) {
        return List.of("Building.");
    }

    public int endGamePoints(JsonElement effect, BuildingScoring scoring) {
        return 0;
    }

    public String iconPath(JsonElement effect) {
        return CharacterType.BUILDER.iconPath();
    }

    public String label(JsonElement effect, int cardId) {
        return "Building #" + cardId;
    }

    public static BuildingEffect from(String token) {
        return BY_TOKEN.getOrDefault(token, UNKNOWN);
    }

    protected static String target(JsonElement effect) {
        return effect != null && effect.isJsonObject()
                ? CardJson.string(effect.getAsJsonObject(), "targetType")
                : "";
    }

    protected static int amount(JsonElement effect, String key) {
        return effect != null && effect.isJsonObject()
                ? CardJson.integer(effect.getAsJsonObject(), key)
                : 0;
    }
}
