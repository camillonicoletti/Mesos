package it.polimi.ingsw.view.gui.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.polimi.ingsw.view.gui.asset.CardAsset;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Centralizes user-facing card descriptions for the GUI.
 * Keeping this out of GameBoardView avoids mixing layout code with rule text.
 */
final class CardDescriptionService {
    private static final int FOOD_DISCOUNT_PER_GATHERER = 3;

    private final GuiAssetCatalog assetCatalog;

    CardDescriptionService(GuiAssetCatalog assetCatalog) {
        this.assetCatalog = assetCatalog;
    }

    List<String> describeCard(int cardId, int lastResolvedEventCardId, String lastResolvedEventDescription) {
        List<String> lines = new ArrayList<>();
        Optional<CardAsset> asset = assetCatalog.findCard(cardId);
        Optional<JsonObject> details = assetCatalog.findCardDetails(cardId);

        asset.ifPresent(card -> {
            lines.add(detailTitleFor(card, details.orElse(null)));
        });

        if (asset.isPresent() && details.isPresent()) {
            CardAsset card = asset.get();
            JsonObject data = details.get();
            switch (card.getKind()) {
                case "character" -> lines.addAll(characterRuleLines(data));
                case "building" -> {
                    lines.add("Cost: " + intDetail(data, "foodCost") + " food. End-game points: "
                            + intDetail(data, "pointsEndOfMatch") + ".");
                    lines.addAll(buildingRuleLines(data));
                }
                case "event" -> lines.add(eventRuleText(data));
                default -> lines.addAll(fallbackDetailLines(cardId));
            }
        } else {
            lines.addAll(fallbackDetailLines(cardId));
        }

        if (cardId == lastResolvedEventCardId && lastResolvedEventDescription != null) {
            lines.add("Last resolved effect: " + lastResolvedEventDescription);
        }

        return lines;
    }

    String summaryLabelFor(int cardId) {
        Optional<CardAsset> asset = assetCatalog.findCard(cardId);
        if (asset.isEmpty()) {
            return "Card";
        }
        CardAsset card = asset.get();
        if ("building".equals(card.getKind())) {
            return assetCatalog.findCardDetails(cardId)
                    .map(details -> details.get("effect"))
                    .map(this::formatDetailValue)
                    .map(this::labelForType)
                    .orElse("Building");
        }
        return card.getType().map(this::labelForType).orElse(labelForKind(card.getKind()));
    }

    private List<String> characterRuleLines(JsonObject data) {
        String type = stringDetail(data, "type");
        return switch (type) {
            case "hunter" -> boolDetail(data, "foodIcon")
                    ? List.of(
                    "Hunter: counts during the Hunt Event to earn food and PP as shown on the event card.",
                    "When taken, immediately gain 1 food for each Hunter in your tribe, including this one.")
                    : List.of(
                    "Hunter: counts during the Hunt Event to earn food and PP as shown on the event card.",
                    "Has no food icon, so taking it does not grant immediate food.");
            case "builder" -> List.of(
                    "Builder: reduces the cost of each Building you take by " + intDetail(data, "foodDiscount") + ".",
                    "Builder discounts stack, but the cost cannot drop below 0.",
                    "Worth " + intDetail(data, "pointsEndOfMatch") + " PP at end of game.");
            case "gatherer" -> List.of(
                    "Gatherer: during the Sustenance Event, reduces the total food you pay by "
                            + FOOD_DISCOUNT_PER_GATHERER + ".",
                    "Does not provide food when added to your tribe.");
            case "artist" -> List.of(
                    "Artist: during the Cave Paintings Event, determines whether you lose or gain PP per artist.",
                    "At end of game, gain 10 PP for each pair of Artists in your tribe.");
            case "inventor" -> List.of(
                    "Inventor: carries invention icon " + stringDetail(data, "inventionIcon") + ".",
                    "End-game score: number of Inventors × number of distinct invention icons.");
            case "shaman" -> List.of(
                    "Shaman: adds " + intDetail(data, "stars") + " stars to your tribe.",
                    "During the Shamanic Ritual, more stars earn PP; fewer stars lose PP.");
            default -> List.of("Character card.");
        };
    }

    private List<String> buildingRuleLines(JsonObject data) {
        JsonElement effect = data.get("effect");
        String effectName = effectName(effect);
        return switch (effectName) {
            case "FullSetFood" -> List.of(
                    "While you own this Building, gain 5 food each time you complete a new set of all 6 different Character types.",
                    "Sets completed before taking this Building do not grant the bonus.");
            case "SustenanceFoodDiscount" -> List.of(
                    "During the Sustenance Event, reduce total food paid by 1 for each "
                            + characterTypeSingular(effectTarget(effect)) + " in your tribe.");
            case "ShamanicRitualImmunity" -> List.of(
                    "During the Shamanic Ritual, you do not lose PP even if you have fewer stars than others.");
            case "TotemSlotFoodBonus" -> List.of(
                    "When your totem returns to the Turn Order Tile, gain 1 extra food if the chosen slot already has a food bonus.",
                    "If you end up in the last slot, pay 1 food normally or lose 2 PP if you cannot.");
            case "InventorPairFood" -> List.of(
                    "While you own this Building, gain 3 food each time you complete a new pair of matching Inventors.",
                    "Inventors with the same invention icon count as matching; pairs already present before taking this Building do not grant the bonus.");
            case "ShamanicRitualDoublePP" -> List.of(
                    "During the Shamanic Ritual, if you have more stars than all others, gain double the PP shown on the event card.",
                    "If tied for the lead, you still gain the PP shown on the event card.");
            case "Shaman3ExtraStars" -> List.of(
                    "During the Shamanic Ritual, your tribe counts 3 additional stars.");
            case "Hunt1Food1PP" -> List.of(
                    "During the Hunt Event, gain 1 extra food and 1 extra PP for each Hunter in your tribe.");
            case "EndBuilderPP" -> List.of(
                    "At end of game, double the printed PP on all your Builder cards.");
            case "CavePaintingsFood" -> List.of(
                    "During the Cave Paintings Event, gain 1 food for each Artist in your tribe.");
            case "EndFullSetPP" -> List.of(
                    "At end of game, gain 6 PP for each complete set of all 6 different Character types in your tribe.");
            case "ExtraCardUpperRow" -> List.of(
                    "After all players resolve actions and before End of Round, take 1 card from the upper row.",
                    "If you choose a Building, you still pay its cost, applying any Builder discounts.");
            case "EndPP" -> List.of(
                    "Gain 25 PP at end of game from this effect.");
            case "EndCharacterCountPP" -> List.of(
                    "At end of game, gain " + effectInt(effect, "pointsPerCard") + " PP for each "
                            + characterTypeSingular(effectTarget(effect)) + " in your tribe.");
            default -> List.of("Building: " + formatDetailValue(effect) + ".");
        };
    }

    private String eventRuleText(JsonObject data) {
        String suffix = boolDetail(data, "closeEvent") ? " This is a closing event." : "";
        return switch (stringDetail(data, "type")) {
            case "hunt" -> "Hunt: each Hunter provides " + intDetail(data, "foodPerHunter")
                    + " food and " + intDetail(data, "ppPerHunter") + " PP." + suffix;
            case "sustenance" -> "Sustenance: each unfed character causes -"
                    + intDetail(data, "ppPenaltyPerUnfed") + " PP." + suffix;
            case "shamanicRitual" -> "Shamanic Ritual: whoever has the most stars gains "
                    + intDetail(data, "ppGain") + " PP; whoever has the fewest loses "
                    + intDetail(data, "ppLoss") + " PP." + suffix;
            case "cavePaintings" -> "Cave Paintings: you need at least " + intDetail(data, "minArtists")
                    + " artists; if you don't have them lose " + intDetail(data, "ppPenalty")
                    + " PP, otherwise gain " + intDetail(data, "ppPerArtist")
                    + " PP per artist." + suffix;
            default -> "Event." + suffix;
        };
    }

    private List<String> fallbackDetailLines(int cardId) {
        List<String> lines = new ArrayList<>();
        Optional<CardAsset> asset = assetCatalog.findCard(cardId);
        lines.add("ID: " + cardId);
        asset.ifPresent(card -> {
            lines.add("Kind: " + labelForKind(card.getKind()));
            card.getType().ifPresent(type -> lines.add("Type: " + labelForType(type)));
            card.getEra().ifPresent(era -> lines.add("Era: " + era));
        });

        assetCatalog.findCardDetails(cardId).ifPresent(details -> {
            List<String> extra = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : details.entrySet()) {
                String key = entry.getKey();
                if ("id".equals(key) || "type".equals(key) || "era".equals(key)) {
                    continue;
                }
                extra.add(labelForDetailKey(key) + ": " + formatDetailValue(entry.getValue()));
            }
            if (!extra.isEmpty()) {
                lines.addAll(extra);
            }
        });

        return lines;
    }

    private String detailTitleFor(CardAsset card, JsonObject details) {
        String title = switch (card.getKind()) {
            case "character" -> card.getType()
                    .map(this::labelForType)
                    .orElse("Character");
            case "event" -> "Event - " + card.getType()
                    .map(this::labelForType)
                    .orElse("Event");
            case "building" -> Optional.ofNullable(details)
                    .map(data -> data.get("effect"))
                    .map(this::effectName)
                    .map(this::labelForType)
                    .orElse("Building");
            default -> labelForKind(card.getKind());
        };
        if ("character".equals(card.getKind())
                || "event".equals(card.getKind())
                || "building".equals(card.getKind())) {
            return title;
        }

        String era = details != null ? stringDetail(details, "era") : "";
        if (!era.isEmpty()) {
            return title + " | Era " + era;
        }
        return card.getEra()
                .map(value -> title + " | Era " + value)
                .orElse(title);
    }

    private String stringDetail(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return "";
        }
        return value.getAsString();
    }

    private int intDetail(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return 0;
        }
        return value.getAsInt();
    }

    private boolean boolDetail(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && !value.isJsonNull() && value.getAsBoolean();
    }

    private String effectName(JsonElement effect) {
        if (effect == null || effect.isJsonNull()) {
            return "";
        }
        if (effect.isJsonObject()) {
            JsonElement name = effect.getAsJsonObject().get("name");
            return name == null || name.isJsonNull() ? "" : name.getAsString();
        }
        return effect.getAsString();
    }

    private String effectTarget(JsonElement effect) {
        if (effect == null || !effect.isJsonObject()) {
            return "";
        }
        JsonElement target = effect.getAsJsonObject().get("targetType");
        return target == null || target.isJsonNull() ? "" : target.getAsString();
    }

    private int effectInt(JsonElement effect, String key) {
        if (effect == null || !effect.isJsonObject()) {
            return 0;
        }
        JsonElement value = effect.getAsJsonObject().get(key);
        return value == null || value.isJsonNull() ? 0 : value.getAsInt();
    }

    private String labelForKind(String kind) {
        return switch (kind) {
            case "character" -> "Character";
            case "building" -> "Building";
            case "event" -> "Event";
            default -> capitalize(kind);
        };
    }

    private String labelForType(String type) {
        return switch (type) {
            case "hunter" -> "Hunters";
            case "builder" -> "Builders";
            case "gatherer" -> "Gatherers";
            case "artist" -> "Artists";
            case "inventor" -> "Inventors";
            case "shaman" -> "Shamans";
            case "hunt" -> "Hunt";
            case "sustenance" -> "Sustenance";
            case "shamanicRitual" -> "Shamanic Ritual";
            case "cavePaintings" -> "Cave Paintings";
            case "FullSetFood" -> "Food from full set";
            case "SustenanceFoodDiscount" -> "Sustenance discount";
            case "ShamanicRitualImmunity" -> "Shamanic ritual immunity";
            case "TotemSlotFoodBonus" -> "Totem slot food bonus";
            case "InventorPairFood" -> "Food from inventor pairs";
            case "ShamanicRitualDoublePP" -> "Double shamanic ritual";
            case "Shaman3ExtraStars" -> "Extra shaman stars";
            case "Hunt1Food1PP" -> "Hunt: food and PP";
            case "EndBuilderPP" -> "End PP builders";
            case "CavePaintingsFood" -> "Cave paintings food";
            case "EndFullSetPP" -> "End PP full set";
            case "ExtraCardUpperRow" -> "Extra card upper row";
            case "EndPP" -> "End PP";
            case "EndCharacterCountPP" -> "End PP per character";
            default -> capitalize(type);
        };
    }

    private String characterTypeSingular(String type) {
        return switch (type) {
            case "hunter" -> "Hunter";
            case "builder" -> "Builder";
            case "gatherer" -> "Gatherer";
            case "artist" -> "Artist";
            case "inventor" -> "Inventor";
            case "shaman" -> "Shaman";
            default -> labelForType(type);
        };
    }

    private String labelForDetailKey(String key) {
        return switch (key) {
            case "minPlayers" -> "Min. players";
            case "foodIcon" -> "Food icon";
            case "pointsEndOfMatch" -> "End-game points";
            case "foodDiscount" -> "Food discount";
            case "inventionIcon" -> "Invention icon";
            case "stars" -> "Stars";
            case "foodCost" -> "Food cost";
            case "effect" -> "Effect";
            case "closeEvent" -> "Closing event";
            case "foodPerHunter" -> "Food per hunter";
            case "ppPerHunter" -> "PP per hunter";
            case "ppPenaltyPerUnfed" -> "PP penalty per unfed";
            case "ppGain" -> "PP gained";
            case "ppLoss" -> "PP lost";
            case "minArtists" -> "Min. artists";
            case "ppPenalty" -> "PP penalty";
            case "ppPerArtist" -> "PP per artist";
            case "targetType" -> "Target type";
            case "pointsPerCard" -> "Points per card";
            case "name" -> "Name";
            default -> capitalize(key);
        };
    }

    private String formatDetailValue(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return "-";
        }
        if (value.isJsonPrimitive()) {
            if (value.getAsJsonPrimitive().isBoolean()) {
                return value.getAsBoolean() ? "yes" : "no";
            }
            if (value.getAsJsonPrimitive().isString()) {
                return labelForType(value.getAsString());
            }
            return value.getAsString();
        }
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            String name = object.has("name") ? labelForType(object.get("name").getAsString()) : "Effect";
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if ("name".equals(entry.getKey())) {
                    continue;
                }
                parts.add(labelForDetailKey(entry.getKey()) + " " + formatDetailValue(entry.getValue()));
            }
            return parts.isEmpty() ? name : name + " (" + String.join(", ", parts) + ")";
        }
        return value.toString();
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }
}
