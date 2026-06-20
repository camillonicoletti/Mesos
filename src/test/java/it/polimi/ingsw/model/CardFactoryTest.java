package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.enums.Era;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CardFactory (model.card).
 *
 * Verifies: deterministic construction of tribute and building decks per era.
 */
public class CardFactoryTest {

    // ---------- createBuildingCards ----------

    @Test
    void buildings_eraI_5Players_returns2Cards() {
        List<BuildingCard> cards = CardFactory.createBuildingCards(Era.I, 5);
        assertEquals(2, cards.size());
        cards.forEach(c -> assertEquals(Era.I, c.getEra()));
    }

    @Test
    void buildings_eraII_2Players_returns2Cards() {
        List<BuildingCard> cards = CardFactory.createBuildingCards(Era.II, 2);
        assertEquals(2, cards.size());
        cards.forEach(c -> assertEquals(Era.II, c.getEra()));
    }

    @Test
    void buildings_eraIII_5Players_returns5Cards() {
        List<BuildingCard> cards = CardFactory.createBuildingCards(Era.III, 5);
        assertEquals(5, cards.size());
        cards.forEach(c -> assertEquals(Era.III, c.getEra()));
    }

    @Test
    void buildings_allCardsHaveValidEffectAndCost() {
        for (Era era : Era.values()) {
            for (BuildingCard c : CardFactory.createBuildingCards(era, 5)) {
                assertNotNull(c.getEffect(), "Effect must not be null for id " + c.getId());
                assertTrue(c.getFoodCost() >= 0, "Food cost must be >= 0");
                assertTrue(c.getId() >= 97 && c.getId() <= 117, "Building id out of range: " + c.getId());
            }
        }
    }

    @Test
    void buildings_invalidPlayerCountThrows() {
        assertThrows(IllegalStateException.class, () -> CardFactory.createBuildingCards(Era.I, 1));
        assertThrows(IllegalStateException.class, () -> CardFactory.createBuildingCards(Era.I, 6));
    }

    // ---------- createTribeDeck ----------

    @Test
    void tribeDeck_lastTwoCardsAreCloseEvents() {
        List<Card> deck = CardFactory.createTribeDeck(4);
        int n = deck.size();
        assertTrue(n >= 2);
        Card last = deck.get(n - 1);
        Card secondLast = deck.get(n - 2);
        assertTrue(last instanceof EventCard ec && ec.isCloseEvent(),
                "Last card must be a closeEvent");
        assertTrue(secondLast instanceof EventCard ec && ec.isCloseEvent(),
                "Second-last card must be a closeEvent");
    }

    @Test
    void tribeDeck_allCharactersRespectMinPlayers() {
        int numPlayers = 3;
        for (Card c : CardFactory.createTribeDeck(numPlayers)) {
            if (c instanceof CharacterCard cc) {
                assertTrue(cc.isAvailableFor(numPlayers),
                        "Character " + cc.getId() + " has minPlayers > " + numPlayers);
            }
        }
    }

    @Test
    void tribeDeck_eraOrdering_IbeforeII_IIbeforeIII() {
        List<Card> deck = CardFactory.createTribeDeck(5);
        // ignore the closeEvents at the end of the deck
        int lastNonClose = deck.size() - 1;
        while (lastNonClose >= 0 && deck.get(lastNonClose) instanceof EventCard ec && ec.isCloseEvent()) {
            lastNonClose--;
        }
        Era seen = Era.I;
        for (int i = 0; i <= lastNonClose; i++) {
            Era e = deck.get(i).getEra();
            assertTrue(e.ordinal() >= seen.ordinal(),
                    "Era ordering violated at index " + i + ": found " + e + " after " + seen);
            seen = e;
        }
    }

    @Test
    void tribeDeck_doesNotContainNullCards() {
        List<Card> deck = CardFactory.createTribeDeck(5);
        deck.forEach(c -> assertNotNull(c));
    }

    @Test
    void tribeDeck_sizeIsConsistentBetweenCalls() {
        // numPlayers fixed → same size, contents may vary due to shuffle
        int s1 = CardFactory.createTribeDeck(4).size();
        int s2 = CardFactory.createTribeDeck(4).size();
        assertEquals(s1, s2);
    }

    @Test
    void tribeDeck_higherPlayerCountIncludesMoreOrEqualCards() {
        int s2 = CardFactory.createTribeDeck(2).size();
        int s5 = CardFactory.createTribeDeck(5).size();
        assertTrue(s5 >= s2, "More players should include >= cards (some require minPlayers >= 3/4/5)");
    }

    // ---------- effetti building specifici ----------

    @Test
    void buildings_eraIII_containsExpectedEffectTypes() {
        // Ensures parseEffect works with parameterized effects
        List<BuildingCard> all = CardFactory.createBuildingCards(Era.III, 5);
        boolean hasParameterized = all.stream().anyMatch(b ->
                b.getEffect().getClass().getSimpleName().contains("EndCharacterCountPP"));
        // A single draw could miss this effect by chance, so retry a few times
        // before concluding it is genuinely absent from the pool.
        for (int i = 0; i < 20 && !hasParameterized; i++) {
            hasParameterized = CardFactory.createBuildingCards(Era.III, 5).stream().anyMatch(b ->
                    b.getEffect().getClass().getSimpleName().contains("EndCharacterCountPP"));
        }
        assertTrue(hasParameterized, "Era III should contain at least one EndCharacterCountPP effect across runs");
    }
}