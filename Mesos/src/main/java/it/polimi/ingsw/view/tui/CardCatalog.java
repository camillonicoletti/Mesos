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
        public final String detail;    // breve qualificatore (es. "+", "[9]", "2★", "+3 -1c", "FullSet")
        public final int foodCost;     // solo edifici (0 altrimenti)
        public final int pointsEnd;    // solo edifici (0 altrimenti)
        public final String eventTag;  // solo eventi (es. "Hunt", "Cave", ...)

        CardInfo(int id, Kind kind, String era, String typeName, String detail,
                 int foodCost, int pointsEnd, String eventTag) {
            this.id = id;
            this.kind = kind;
            this.era = era;
            this.typeName = typeName;
            this.detail = detail;
            this.foodCost = foodCost;
            this.pointsEnd = pointsEnd;
            this.eventTag = eventTag;
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

    // ── caricamento ──────────────────────────────────────────────────────────

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
            String fxName = effect.isJsonPrimitive()
                    ? effect.getAsString()
                    : effect.getAsJsonObject().get("name").getAsString();
            CARDS.put(id, new CardInfo(id, Kind.BUILDING, era, "Bldg",
                    shortFx(fxName), cost, pp, ""));
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
            CARDS.put(id, new CardInfo(id, Kind.EVENT, era, "Event", detail, 0, 0, tag));
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