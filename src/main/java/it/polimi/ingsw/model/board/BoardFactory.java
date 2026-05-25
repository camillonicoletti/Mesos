package it.polimi.ingsw.model.board;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class BoardFactory {

    public static Board createBoard(int numPlayers) {
        List<OfferTile> allTiles = loadOfferTiles();
        List<OfferTile> filtered = allTiles.stream()
                .filter(t -> t.isAvailableFor(numPlayers))
                .toList();
        TurnOrderTile turnOrder = TurnOrderTile.createForPlayers(numPlayers);
        return new Board(filtered, turnOrder);
    }

    private static List<OfferTile> loadOfferTiles() {
        Gson gson = new Gson();
        InputStream is = BoardFactory.class
                .getClassLoader()
                .getResourceAsStream("offer_tiles.json");
        JsonArray array = gson.fromJson(new InputStreamReader(is), JsonArray.class);

        List<OfferTile> tiles = new ArrayList<>();
        for (JsonElement elem : array) {
            JsonObject obj = elem.getAsJsonObject();
            char letter = obj.get("letter").getAsString().charAt(0);
            int minPlayers = obj.get("minPlayers").getAsInt();

            List<ActionSlot> slots = new ArrayList<>();
            for (JsonElement slotElem : obj.getAsJsonArray("slots")) {
                JsonObject slotObj = slotElem.getAsJsonObject();
                SlotEffect effect = switch (slotObj.get("type").getAsString()) {
                    case "FOOD"      -> new FoodSlot();
                    case "UPPER_ROW" -> new UpperRowSlot();
                    case "LOWER_ROW" -> new LowerRowSlot();
                    default -> throw new IllegalArgumentException("Unknown slot type: " + slotObj.get("type").getAsString());
                };
                int amount = slotObj.get("amount").getAsInt();
                slots.add(new ActionSlot(effect, amount));
            }

            tiles.add(new OfferTile(letter, minPlayers, slots));
        }
        return tiles;
    }
}