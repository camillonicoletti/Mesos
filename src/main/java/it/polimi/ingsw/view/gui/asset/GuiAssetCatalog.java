package it.polimi.ingsw.view.gui.asset;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.polimi.ingsw.view.card.CardDataCatalog;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

// The catalog of all card assets, loaded once from a manifest file.
// Lets the GUI find a card by its id, get its details and reuse cached images.
public final class GuiAssetCatalog {
    private static final String CARDS_MANIFEST = "assets/manifest/cards.json";

    private final Map<Integer, CardAsset> cards;
    private final Map<Integer, JsonObject> cardDetails;
    /** Decoded images cached by resource path; JavaFX Image is immutable and shareable across ImageViews. */
    private final Map<String, Image> imageCache = new HashMap<>();

    private GuiAssetCatalog(Map<Integer, CardAsset> cards, Map<Integer, JsonObject> cardDetails) {
        this.cards = Collections.unmodifiableMap(new LinkedHashMap<>(cards));
        this.cardDetails = Collections.unmodifiableMap(new LinkedHashMap<>(cardDetails));
    }

    public static GuiAssetCatalog loadDefault() {
        JsonObject cardsManifest = loadJson(CARDS_MANIFEST);

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

        Map<Integer, JsonObject> cardDetails = CardDataCatalog.loadDefault().allDetails();

        return new GuiAssetCatalog(cards, cardDetails);
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

    public Optional<InputStream> openResource(String resourcePath) {
        return GuiAssetLoader.open(resourcePath);
    }

    /**
     * Returns the decoded image for the given resource path, loading it from
     * disk at most once and caching it thereafter. Returns empty if the
     * resource is missing or cannot be decoded. Must be called on the JavaFX
     * thread (the only thread that builds the views).
     */
    public Optional<Image> image(String resourcePath) {
        if (resourcePath == null) {
            return Optional.empty();
        }
        Image cached = imageCache.get(resourcePath);
        if (cached != null) {
            return Optional.of(cached);
        }
        try (InputStream stream = GuiAssetLoader.open(resourcePath).orElse(null)) {
            if (stream == null) return Optional.empty();
            Image image = new Image(stream);
            imageCache.put(resourcePath, image);
            return Optional.of(image);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public String placeholderLabelFor(CardAsset card) {
        StringBuilder label = new StringBuilder();
        label.append(card.getKind().toUpperCase()).append(" #").append(card.getId());
        card.getEra().ifPresent(era -> label.append(" ERA ").append(era));
        card.getType().ifPresent(type -> label.append(" ").append(type));
        return label.toString();
    }

    private static JsonObject loadJson(String resourcePath) {
        InputStream stream = GuiAssetLoader.open(resourcePath).orElse(null);
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
        try (InputStream stream = GuiAssetLoader.open(resourcePath).orElse(null)) {
            return stream != null;
        } catch (Exception exception) {
            return false;
        }
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
