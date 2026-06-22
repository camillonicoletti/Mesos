package it.polimi.ingsw.view.tui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.polimi.ingsw.view.card.CardDataCatalog;

import java.util.HashMap;
import java.util.Map;

public final class CardCatalog {


    public interface CardVisitor<R> {
        default R visitCharacter(CardInfo card) {
            return visitCard(card);
        }

        default R visitBuilding(CardInfo card) {
            return visitCard(card);
        }

        default R visitEvent(CardInfo card) {
            return visitCard(card);
        }

        R visitCard(CardInfo card);
    }

    public abstract static class CardInfo {
        public final int id;
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

        protected CardInfo(int id, String era, String typeName, String detail,
                           int foodCost, int pointsEnd,
                           String buildingEffectName, String buildingEffectTarget, int buildingEffectPointsPerCard,
                           String eventTag, int eventFood, int eventPoints, int eventRequired, int eventPenalty) {
            this.id = id;
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

        public abstract <R> R accept(CardVisitor<R> visitor);
    }

    public static final class CharacterCardInfo extends CardInfo {
        CharacterCardInfo(int id, String era, String typeName, String detail) {
            super(id, era, typeName, detail, 0, 0, "", "", 0, "", 0, 0, 0, 0);
        }

        @Override
        public <R> R accept(CardVisitor<R> visitor) {
            return visitor.visitCharacter(this);
        }
    }

    public static final class BuildingCardInfo extends CardInfo {
        BuildingCardInfo(int id, String era, String typeName, String detail, int foodCost, int pointsEnd,
                         String effectName, String effectTarget, int pointsPerCard) {
            super(id, era, typeName, detail, foodCost, pointsEnd, effectName, effectTarget, pointsPerCard, "", 0, 0, 0, 0);
        }

        @Override
        public <R> R accept(CardVisitor<R> visitor) {
            return visitor.visitBuilding(this);
        }
    }

    public static final class EventCardInfo extends CardInfo {
        EventCardInfo(int id, String era, String typeName, String detail,
                      String eventTag, int eventFood, int eventPoints, int eventRequired, int eventPenalty) {
            super(id, era, typeName, detail, 0, 0, "", "", 0, eventTag, eventFood, eventPoints, eventRequired, eventPenalty);
        }

        @Override
        public <R> R accept(CardVisitor<R> visitor) {
            return visitor.visitEvent(this);
        }
    }

    public static final class UnknownCardInfo extends CardInfo {
        UnknownCardInfo(int id) {
            super(id, "?", "?", "", 0, 0, "", "", 0, "", 0, 0, 0, 0);
        }

        @Override
        public <R> R accept(CardVisitor<R> visitor) {
            return visitor.visitCard(this);
        }
    }

    private static final Map<Integer, CardInfo> CARDS = new HashMap<>();

    static {
        CardDataCatalog catalog = CardDataCatalog.loadDefault();
        loadCharacters(catalog.characters());
        loadBuildings(catalog.buildings());
        loadEvents(catalog.events());
    }

    public static CardInfo get(int id) {
        return CARDS.getOrDefault(id, new UnknownCardInfo(id));
    }

    public static String label(int id) {
        CardInfo c = get(id);
        return "#" + c.id + " " + c.era + " " + c.typeName
                + (c.detail.isEmpty() ? "" : " " + c.detail);
    }

    public static boolean isBuilding(int id) {
        return get(id).accept(new CardVisitor<Boolean>() {
            @Override
            public Boolean visitBuilding(CardInfo card) {
                return true;
            }

            @Override
            public Boolean visitCard(CardInfo card) {
                return false;
            }
        });
    }

    // ── loading ──────────────────────────────────────────────────────────────

    private static void loadCharacters(Iterable<JsonObject> cards) {
        for (JsonObject o : cards) {
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
            CARDS.put(id, new CharacterCardInfo(id, era, typeName, detail));
        }
    }

    private static void loadBuildings(Iterable<JsonObject> cards) {
        for (JsonObject o : cards) {
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
            CARDS.put(id, new BuildingCardInfo(id, era, "Bldg",
                    shortFx(fxName), cost, pp, fxName, targetType, pointsPerCard));
        }
    }

    private static void loadEvents(Iterable<JsonObject> cards) {
        for (JsonObject o : cards) {
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
            CARDS.put(id, new EventCardInfo(id, era, "Event", detail,
                    tag, eventFood, eventPoints, eventRequired, eventPenalty));
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
            case "EndFullSetPP"           -> "Set>PP";
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

    private CardCatalog() {}
}
