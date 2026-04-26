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
        // 1. Carica e filtra i Personaggi per numPlayers, partiziona per Era
        List<CharacterCard> characters = loadCharacterCards();
        List<Card> eraI   = new ArrayList<>();
        List<Card> eraII  = new ArrayList<>();
        List<Card> eraIII = new ArrayList<>();
        for (CharacterCard c : characters) {
            if (!c.isAvailableFor(numPlayers)) continue;
            switch (c.getEra()) {
                case I   -> eraI.add(c);
                case II  -> eraII.add(c);
                case III -> eraIII.add(c);
            }
        }

        // 2. Carica gli Eventi, separa closeEvent (Eventi Finali) dagli altri per Era
        List<EventCard> events = loadEventCards();
        List<Card> closeEvents = new ArrayList<>();
        for (EventCard e : events) {
            if (e.isCloseEvent()) {
                closeEvents.add(e);
                continue;
            }
            switch (e.getEra()) {
                case I   -> eraI.add(e);
                case II  -> eraII.add(e);
                case III -> eraIII.add(e);
            }
        }

        // 3. Mescola ogni pila per Era, e i 2 Eventi Finali tra loro
        Collections.shuffle(eraI);
        Collections.shuffle(eraII);
        Collections.shuffle(eraIII);
        Collections.shuffle(closeEvents);

        // 4. Componi: TOP = Era I, poi II, III, BOTTOM = closeEvents
        List<Card> deck = new ArrayList<>();
        deck.addAll(eraI);
        deck.addAll(eraII);
        deck.addAll(eraIII);
        deck.addAll(closeEvents);
        return deck;
    }

    public static List<BuildingCard> createBuildingCards(Era era, int numPlayers) {
        // 1. Carica e filtra per Era richiesta
        List<BuildingCard> all = loadBuildingCards();
        List<BuildingCard> filtered = new ArrayList<>();
        for (BuildingCard b : all) {
            if (b.getEra() == era) filtered.add(b);
        }

        // 2. Mescola
        Collections.shuffle(filtered);

        // 3. Prendi le prime N secondo la tabella giocatori/Era
        int count = getBuildingCountForEra(era, numPlayers);
        if (count > filtered.size()) {
            throw new IllegalStateException(
                    "Not enough buildings for era " + era + ": need " + count + ", have " + filtered.size());
        }
        return new ArrayList<>(filtered.subList(0, count));
    }
    private static int getBuildingCountForEra(Era era, int numPlayers) {
        return switch (numPlayers) {
            case 2 -> switch (era) { case I -> 1; case II -> 2; case III -> 3; };
            case 3 -> switch (era) { case I -> 2; case II -> 2; case III -> 4; };
            case 4 -> switch (era) { case I -> 2; case II -> 3; case III -> 4; };
            case 5 -> switch (era) { case I -> 2; case II -> 3; case III -> 5; };
            default -> throw new IllegalStateException("Unsupported number of players: " + numPlayers);
        };
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
    private static List<BuildingCard> loadBuildingCards() {
        JsonArray array = readJsonArray("buildings.json");
        List<BuildingCard> result = new ArrayList<>();

        for (JsonElement el : array) {
            JsonObject obj = el.getAsJsonObject();

            int id = obj.get("id").getAsInt();
            Era era = parseEra(obj.get("era").getAsString());
            int foodCost  = obj.get("foodCost").getAsInt();
            int points = obj.get("pointsEndOfMatch").getAsInt();
            Effect effect = parseEffect(obj.get("effect"));

            result.add(new BuildingCard(id, era, foodCost, effect, points));
        }
        return result;
    }

    private static List<CharacterCard> loadCharacterCards() {
        JsonArray array = readJsonArray("characters.json");
        List<CharacterCard> result = new ArrayList<>();

        for (JsonElement el : array) {
            JsonObject obj = el.getAsJsonObject();

            int id         = obj.get("id").getAsInt();
            String type    = obj.get("type").getAsString();
            Era era        = parseEra(obj.get("era").getAsString());
            int minPlayers = obj.get("minPlayers").getAsInt();

            CharacterCard card = switch (type) {
                case "hunter"   -> new HunterCard(id, era, minPlayers,
                        obj.get("foodIcon").getAsBoolean());
                case "gatherer" -> new GathererCard(id, era, minPlayers);
                case "artist"   -> new ArtistCard(id, era, minPlayers);
                case "shaman"   -> new ShamanCard(id, era, minPlayers,
                        obj.get("stars").getAsInt());
                case "inventor" -> new InventorCard(id, era, minPlayers,
                        parseInventorIcon(obj.get("inventionIcon").getAsString()));
                case "builder"  -> new BuilderCard(id, era, minPlayers,
                        obj.get("pointsEndOfMatch").getAsInt(),
                        obj.get("foodDiscount").getAsInt());
                default -> throw new IllegalArgumentException("Unknown character type: " + type);
            };

            result.add(card);
        }
        return result;
    }
    private static List<EventCard> loadEventCards() {
        JsonArray array = readJsonArray("events.json");
        List<EventCard> result = new ArrayList<>();

        for (JsonElement el : array) {
            JsonObject obj = el.getAsJsonObject();

            int id = obj.get("id").getAsInt();
            String type  = obj.get("type").getAsString();
            Era era = parseEra(obj.get("era").getAsString());
            boolean closeEvent = obj.get("closeEvent").getAsBoolean();

            EventCard card = switch (type) {
                case "hunt" -> new HuntEventCard(id, era, closeEvent,
                        obj.get("foodPerHunter").getAsInt(),
                        obj.get("ppPerHunter").getAsInt());
                case "sustenance" -> new SustenanceEventCard(id, era, closeEvent,
                        obj.get("ppPenaltyPerUnfed").getAsInt());
                case "shamanicRitual" -> new ShamanicRitualEventCard(id, era, closeEvent,
                        obj.get("ppGain").getAsInt(),
                        obj.get("ppLoss").getAsInt());
                case "cavePaintings" -> new CavePaintingsEventCard(id, era, closeEvent,
                        obj.get("minArtists").getAsInt(),
                        obj.get("ppPenalty").getAsInt(),
                        obj.get("ppPerArtist").getAsInt());
                default -> throw new IllegalArgumentException("Unknown event type: " + type);
            };

            result.add(card);
        }
        return result;
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