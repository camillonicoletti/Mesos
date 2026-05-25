package it.polimi.ingsw.view.gui.asset;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class GuiAssetCatalog {
    private static final String CARDS_MANIFEST = "assets/manifest/cards.json";
    private static final String COMPONENTS_MANIFEST = "assets/manifest/components.json";

    private final Map<Integer, CardAsset> cards;
    private final Map<Integer, JsonObject> cardDetails;
    private final JsonObject components;

    private GuiAssetCatalog(Map<Integer, CardAsset> cards, Map<Integer, JsonObject> cardDetails, JsonObject components) {
        this.cards = Collections.unmodifiableMap(new LinkedHashMap<>(cards));
        this.cardDetails = Collections.unmodifiableMap(new LinkedHashMap<>(cardDetails));
        this.components = components.deepCopy();
    }

    public static GuiAssetCatalog loadDefault() {
        JsonObject cardsManifest = loadJson(CARDS_MANIFEST);
        JsonObject componentsManifest = loadJson(COMPONENTS_MANIFEST);

        Map<Integer, CardAsset> cards = new LinkedHashMap<>();
        JsonObject cardsObject = cardsManifest.getAsJsonObject("cards");
        for (Map.Entry<String, JsonElement> entry : cardsObject.entrySet()) {
            JsonObject value = entry.getValue().getAsJsonObject();
            int id = Integer.parseInt(entry.getKey());
            String frontPath = requiredString(value, "asset");
            String backPath = requiredString(value, "backAsset");
            cards.put(id, new CardAsset(
                    id,
                    requiredString(value, "kind"),
                    nullableString(value, "type"),
                    nullableString(value, "era"),
                    frontPath,
                    backPath,
                    resourceExists(frontPath),
                    resourceExists(backPath)
            ));
        }

        Map<Integer, JsonObject> cardDetails = new LinkedHashMap<>();
        cardDetails.putAll(loadDetailMap("characters.json"));
        cardDetails.putAll(loadDetailMap("events.json"));
        cardDetails.putAll(loadDetailMap("buildings.json"));

        return new GuiAssetCatalog(cards, cardDetails, componentsManifest);
    }

    public Optional<CardAsset> findCard(int cardId) {
        return Optional.ofNullable(cards.get(cardId));
    }

    public CardAsset getCard(int cardId) {
        CardAsset card = cards.get(cardId);
        if (card == null) {
            throw new IllegalArgumentException("Unknown card id: " + cardId);
        }
        return card;
    }

    public Map<Integer, CardAsset> getCards() {
        return cards;
    }

    public Optional<JsonObject> findCardDetails(int cardId) {
        JsonObject details = cardDetails.get(cardId);
        return details == null ? Optional.empty() : Optional.of(details.deepCopy());
    }

    public JsonObject getComponents() {
        return components.deepCopy();
    }

    public boolean hasCardImage(int cardId) {
        return findCard(cardId)
                .map(CardAsset::hasCompleteImages)
                .orElse(false);
    }

    public Optional<InputStream> openResource(String resourcePath) {
        return Optional.ofNullable(classLoader().getResourceAsStream(resourcePath));
    }

    public String placeholderLabelFor(CardAsset card) {
        StringBuilder label = new StringBuilder();
        label.append(card.getKind().toUpperCase()).append(" #").append(card.getId());
        card.getEra().ifPresent(era -> label.append(" ERA ").append(era));
        card.getType().ifPresent(type -> label.append(" ").append(type));
        return label.toString();
    }

    private static JsonObject loadJson(String resourcePath) {
        InputStream stream = classLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalStateException("Missing GUI asset manifest: " + resourcePath);
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read GUI asset manifest: " + resourcePath, exception);
        }
    }

    private static Map<Integer, JsonObject> loadDetailMap(String resourcePath) {
        InputStream stream = classLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            return Map.of();
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            Map<Integer, JsonObject> result = new LinkedHashMap<>();
            for (JsonElement element : array) {
                JsonObject object = element.getAsJsonObject();
                JsonElement id = object.get("id");
                if (id != null && !id.isJsonNull()) {
                    result.put(id.getAsInt(), object.deepCopy());
                }
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read card detail resource: " + resourcePath, exception);
        }
    }

    private static boolean resourceExists(String resourcePath) {
        try (InputStream stream = classLoader().getResourceAsStream(resourcePath)) {
            return stream != null;
        } catch (Exception exception) {
            return false;
        }
    }

    private static ClassLoader classLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    private static String requiredString(JsonObject object, String key) {
        String value = nullableString(object, key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing manifest field: " + key);
        }
        return value;
    }

    private static String nullableString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return element.getAsString();
    }
}
