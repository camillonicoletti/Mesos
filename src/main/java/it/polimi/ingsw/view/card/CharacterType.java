package it.polimi.ingsw.view.card;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Represents the 6 character card types.
// Each type defines its icon, rule text and scoring data.
// It helps the view describe and count character cards.

public abstract class CharacterType {
    private static final int FOOD_DISCOUNT_PER_GATHERER = 3;
    private static final String ICON_DIR = "assets/icons/summary/";

    public static final CharacterType HUNTER = new CharacterType("hunter") {
        @Override public String singular() { return "Hunter"; }
        @Override public String plural() { return "Hunters"; }
        @Override public String iconPath() { return ICON_DIR + "hunter.png"; }

        //ruleLines(JsonObject details) creates the descriptive text shown in the view to explain what the card does.`
        @Override public List<String> ruleLines(JsonObject details) {
            return CardJson.bool(details, "foodIcon")
                    ? List.of(
                    "Hunter: counts during the Hunt Event to earn food and PP as shown on the event card.",
                    "When taken, immediately gain 1 food for each Hunter in your tribe, including this one.")
                    : List.of(
                    "Hunter: counts during the Hunt Event to earn food and PP as shown on the event card.",
                    "Has no food icon, so taking it does not grant immediate food.");
        }

        @Override public void tally(CharacterTally tally, JsonObject details) {
            tally.addHunter();
        }
    };

    public static final CharacterType BUILDER = new CharacterType("builder") {
        @Override public String singular() { return "Builder"; }
        @Override public String plural() { return "Builders"; }
        @Override public String iconPath() { return ICON_DIR + "builder.png"; }

        @Override public List<String> ruleLines(JsonObject details) {
            return List.of(
                    "Builder: reduces the cost of each Building you take by " + CardJson.integer(details, "foodDiscount") + ".",
                    "Builder discounts stack, but the cost cannot drop below 0.",
                    "Worth " + CardJson.integer(details, "pointsEndOfMatch") + " PP at end of game.");
        }

        @Override public void tally(CharacterTally tally, JsonObject details) {
            tally.addBuilder(CardJson.integer(details, "foodDiscount"));
        }
    };

    public static final CharacterType GATHERER = new CharacterType("gatherer") {
        @Override public String singular() { return "Gatherer"; }
        @Override public String plural() { return "Gatherers"; }
        @Override public String iconPath() { return ICON_DIR + "gatherer.png"; }

        @Override public List<String> ruleLines(JsonObject details) {
            return List.of(
                    "Gatherer: during the Sustenance Event, reduces the total food you pay by "
                            + FOOD_DISCOUNT_PER_GATHERER + ".",
                    "Does not provide food when added to your tribe.");
        }

        @Override public void tally(CharacterTally tally, JsonObject details) {
            tally.addGatherer();
        }
    };

    public static final CharacterType ARTIST = new CharacterType("artist") {
        @Override public String singular() { return "Artist"; }
        @Override public String plural() { return "Artists"; }
        @Override public String iconPath() { return ICON_DIR + "artist.png"; }

        @Override public List<String> ruleLines(JsonObject details) {
            return List.of(
                    "Artist: during the Cave Paintings Event, determines whether you lose or gain PP per artist.",
                    "At end of game, gain 10 PP for each pair of Artists in your tribe.");
        }

        @Override public void tally(CharacterTally tally, JsonObject details) {
            tally.addArtist();
        }
    };

    public static final CharacterType INVENTOR = new CharacterType("inventor") {
        @Override public String singular() { return "Inventor"; }
        @Override public String plural() { return "Inventors"; }
        @Override public String iconPath() { return ICON_DIR + "inventor.png"; }

        @Override public List<String> ruleLines(JsonObject details) {
            return List.of(
                    "Inventor: carries invention icon " + CardJson.string(details, "inventionIcon") + ".",
                    "End-game score: number of Inventors × number of distinct invention icons.");
        }

        @Override public void tally(CharacterTally tally, JsonObject details) {
            tally.addInventor(CardJson.string(details, "inventionIcon"));
        }
    };

    public static final CharacterType SHAMAN = new CharacterType("shaman") {
        @Override public String singular() { return "Shaman"; }
        @Override public String plural() { return "Shamans"; }
        @Override public String iconPath() { return ICON_DIR + "shaman.png"; }

        @Override public List<String> ruleLines(JsonObject details) {
            return List.of(
                    "Shaman: adds " + CardJson.integer(details, "stars") + " stars to your tribe.",
                    "During the Shamanic Ritual, more stars earn PP; fewer stars lose PP.");
        }

        @Override public void tally(CharacterTally tally, JsonObject details) {
            tally.addShaman(CardJson.integer(details, "stars"));
        }
    };

    public static final CharacterType UNKNOWN = new CharacterType("") {
        @Override public String singular() { return "Character"; }
        @Override public String plural() { return "Characters"; }
        @Override public String iconPath() { return ICON_DIR + "builder.png"; }

        @Override public List<String> ruleLines(JsonObject details) {
            return List.of("Character card.");
        }

        @Override public void tally(CharacterTally tally, JsonObject details) {
        }
    };

    private static final List<CharacterType> PLAYABLE =
            List.of(HUNTER, BUILDER, GATHERER, ARTIST, INVENTOR, SHAMAN);
    private static final Map<String, CharacterType> BY_TOKEN = new LinkedHashMap<>();

    static {
        for (CharacterType type : PLAYABLE) {
            BY_TOKEN.put(type.token, type);
        }
    }

    private final String token;

    protected CharacterType(String token) {
        this.token = token;
    }

    public String token() {
        return token;
    }

    public abstract String singular();

    public abstract String plural();

    public abstract String iconPath();

    public abstract List<String> ruleLines(JsonObject details);

    public abstract void tally(CharacterTally tally, JsonObject details);

    public static CharacterType from(String token) {
        return BY_TOKEN.getOrDefault(token, UNKNOWN);
    }

    public static List<CharacterType> playable() {
        return PLAYABLE;
    }
}
