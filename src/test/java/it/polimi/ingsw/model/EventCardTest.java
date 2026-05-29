package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.enums.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EventCard hierarchy (model.card).
 *
 * <p>Verifies: Hunt, Shamanic Ritual, Cave Paintings and Sustenance event card behaviour.
 */
public class EventCardTest {

    private Game buildGame(List<Player> players) {
        return new Game(players, 1, null, Era.I, null, null,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    // -------------------------------------------------------
    // HuntEventCard — base resolve (without building)
    // -------------------------------------------------------

    @Test
    void huntEventCard_givesCorrectFoodAndPPPerHunter() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addCharacterCard(new HunterCard(1, Era.I, 2, false));
        player.addCharacterCard(new HunterCard(2, Era.I, 2, false));

        Game game = buildGame(List.of(player));
        new HuntEventCard(1, Era.I, false, 2, 3).resolve(game);

        assertEquals(4, player.getFood());           // 2 hunters × 2 food
        assertEquals(6, player.getPrestigePoints()); // 2 hunters × 3 PP
    }

    @Test
    void huntEventCard_givesNothingWhenNoHunters() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addCharacterCard(new ArtistCard(1, Era.I, 2));

        Game game = buildGame(List.of(player));
        new HuntEventCard(1, Era.I, false, 2, 3).resolve(game);

        assertEquals(0, player.getFood());
        assertEquals(0, player.getPrestigePoints());
    }

    @Test
    void huntEventCard_appliesToAllPlayers() {
        Player p1 = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        Player p2 = new Player("Bob",   new Totem(TotemColor.BLUE, "OFFER_TRACK"));
        p1.addCharacterCard(new HunterCard(1, Era.I, 2, false));
        p2.addCharacterCard(new HunterCard(2, Era.I, 2, false));
        p2.addCharacterCard(new HunterCard(3, Era.I, 2, false));

        Game game = buildGame(List.of(p1, p2));
        new HuntEventCard(1, Era.I, false, 1, 2).resolve(game);

        assertEquals(1, p1.getFood());  assertEquals(2, p1.getPrestigePoints());
        assertEquals(2, p2.getFood());  assertEquals(4, p2.getPrestigePoints());
    }

    // -------------------------------------------------------
    // CavePaintingsEventCard — missing cases
    // -------------------------------------------------------

    @Test
    void cavePaintingsEventCard_exactlyAtMinimum_gainsPP() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new ArtistCard(2, Era.I, 2)); // exactly 2, minimum 2

        Game game = buildGame(List.of(player));
        new CavePaintingsEventCard(1, Era.I, false, 2, 3, 1).resolve(game);

        assertEquals(2, player.getPrestigePoints()); // 2 artisti × 1 PP
        assertEquals(0, player.getFood());
    }

    @Test
    void cavePaintingsEventCard_noArtists_losesPrestigePoints() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        // no artist

        Game game = buildGame(List.of(player));
        new CavePaintingsEventCard(1, Era.I, false, 2, 3, 1).resolve(game);

        assertEquals(-3, player.getPrestigePoints()); // below threshold → loses 3 PP
    }

    // -------------------------------------------------------
    // SustenanceEventCard — missing cases
    // -------------------------------------------------------

    @Test
    void sustenanceEventCard_gathererReducesFoodRequiredBy3() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addFood(10);
        // 2 gatherer + 2 artists = 4 characters → 4 food required
        // discount: 2 gatherer × 3 = 6 → food required = max(0, 4-6) = 0
        player.addCharacterCard(new GathererCard(1, Era.I, 2));
        player.addCharacterCard(new GathererCard(2, Era.I, 2));
        player.addCharacterCard(new ArtistCard(3, Era.I, 2));
        player.addCharacterCard(new ArtistCard(4, Era.I, 2));

        Game game = buildGame(List.of(player));
        new SustenanceEventCard(1, Era.I, false, 2).resolve(game);

        assertEquals(10, player.getFood());         // pays nothing
        assertEquals(0, player.getPrestigePoints());
    }

    @Test
    void sustenanceEventCard_exactFoodAvailable_noPPLost() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addFood(3);
        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new ArtistCard(2, Era.I, 2));
        player.addCharacterCard(new ArtistCard(3, Era.I, 2)); // 3 characters, 3 food

        Game game = buildGame(List.of(player));
        new SustenanceEventCard(1, Era.I, false, 2).resolve(game);

        assertEquals(0, player.getFood());
        assertEquals(0, player.getPrestigePoints());
    }

    @Test
    void sustenanceEventCard_moreFoodThanNeeded_noRefund() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addFood(10);
        player.addCharacterCard(new ArtistCard(1, Era.I, 2)); // 1 character, 1 food

        Game game = buildGame(List.of(player));
        new SustenanceEventCard(1, Era.I, false, 2).resolve(game);

        assertEquals(9, player.getFood()); // pays only 1, the rest remains
        assertEquals(0, player.getPrestigePoints());
    }

    // -------------------------------------------------------
    // ShamanicRitualEventCard — ties
    // -------------------------------------------------------

    @Test
    void shamanicRitualEventCard_tieForMaxStars_allWinnersGetPP() {
        Player p1 = new Player("Alice", new Totem(TotemColor.RED,    "OFFER_TRACK"));
        Player p2 = new Player("Bob",   new Totem(TotemColor.BLUE,   "OFFER_TRACK"));
        Player p3 = new Player("Carol", new Totem(TotemColor.PURPLE, "OFFER_TRACK"));

        p1.addCharacterCard(new ShamanCard(1, Era.I, 2, 2)); // 2 stars
        p2.addCharacterCard(new ShamanCard(2, Era.I, 2, 2)); // 2 stars (tied for the max)
        // p3 has 0 stars → loser

        Game game = buildGame(List.of(p1, p2, p3));
        new ShamanicRitualEventCard(1, Era.I, false, 4, 2).resolve(game);

        assertEquals(4,  p1.getPrestigePoints()); // winner
        assertEquals(4,  p2.getPrestigePoints()); // winner (tie)
        assertEquals(-2, p3.getPrestigePoints()); // loser
    }

    @Test
    void shamanicRitualEventCard_tieForMinStars_allLosersLosePP() {
        Player p1 = new Player("Alice", new Totem(TotemColor.RED,    "OFFER_TRACK"));
        Player p2 = new Player("Bob",   new Totem(TotemColor.BLUE,   "OFFER_TRACK"));
        Player p3 = new Player("Carol", new Totem(TotemColor.PURPLE, "OFFER_TRACK"));

        p1.addCharacterCard(new ShamanCard(1, Era.I, 2, 3)); // 3 stars → winner
        // p2 and p3 have 0 stars → tied for the min

        Game game = buildGame(List.of(p1, p2, p3));
        new ShamanicRitualEventCard(1, Era.I, false, 4, 2).resolve(game);

        assertEquals(4,  p1.getPrestigePoints()); // winner
        assertEquals(-2, p2.getPrestigePoints()); // loser
        assertEquals(-2, p3.getPrestigePoints()); // loser (tie)
    }

    @Test
    void shamanicRitualEventCard_allPlayersHaveSameStars_allGetPPAndLosePP() {
        // all with the same stars → everyone is both max and min
        Player p1 = new Player("Alice", new Totem(TotemColor.RED,  "OFFER_TRACK"));
        Player p2 = new Player("Bob",   new Totem(TotemColor.BLUE, "OFFER_TRACK"));

        p1.addCharacterCard(new ShamanCard(1, Era.I, 2, 2));
        p2.addCharacterCard(new ShamanCard(2, Era.I, 2, 2));

        Game game = buildGame(List.of(p1, p2));
        // they gain 4 PP (winners) and lose 2 PP (losers) → net +2
        new ShamanicRitualEventCard(1, Era.I, false, 4, 2).resolve(game);

        assertEquals(2, p1.getPrestigePoints());
        assertEquals(2, p2.getPrestigePoints());
    }
}
