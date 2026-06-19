package it.polimi.ingsw.view.card;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Loads card data from the characters, buildings and events JSON files.
// It stores the data so the view can show the cards.
// It returns copies to avoid changing and reloading the original catalog.

public final class CardDataCatalog {
    private static final String CHARACTERS_RESOURCE = "characters.json";
    private static final String BUILDINGS_RESOURCE = "buildings.json";
    private static final String EVENTS_RESOURCE = "events.json";

    private final Map<Integer, JsonObject> characters;
    private final Map<Integer, JsonObject> buildings;
    private final Map<Integer, JsonObject> events;
    private final Map<Integer, JsonObject> allDetails;

    private CardDataCatalog(Map<Integer, JsonObject> characters,
                            Map<Integer, JsonObject> buildings,
                            Map<Integer, JsonObject> events) {
        this.characters = immutableDeepCopy(characters);
        this.buildings = immutableDeepCopy(buildings);
        this.events = immutableDeepCopy(events);

        Map<Integer, JsonObject> merged = new LinkedHashMap<>();
        merged.putAll(this.characters);
        merged.putAll(this.buildings);
        merged.putAll(this.events);
        this.allDetails = Collections.unmodifiableMap(merged);
    }

    public static CardDataCatalog loadDefault() {
        return new CardDataCatalog(
                loadResource(CHARACTERS_RESOURCE),
                loadResource(BUILDINGS_RESOURCE),
                loadResource(EVENTS_RESOURCE)
        );
    }

    public List<JsonObject> characters() {
        return deepCopyValues(characters);
    }

    public List<JsonObject> buildings() {
        return deepCopyValues(buildings);
    }

    public List<JsonObject> events() {
        return deepCopyValues(events);
    }

    public Map<Integer, JsonObject> allDetails() {
        return immutableDeepCopy(allDetails);
    }

    public Optional<JsonObject> findDetails(int cardId) {
        JsonObject details = allDetails.get(cardId);
        return details == null ? Optional.empty() : Optional.of(details.deepCopy());
    }

    //immutableDeepCopy creates a protected copy of the map, so the original catalog data cannot be changed.
    //deepCopyValues(Map<Integer, JsonObject> source)
    private static Map<Integer, JsonObject> loadResource(String resourcePath) {
        InputStream stream = CardDataCatalog.class.getClassLoader().getResourceAsStream(resourcePath);
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

    private static Map<Integer, JsonObject> immutableDeepCopy(Map<Integer, JsonObject> source) {
        Map<Integer, JsonObject> copy = new LinkedHashMap<>();
        for (Map.Entry<Integer, JsonObject> entry : source.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().deepCopy());
        }
        return Collections.unmodifiableMap(copy);
    }

    private static List<JsonObject> deepCopyValues(Map<Integer, JsonObject> source) {
        List<JsonObject> copy = new ArrayList<>();
        for (JsonObject value : source.values()) {
            copy.add(value.deepCopy());
        }
        return Collections.unmodifiableList(copy);
    }

}
