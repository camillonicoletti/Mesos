package it.polimi.ingsw.view.card;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
