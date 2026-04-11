package it.polimi.ingsw.model.card;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.polimi.ingsw.model.card.effect.*;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.InventorIcons;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CardFactory {

    public static List<Card> createTribeDeck(int numPlayers){
        List<Card> deck = new ArrayList<>();
        deck.addAll(loadCharacterCards(numPlayers));
        deck.addAll(loadEventCards(numPlayers));

        return deck;
    }

    public static List<BuildingCard> createBuildingCards(Era era, int numPlayers) {
        List<BuildingCard> allCards = loadBuildingCards();
        List<BuildingCard> filteredDeck = new ArrayList<>();
       return new ArrayList<>();
    }

    private static List<CharacterCard> loadCharacterCards(int numPlayers) {
        //TODO character_cards.json
        return new ArrayList<>();
    }
    private static List<EventCard> loadEventCards(int numPlayers) {
        // TODO: event_cards.json
        return new ArrayList<>();
    }

    private static List<BuildingCard> loadBuildingCards() {
        // TODO: building_cards.json
        return new ArrayList<>();
    }

}
