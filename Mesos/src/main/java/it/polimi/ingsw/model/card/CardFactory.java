package it.polimi.ingsw.model.card;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.polimi.ingsw.model.card.effect.*;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.InventorIcons;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CardFactory {


    public static List<Card> createTribeDeck(int numPlayers) {
        return new ArrayList<>();
    }

    public static List<BuildingCard> createBuildingCards(Era era, int numPlayers) {
        return new ArrayList<>();
    }

    // HELPERS

    private static Era parseEra(String s) {
        return Era.valueOf(s);
    }

    private static InventorIcons parseInventorIcon(String s) {
        return InventorIcons.valueOf("INVENTION_" + s);
    }

    private static Class<? extends CharacterCard> parseTargetType(String s) {
        return switch (s) {
            case "hunter"   -> HunterCard.class;
            case "builder"  -> BuilderCard.class;
            case "gatherer" -> GathererCard.class;
            case "artist"   -> ArtistCard.class;
            case "inventor" -> InventorCard.class;
            case "shaman"   -> ShamanCard.class;
            default -> throw new IllegalArgumentException("Unknown character type: " + s);
        };
    }

    private static Effect parseEffect(JsonElement el) {
        // Effect as a string
        if (el.isJsonPrimitive()) {
            String name = el.getAsString();
            return switch (name) {
                case "FullSetFood"            -> new FullSetFoodEffect();
                case "ShamanicRitualImmunity" -> new ShamanicRitualImmunityEffect();
                case "TotemSlotFoodBonus"     -> new TotemSlotFoodBonusEffect();
                case "InventorPairFood"       -> new InventorPairFoodEffect();
                case "ShamanicRitualDoublePP" -> new ShamanicRitualDoublePPEffect();
                case "Shaman3ExtraStars"      -> new Shaman3ExtraStarsEffect();
                case "Hunt1Food1PP"           -> new Hunt1Food1PPEffect();
                case "EndBuilderPP"           -> new EndBuilderPPEffect();
                case "CavePaintingsFood"      -> new CavePaintingsFoodEffect();
                case "ExtraCardUpperRow"      -> new ExtraCardUpperRowEffect();
                case "EndPP"                  -> new EndPPEffect();
                default -> throw new IllegalArgumentException("Unknown effect: " + name);
            };
        }

        // Effect as an object
        JsonObject obj = el.getAsJsonObject();
        String name = obj.get("name").getAsString();
        return switch (name) {
            case "SustenanceFoodDiscount" -> new SustenanceFoodDiscountEffect(
                    parseTargetType(obj.get("targetType").getAsString()));
            case "EndCharacterCountPP" -> new EndCharacterCountPPEffect(
                    obj.get("pointsPerCard").getAsInt(),
                    parseTargetType(obj.get("targetType").getAsString()));
            default -> throw new IllegalArgumentException("Unknown parameterized effect: " + name);
        };
    }

    // Read Json as JsonArray
    private static JsonArray readJsonArray(String resourceName) {
        InputStream in = CardFactory.class.getClassLoader().getResourceAsStream(resourceName);
        if (in == null) {
            throw new IllegalStateException("Resource not found: " + resourceName);
        }
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + resourceName, e);
        }
    }
}