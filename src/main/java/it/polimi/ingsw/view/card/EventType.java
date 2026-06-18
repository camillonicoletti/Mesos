package it.polimi.ingsw.view.card;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The four era events plus an unknown fallback. Each instance owns its own rule
 * text and the summary icon that represents it, so callers dispatch through
 * polymorphic methods instead of switching on a token.
 */
public abstract class EventType {
    private static final String PRESTIGE_BADGE_PATH = "assets/icons/endgame_prestige_badge.png";

    public static final EventType HUNT = new EventType("hunt") {
        @Override public String ruleText(JsonObject details, String suffix) {
            return "Hunt: each Hunter provides " + CardJson.integer(details, "foodPerHunter")
                    + " food and " + CardJson.integer(details, "ppPerHunter") + " PP." + suffix;
        }

        @Override public String iconPath() {
            return CharacterType.HUNTER.iconPath();
        }
    };

    public static final EventType SUSTENANCE = new EventType("sustenance") {
        @Override public String ruleText(JsonObject details, String suffix) {
            return "Sustenance: each unfed character causes -"
                    + CardJson.integer(details, "ppPenaltyPerUnfed") + " PP." + suffix;
        }

        @Override public String iconPath() {
            return CharacterType.GATHERER.iconPath();
        }
    };

    public static final EventType SHAMANIC_RITUAL = new EventType("shamanicRitual") {
        @Override public String ruleText(JsonObject details, String suffix) {
            return "Shamanic Ritual: whoever has the most stars gains "
                    + CardJson.integer(details, "ppGain") + " PP; whoever has the fewest loses "
                    + CardJson.integer(details, "ppLoss") + " PP." + suffix;
        }

        @Override public String iconPath() {
            return CharacterType.SHAMAN.iconPath();
        }
    };

    public static final EventType CAVE_PAINTINGS = new EventType("cavePaintings") {
        @Override public String ruleText(JsonObject details, String suffix) {
            return "Cave Paintings: you need at least " + CardJson.integer(details, "minArtists")
                    + " artists; if you don't have them lose " + CardJson.integer(details, "ppPenalty")
                    + " PP, otherwise gain " + CardJson.integer(details, "ppPerArtist")
                    + " PP per artist." + suffix;
        }

        @Override public String iconPath() {
            return CharacterType.ARTIST.iconPath();
        }
    };

    public static final EventType UNKNOWN = new EventType("") {
        @Override public String ruleText(JsonObject details, String suffix) {
            return "Event." + suffix;
        }

        @Override public String iconPath() {
            return PRESTIGE_BADGE_PATH;
        }
    };

    private static final List<EventType> KNOWN =
            List.of(HUNT, SUSTENANCE, SHAMANIC_RITUAL, CAVE_PAINTINGS);
    private static final Map<String, EventType> BY_TOKEN = new LinkedHashMap<>();

    static {
        for (EventType type : KNOWN) {
            BY_TOKEN.put(type.token, type);
        }
    }

    private final String token;

    protected EventType(String token) {
        this.token = token;
    }

    public String token() {
        return token;
    }

    public abstract String ruleText(JsonObject details, String suffix);

    public abstract String iconPath();

    public static EventType from(String token) {
        return BY_TOKEN.getOrDefault(token, UNKNOWN);
    }
}
