package it.polimi.ingsw.view.tui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class CardCatalog {

    public enum Kind { CHARACTER, BUILDING, EVENT, UNKNOWN }

    public static final class CardInfo {
        public final int id;
        public final Kind kind;
        public final String era;       // "I" / "II" / "III"
        public final String typeName;  // "Hunter", "Builder", "Bldg", "Event", ...
        public final String detail;    // short qualifier, e.g. "+", "[9]", "2★", "+3 -1c", "FullSet"
        public final int foodCost;     // buildings only, 0 otherwise
        public final int pointsEnd;    // buildings only, 0 otherwise
        public final String buildingEffectName;
        public final String buildingEffectTarget;
        public final int buildingEffectPointsPerCard;
        public final String eventTag;  // events only, e.g. "Hunt", "Cave", ...
        public final int eventFood;
        public final int eventPoints;
        public final int eventRequired;
        public final int eventPenalty;

        CardInfo(int id, Kind kind, String era, String typeName, String detail,
                 int foodCost, int pointsEnd, String eventTag) {
            this(id, kind, era, typeName, detail, foodCost, pointsEnd,
                    "", "", 0, eventTag, 0, 0, 0, 0);
        }

        CardInfo(int id, Kind kind, String era, String typeName, String detail,
                 int foodCost, int pointsEnd, String eventTag, int eventFood, int eventPoints) {
            this(id, kind, era, typeName, detail, foodCost, pointsEnd,
                    "", "", 0, eventTag, eventFood, eventPoints, 0, 0);
        }

        CardInfo(int id, Kind kind, String era, String typeName, String detail,
                 int foodCost, int pointsEnd,
                 String buildingEffectName, String buildingEffectTarget, int buildingEffectPointsPerCard,
                 String eventTag) {
            this(id, kind, era, typeName, detail, foodCost, pointsEnd,
                    buildingEffectName, buildingEffectTarget, buildingEffectPointsPerCard,
                    eventTag, 0, 0, 0, 0);
        }

        CardInfo(int id, Kind kind, String era, String typeName, String detail,
                 int foodCost, int pointsEnd,
                 String buildingEffectName, String buildingEffectTarget, int buildingEffectPointsPerCard,
                 String eventTag,
                 int eventFood, int eventPoints, int eventRequired, int eventPenalty) {
            this.id = id;
            this.kind = kind;
            this.era = era;
            this.typeName = typeName;
            this.detail = detail;
            this.foodCost = foodCost;
            this.pointsEnd = pointsEnd;
            this.buildingEffectName = buildingEffectName;
            this.buildingEffectTarget = buildingEffectTarget;
            this.buildingEffectPointsPerCard = buildingEffectPointsPerCard;
            this.eventTag = eventTag;
            this.eventFood = eventFood;
            this.eventPoints = eventPoints;
            this.eventRequired = eventRequired;
            this.eventPenalty = eventPenalty;
        }
    }

    private static final Map<Integer, CardInfo> CARDS = new HashMap<>();

    static {
        loadCharacters();
        loadBuildings();
        loadEvents();
    }

    public static CardInfo get(int id) {
        return CARDS.getOrDefault(id, new CardInfo(id, Kind.UNKNOWN, "?", "?", "", 0, 0, ""));
    }

    public static String label(int id) {
        CardInfo c = get(id);
        return "#" + c.id + " " + c.era + " " + c.typeName
                + (c.detail.isEmpty() ? "" : " " + c.detail);
    }

    // ── loading ──────────────────────────────────────────────────────────────

    private static void loadCharacters() {
        for (JsonElement el : safeRead("characters.json")) {
            JsonObject o = el.getAsJsonObject();
            int id = o.get("id").getAsInt();
            String type = o.get("type").getAsString();
            String era = o.get("era").getAsString();
            String typeName;
            String detail;
            switch (type) {
                case "hunter" -> {
                    typeName = "Hunter";
                    detail = o.get("foodIcon").getAsBoolean() ? "+" : "";
                }
                case "builder" -> {
                    typeName = "Builder";
                    int pp = o.get("pointsEndOfMatch").getAsInt();
                    int disc = o.get("foodDiscount").getAsInt();
                    detail = "+" + pp + " -" + disc + "c";
                }
                case "gatherer" -> { typeName = "Gatherer"; detail = ""; }
                case "artist"   -> { typeName = "Artist";   detail = ""; }
                case "inventor" -> {
                    typeName = "Inventor";
                    detail = "[" + o.get("inventionIcon").getAsString() + "]";
                }
                case "shaman" -> {
                    typeName = "Shaman";
                    detail = o.get("stars").getAsInt() + "★";
                }
                default -> { typeName = type; detail = ""; }
            }
            CARDS.put(id, new CardInfo(id, Kind.CHARACTER, era, typeName, detail, 0, 0, ""));
        }
    }

    private static void loadBuildings() {
        for (JsonElement el : safeRead("buildings.json")) {
            JsonObject o = el.getAsJsonObject();
            int id = o.get("id").getAsInt();
            String era = o.get("era").getAsString();
            int cost = o.get("foodCost").getAsInt();
            int pp = o.get("pointsEndOfMatch").getAsInt();
            JsonElement effect = o.get("effect");
            String fxName;
            String targetType = "";
            int pointsPerCard = 0;
            if (effect.isJsonPrimitive()) {
                fxName = effect.getAsString();
            } else {
                JsonObject fx = effect.getAsJsonObject();
                fxName = fx.get("name").getAsString();
                if (fx.has("targetType")) {
                    targetType = fx.get("targetType").getAsString();
                }
                if (fx.has("pointsPerCard")) {
                    pointsPerCard = fx.get("pointsPerCard").getAsInt();
                }
            }
            CARDS.put(id, new CardInfo(id, Kind.BUILDING, era, "Bldg",
                    shortFx(fxName), cost, pp, fxName, targetType, pointsPerCard, ""));
        }
    }

    private static void loadEvents() {
        for (JsonElement el : safeRead("events.json")) {
            JsonObject o = el.getAsJsonObject();
            int id = o.get("id").getAsInt();
            String era = o.get("era").getAsString();
            String type = o.get("type").getAsString();
            boolean close = o.get("closeEvent").getAsBoolean();
            String tag = shortEvType(type);
            String detail = close ? tag + "★" : tag;
            int eventFood = "hunt".equals(type) ? o.get("foodPerHunter").getAsInt() : 0;
            int eventPoints = switch (type) {
                case "hunt" -> o.get("ppPerHunter").getAsInt();
                case "cavePaintings" -> o.get("ppPerArtist").getAsInt();
                case "shamanicRitual" -> o.get("ppGain").getAsInt();
                default -> 0;
            };
            int eventRequired = "cavePaintings".equals(type) ? o.get("minArtists").getAsInt() : 0;
            int eventPenalty = switch (type) {
                case "cavePaintings" -> o.get("ppPenalty").getAsInt();
                case "sustenance" -> o.get("ppPenaltyPerUnfed").getAsInt();
                case "shamanicRitual" -> o.get("ppLoss").getAsInt();
                default -> 0;
            };
            CARDS.put(id, new CardInfo(id, Kind.EVENT, era, "Event", detail, 0, 0,
                    "", "", 0, tag, eventFood, eventPoints, eventRequired, eventPenalty));
        }
    }

    private static String shortFx(String name) {
        return switch (name) {
            case "FullSetFood"            -> "FullSet";
            case "SustenanceFoodDiscount" -> "FoodDisc";
            case "ShamanicRitualImmunity" -> "ShamImm";
            case "TotemSlotFoodBonus"     -> "TotemBon";
            case "InventorPairFood"       -> "InvPair";
            case "ShamanicRitualDoublePP" -> "Sham2x";
            case "Shaman3ExtraStars"      -> "Sham+3";
            case "Hunt1Food1PP"           -> "Hunt+PP";
            case "EndBuilderPP"           -> "Bld>PP";
            case "CavePaintingsFood"      -> "CaveFd";
            case "ExtraCardUpperRow"      -> "+Upper";
            case "EndPP"                  -> "EndPP";
            case "EndCharacterCountPP"    -> "Cnt>PP";
            default -> name;
        };
    }

    private static String shortEvType(String t) {
        return switch (t) {
            case "hunt"           -> "Hunt";
            case "sustenance"     -> "Sust";
            case "shamanicRitual" -> "Sham";
            case "cavePaintings"  -> "Cave";
            default -> t;
        };
    }

    private static JsonArray safeRead(String name) {
        try (InputStream is = CardCatalog.class.getClassLoader().getResourceAsStream(name)) {
            if (is == null) return new JsonArray();
            try (InputStreamReader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(r).getAsJsonArray();
            }
        } catch (Exception e) {
            return new JsonArray();
        }
    }

    private CardCatalog() {}
}
