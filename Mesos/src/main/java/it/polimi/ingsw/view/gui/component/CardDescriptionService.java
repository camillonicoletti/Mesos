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
            StringBuilder header = new StringBuilder(labelForKind(card.getKind()));
            card.getType().ifPresent(type -> header.append(" - ").append(labelForType(type)));
            card.getEra().ifPresent(era -> header.append(" | Era ").append(era));
            lines.add(header.toString());
        });

        if (asset.isPresent() && details.isPresent()) {
            CardAsset card = asset.get();
            JsonObject data = details.get();
            switch (card.getKind()) {
                case "character" -> lines.addAll(characterRuleLines(data));
                case "building" -> {
                    lines.add("Costo: " + intDetail(data, "foodCost") + " cibo. Punti a fine partita: "
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
            lines.add("Ultimo effetto risolto: " + lastResolvedEventDescription);
        }

        return lines;
    }

    String summaryLabelFor(int cardId) {
        Optional<CardAsset> asset = assetCatalog.findCard(cardId);
        if (asset.isEmpty()) {
            return "Carta";
        }
        CardAsset card = asset.get();
        if ("building".equals(card.getKind())) {
            return assetCatalog.findCardDetails(cardId)
                    .map(details -> details.get("effect"))
                    .map(this::formatDetailValue)
                    .map(this::labelForType)
                    .orElse("Edificio");
        }
        return card.getType().map(this::labelForType).orElse(labelForKind(card.getKind()));
    }

    private List<String> characterRuleLines(JsonObject data) {
        String type = stringDetail(data, "type");
        return switch (type) {
            case "hunter" -> boolDetail(data, "foodIcon")
                    ? List.of(
                    "Cacciatore: durante l'Evento Caccia conta per ottenere cibo e PP indicati dalla carta evento.",
                    "Quando lo prendi, ottieni subito 1 cibo per ogni Cacciatore nella tua tribu, incluso questo.")
                    : List.of(
                    "Cacciatore: durante l'Evento Caccia conta per ottenere cibo e PP indicati dalla carta evento.",
                    "Non mostra l'icona cibo, quindi quando lo prendi non da cibo immediato.");
            case "builder" -> List.of(
                    "Costruttore: riduce di " + intDetail(data, "foodDiscount")
                            + " il costo di ogni Edificio che prendi.",
                    "Gli sconti dei Costruttori si sommano, ma il costo non puo scendere sotto 0.",
                    "A fine partita vale " + intDetail(data, "pointsEndOfMatch") + " PP.");
            case "gatherer" -> List.of(
                    "Raccoglitore: durante l'Evento Sostentamento sconta "
                            + FOOD_DISCOUNT_PER_GATHERER + " cibo dal totale da pagare.",
                    "Non fornisce cibo direttamente quando entra nella tribu.");
            case "artist" -> List.of(
                    "Artista: durante l'Evento Pitture Rupestri determina se perdi PP o guadagni PP per artista.",
                    "A fine partita ottieni 10 PP per ogni coppia di Artisti nella tua tribu.");
            case "inventor" -> List.of(
                    "Inventore: porta l'icona invenzione " + stringDetail(data, "inventionIcon") + ".",
                    "A fine partita vale: numero di Inventori nella tribu x numero di icone invenzione diverse.");
            case "shaman" -> List.of(
                    "Sciamano: aggiunge " + intDetail(data, "stars") + " stelle alla tua tribu.",
                    "Durante il Rituale Sciamanico, piu stelle fanno guadagnare PP; meno stelle fanno perdere PP.");
            default -> List.of("Carta personaggio.");
        };
    }

    private List<String> buildingRuleLines(JsonObject data) {
        JsonElement effect = data.get("effect");
        String effectName = effectName(effect);
        return switch (effectName) {
            case "FullSetFood" -> List.of(
                    "Da quando possiedi questo Edificio, ogni volta che completi un nuovo set di 6 Personaggi tutti diversi ottieni 5 cibo.",
                    "I set gia completi prima di prendere l'Edificio non danno questo bonus.");
            case "SustenanceFoodDiscount" -> List.of(
                    "Durante l'Evento Sostentamento sconta 1 cibo dal totale da pagare per ogni "
                            + characterTypeSingular(effectTarget(effect)) + " nella tua tribu.");
            case "ShamanicRitualImmunity" -> List.of(
                    "Durante il Rituale Sciamanico non perdi PP se hai meno stelle degli altri giocatori.");
            case "TotemSlotFoodBonus" -> List.of(
                    "Alla fine del tuo turno, quando il totem rientra sulla Tessera Ordine di Turno, ottieni 1 cibo extra se lo spazio scelto da gia un bonus cibo.",
                    "Se finisci nell'ultimo spazio, paghi normalmente 1 cibo oppure perdi 2 PP se non puoi pagare.");
            case "InventorPairFood" -> List.of(
                    "Da quando possiedi questo Edificio, ottieni 3 cibo ogni volta che completi una nuova coppia di Inventori uguali.",
                    "Contano come uguali gli Inventori con la stessa icona invenzione; le coppie gia presenti non danno il bonus.");
            case "ShamanicRitualDoublePP" -> List.of(
                    "Durante il Rituale Sciamanico, se hai piu stelle di tutti gli altri, guadagni il doppio dei PP indicati dall'evento.",
                    "Se sei in parita per la maggioranza, guadagni comunque i PP indicati dall'evento.");
            case "Shaman3ExtraStars" -> List.of(
                    "Durante il Rituale Sciamanico la tua tribu conta 3 stelle aggiuntive.");
            case "Hunt1Food1PP" -> List.of(
                    "Durante l'Evento Caccia, per ogni Cacciatore nella tua tribu ottieni 1 cibo e 1 PP addizionali.");
            case "EndBuilderPP" -> List.of(
                    "A fine partita raddoppi i PP stampati sulle tue carte Costruttore.");
            case "CavePaintingsFood" -> List.of(
                    "Durante l'Evento Pitture Rupestri ottieni 1 cibo per ogni Artista nella tua tribu.");
            case "EndFullSetPP" -> List.of(
                    "A fine partita guadagni 6 PP per ogni set completo di 6 Personaggi tutti diversi nella tua tribu.");
            case "ExtraCardUpperRow" -> List.of(
                    "Dopo che tutti hanno risolto le azioni e prima della Fine del Round, puoi prendere 1 carta dalla fila superiore.",
                    "Se scegli un Edificio devi comunque pagarne il costo, applicando gli sconti dei Costruttori.");
            case "EndPP" -> List.of(
                    "A fine partita guadagni 25 PP grazie a questo effetto.");
            case "EndCharacterCountPP" -> List.of(
                    "A fine partita guadagni " + effectInt(effect, "pointsPerCard") + " PP per ogni "
                            + characterTypeSingular(effectTarget(effect)) + " nella tua tribu.");
            default -> List.of("Edificio: " + formatDetailValue(effect) + ".");
        };
    }

    private String eventRuleText(JsonObject data) {
        String suffix = boolDetail(data, "closeEvent") ? " Questo e un evento finale." : "";
        return switch (stringDetail(data, "type")) {
            case "hunt" -> "Caccia: ogni cacciatore fornisce " + intDetail(data, "foodPerHunter")
                    + " cibo e " + intDetail(data, "ppPerHunter") + " PP." + suffix;
            case "sustenance" -> "Sostentamento: ogni personaggio non nutrito causa -"
                    + intDetail(data, "ppPenaltyPerUnfed") + " PP." + suffix;
            case "shamanicRitual" -> "Rituale sciamanico: chi ha piu stelle guadagna "
                    + intDetail(data, "ppGain") + " PP, chi ne ha meno perde "
                    + intDetail(data, "ppLoss") + " PP." + suffix;
            case "cavePaintings" -> "Pitture rupestri: servono almeno " + intDetail(data, "minArtists")
                    + " artisti; se non li hai perdi " + intDetail(data, "ppPenalty")
                    + " PP, altrimenti ottieni " + intDetail(data, "ppPerArtist")
                    + " PP per artista." + suffix;
            default -> "Evento." + suffix;
        };
    }

    private List<String> fallbackDetailLines(int cardId) {
        List<String> lines = new ArrayList<>();
        Optional<CardAsset> asset = assetCatalog.findCard(cardId);
        lines.add("ID: " + cardId);
        asset.ifPresent(card -> {
            lines.add("Categoria: " + labelForKind(card.getKind()));
            card.getType().ifPresent(type -> lines.add("Tipo: " + labelForType(type)));
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
            case "character" -> "Personaggio";
            case "building" -> "Edificio";
            case "event" -> "Evento";
            default -> capitalize(kind);
        };
    }

    private String labelForType(String type) {
        return switch (type) {
            case "hunter" -> "Cacciatori";
            case "builder" -> "Costruttori";
            case "gatherer" -> "Raccoglitori";
            case "artist" -> "Artisti";
            case "inventor" -> "Inventori";
            case "shaman" -> "Sciamani";
            case "hunt" -> "Caccia";
            case "sustenance" -> "Sostentamento";
            case "shamanicRitual" -> "Rituale sciamanico";
            case "cavePaintings" -> "Pitture rupestri";
            case "FullSetFood" -> "Cibo da set completo";
            case "SustenanceFoodDiscount" -> "Sconto sostentamento";
            case "ShamanicRitualImmunity" -> "Immunita rituale sciamanico";
            case "TotemSlotFoodBonus" -> "Bonus cibo slot totem";
            case "InventorPairFood" -> "Cibo da coppie inventori";
            case "ShamanicRitualDoublePP" -> "Doppio rituale sciamanico";
            case "Shaman3ExtraStars" -> "Stelle sciamano extra";
            case "Hunt1Food1PP" -> "Caccia: cibo e PP";
            case "EndBuilderPP" -> "PP finali costruttori";
            case "CavePaintingsFood" -> "Cibo pitture rupestri";
            case "EndFullSetPP" -> "PP finali da set completo";
            case "ExtraCardUpperRow" -> "Carta extra fila alta";
            case "EndPP" -> "PP finali";
            case "EndCharacterCountPP" -> "PP finali per personaggio";
            default -> capitalize(type);
        };
    }

    private String characterTypeSingular(String type) {
        return switch (type) {
            case "hunter" -> "Cacciatore";
            case "builder" -> "Costruttore";
            case "gatherer" -> "Raccoglitore";
            case "artist" -> "Artista";
            case "inventor" -> "Inventore";
            case "shaman" -> "Sciamano";
            default -> labelForType(type);
        };
    }

    private String labelForDetailKey(String key) {
        return switch (key) {
            case "minPlayers" -> "Giocatori minimi";
            case "foodIcon" -> "Icona cibo";
            case "pointsEndOfMatch" -> "Punti fine partita";
            case "foodDiscount" -> "Sconto cibo";
            case "inventionIcon" -> "Icona invenzione";
            case "stars" -> "Stelle";
            case "foodCost" -> "Costo cibo";
            case "effect" -> "Effetto";
            case "closeEvent" -> "Evento finale";
            case "foodPerHunter" -> "Cibo per cacciatore";
            case "ppPerHunter" -> "PP per cacciatore";
            case "ppPenaltyPerUnfed" -> "Penalita PP per non nutrito";
            case "ppGain" -> "PP guadagnati";
            case "ppLoss" -> "PP persi";
            case "minArtists" -> "Artisti minimi";
            case "ppPenalty" -> "Penalita PP";
            case "ppPerArtist" -> "PP per artista";
            case "targetType" -> "Tipo bersaglio";
            case "pointsPerCard" -> "Punti per carta";
            case "name" -> "Nome";
            default -> capitalize(key);
        };
    }

    private String formatDetailValue(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return "-";
        }
        if (value.isJsonPrimitive()) {
            if (value.getAsJsonPrimitive().isBoolean()) {
                return value.getAsBoolean() ? "si" : "no";
            }
            if (value.getAsJsonPrimitive().isString()) {
                return labelForType(value.getAsString());
            }
            return value.getAsString();
        }
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            String name = object.has("name") ? labelForType(object.get("name").getAsString()) : "Effetto";
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
