package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.enums.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EventCardTest {

    private Game buildGame(List<Player> players) {
        return new Game(players, 1, null, Era.I, null, null,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    // -------------------------------------------------------
    // HuntEventCard — base resolve (senza edificio)
    // -------------------------------------------------------

    @Test
    void huntEventCard_givesCorrectFoodAndPPPerHunter() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addCharacterCard(new HunterCard(1, Era.I, 2, false));
        player.addCharacterCard(new HunterCard(2, Era.I, 2, false));

        Game game = buildGame(List.of(player));
        new HuntEventCard(1, Era.I, false, 2, 3).resolve(game);

        assertEquals(4, player.getFood());           // 2 cacciatori × 2 cibo
        assertEquals(6, player.getPrestigePoints()); // 2 cacciatori × 3 PP
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
    // CavePaintingsEventCard — casi mancanti
    // -------------------------------------------------------

    @Test
    void cavePaintingsEventCard_exactlyAtMinimum_gainsPP() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new ArtistCard(2, Era.I, 2)); // esattamente 2, minimo 2

        Game game = buildGame(List.of(player));
        new CavePaintingsEventCard(1, Era.I, false, 2, 3, 1).resolve(game);

        assertEquals(2, player.getPrestigePoints()); // 2 artisti × 1 PP
        assertEquals(0, player.getFood());
    }

    @Test
    void cavePaintingsEventCard_noArtists_losesPrestigePoints() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        // nessun artista

        Game game = buildGame(List.of(player));
        new CavePaintingsEventCard(1, Era.I, false, 2, 3, 1).resolve(game);

        assertEquals(-3, player.getPrestigePoints()); // sotto soglia → perde 3 PP
    }

    // -------------------------------------------------------
    // SustenanceEventCard — casi mancanti
    // -------------------------------------------------------

    @Test
    void sustenanceEventCard_gathererReducesFoodRequiredBy3() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addFood(10);
        // 2 gatherer + 2 artisti = 4 personaggi → 4 cibo richiesto
        // sconto: 2 gatherer × 3 = 6 → cibo richiesto = max(0, 4-6) = 0
        player.addCharacterCard(new GathererCard(1, Era.I, 2));
        player.addCharacterCard(new GathererCard(2, Era.I, 2));
        player.addCharacterCard(new ArtistCard(3, Era.I, 2));
        player.addCharacterCard(new ArtistCard(4, Era.I, 2));

        Game game = buildGame(List.of(player));
        new SustenanceEventCard(1, Era.I, false, 2).resolve(game);

        assertEquals(10, player.getFood());         // non paga nulla
        assertEquals(0, player.getPrestigePoints());
    }

    @Test
    void sustenanceEventCard_exactFoodAvailable_noPPLost() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addFood(3);
        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new ArtistCard(2, Era.I, 2));
        player.addCharacterCard(new ArtistCard(3, Era.I, 2)); // 3 personaggi, 3 cibo

        Game game = buildGame(List.of(player));
        new SustenanceEventCard(1, Era.I, false, 2).resolve(game);

        assertEquals(0, player.getFood());
        assertEquals(0, player.getPrestigePoints());
    }

    @Test
    void sustenanceEventCard_moreFoodThanNeeded_noRefund() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addFood(10);
        player.addCharacterCard(new ArtistCard(1, Era.I, 2)); // 1 personaggio, 1 cibo

        Game game = buildGame(List.of(player));
        new SustenanceEventCard(1, Era.I, false, 2).resolve(game);

        assertEquals(9, player.getFood()); // paga solo 1, il resto rimane
        assertEquals(0, player.getPrestigePoints());
    }

    // -------------------------------------------------------
    // ShamanicRitualEventCard — parità
    // -------------------------------------------------------

    @Test
    void shamanicRitualEventCard_tieForMaxStars_allWinnersGetPP() {
        Player p1 = new Player("Alice", new Totem(TotemColor.RED,    "OFFER_TRACK"));
        Player p2 = new Player("Bob",   new Totem(TotemColor.BLUE,   "OFFER_TRACK"));
        Player p3 = new Player("Carol", new Totem(TotemColor.PURPLE, "OFFER_TRACK"));

        p1.addCharacterCard(new ShamanCard(1, Era.I, 2, 2)); // 2 stelle
        p2.addCharacterCard(new ShamanCard(2, Era.I, 2, 2)); // 2 stelle (parità per il max)
        // p3 ha 0 stelle → perdente

        Game game = buildGame(List.of(p1, p2, p3));
        new ShamanicRitualEventCard(1, Era.I, false, 4, 2).resolve(game);

        assertEquals(4,  p1.getPrestigePoints()); // vincitore
        assertEquals(4,  p2.getPrestigePoints()); // vincitore (parità)
        assertEquals(-2, p3.getPrestigePoints()); // perdente
    }

    @Test
    void shamanicRitualEventCard_tieForMinStars_allLosersLosePP() {
        Player p1 = new Player("Alice", new Totem(TotemColor.RED,    "OFFER_TRACK"));
        Player p2 = new Player("Bob",   new Totem(TotemColor.BLUE,   "OFFER_TRACK"));
        Player p3 = new Player("Carol", new Totem(TotemColor.PURPLE, "OFFER_TRACK"));

        p1.addCharacterCard(new ShamanCard(1, Era.I, 2, 3)); // 3 stelle → vincitore
        // p2 e p3 hanno 0 stelle → parità per il min

        Game game = buildGame(List.of(p1, p2, p3));
        new ShamanicRitualEventCard(1, Era.I, false, 4, 2).resolve(game);

        assertEquals(4,  p1.getPrestigePoints()); // vincitore
        assertEquals(-2, p2.getPrestigePoints()); // perdente
        assertEquals(-2, p3.getPrestigePoints()); // perdente (parità)
    }

    @Test
    void shamanicRitualEventCard_allPlayersHaveSameStars_allGetPPAndLosePP() {
        // tutti con stesse stelle → tutti sono sia massimo che minimo
        Player p1 = new Player("Alice", new Totem(TotemColor.RED,  "OFFER_TRACK"));
        Player p2 = new Player("Bob",   new Totem(TotemColor.BLUE, "OFFER_TRACK"));

        p1.addCharacterCard(new ShamanCard(1, Era.I, 2, 2));
        p2.addCharacterCard(new ShamanCard(2, Era.I, 2, 2));

        Game game = buildGame(List.of(p1, p2));
        // guadagnano 4 PP (vincitori) e perdono 2 PP (perdenti) → netto +2
        new ShamanicRitualEventCard(1, Era.I, false, 4, 2).resolve(game);

        assertEquals(2, p1.getPrestigePoints());
        assertEquals(2, p2.getPrestigePoints());
    }
}
