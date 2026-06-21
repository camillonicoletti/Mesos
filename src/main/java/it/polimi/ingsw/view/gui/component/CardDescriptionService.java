package it.polimi.ingsw.view.gui.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.polimi.ingsw.view.card.BuildingEffect;
import it.polimi.ingsw.view.card.CharacterType;
import it.polimi.ingsw.view.card.EventType;
import it.polimi.ingsw.view.gui.asset.CardAsset;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// Builds the text shown to the player to describe a card (its rules and cost).
// It reads the card data from the asset catalog and turns it into easy lines.
final class CardDescriptionService {
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
        return CharacterType.from(stringDetail(data, "type")).ruleLines(data);
    }

    private List<String> buildingRuleLines(JsonObject data) {
        JsonElement effect = data.get("effect");
        return BuildingEffect.from(effectName(effect)).ruleLines(effect);
    }

    private String eventRuleText(JsonObject data) {
        String suffix = boolDetail(data, "closeEvent") ? " This is a closing event." : "";
        return EventType.from(stringDetail(data, "type")).ruleText(data, suffix);
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
