package it.polimi.ingsw.view.card;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

// Reads simple values from a JSON object safely.
// If a value is missing, it returns a default value.
// It avoids repeating the same JSON checks in card classes.

// Helper used by CharacterType, EventType and BuildingEffect to read card JSON values safely.
final class CardJson {
    private CardJson() {
    }

    static String string(JsonObject object, String key) {
        JsonElement value = object == null ? null : object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    static int integer(JsonObject object, String key) {
        JsonElement value = object == null ? null : object.get(key);
        return value == null || value.isJsonNull() ? 0 : value.getAsInt();
    }

    static boolean bool(JsonObject object, String key) {
        JsonElement value = object == null ? null : object.get(key);
        return value != null && !value.isJsonNull() && value.getAsBoolean();
    }
}
