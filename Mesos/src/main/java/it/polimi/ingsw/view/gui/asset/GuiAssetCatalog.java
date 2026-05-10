package it.polimi.ingsw.view.gui.asset;

import com.google.gson.JsonElement;
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
    private static final String CARDS_MANIFEST = "gui/assets/manifest/cards.json";
    private static final String COMPONENTS_MANIFEST = "gui/assets/manifest/components.json";

    private final Map<Integer, CardAsset> cards;
    private final JsonObject components;

    private GuiAssetCatalog(Map<Integer, CardAsset> cards, JsonObject components) {
        this.cards = Collections.unmodifiableMap(new LinkedHashMap<>(cards));
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

        return new GuiAssetCatalog(cards, componentsManifest);
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
