package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.ArtistCard;
import it.polimi.ingsw.model.card.CavePaintingsEventCard;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression test pinning down the rulebook-verified behaviour of the three
 * physical Cave Paintings cards: the rulebook uses two adjacent thresholds,
 * "lose row" and "gain row", with no neutral band in between. These tests
 * freeze that contract so refactors of resolve or the event JSON don't drift.
 */
class CavePaintingsRulebookRegressionTest {

    private Game buildGame(List<Player> players) {
        return new Game(players, 1, null, Era.I, null, null,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    private Player playerWithArtists(int artists) {
        Player p = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        for (int i = 0; i < artists; i++) {
            p.addCharacterCard(new ArtistCard(1000 + i, Era.I, 2));
        }
        return p;
    }

    // ── Era I (id 88) — minArtists = 1, ppPenalty = 2, ppPerArtist = 1 ──────

    @Test
    void eraI_0Artists_lose2PP() {
        Player p = playerWithArtists(0);
        new CavePaintingsEventCard(88, Era.I, false, 1, 2, 1).resolve(buildGame(List.of(p)));
        assertEquals(-2, p.getPrestigePoints());
    }

    @Test
    void eraI_1Artist_gain1PP() {
        Player p = playerWithArtists(1);
        new CavePaintingsEventCard(88, Era.I, false, 1, 2, 1).resolve(buildGame(List.of(p)));
        assertEquals(1, p.getPrestigePoints());
    }

    @Test
    void eraI_3Artists_gain3PP() {
        Player p = playerWithArtists(3);
        new CavePaintingsEventCard(88, Era.I, false, 1, 2, 1).resolve(buildGame(List.of(p)));
        assertEquals(3, p.getPrestigePoints());
    }

    // ── Era II (id 92) — minArtists = 2, ppPenalty = 2, ppPerArtist = 2 ─────

    @Test
    void eraII_0Artists_lose2PP() {
        Player p = playerWithArtists(0);
        new CavePaintingsEventCard(92, Era.II, false, 2, 2, 2).resolve(buildGame(List.of(p)));
        assertEquals(-2, p.getPrestigePoints());
    }

    @Test
    void eraII_1Artist_lose2PP() {
        Player p = playerWithArtists(1);
        new CavePaintingsEventCard(92, Era.II, false, 2, 2, 2).resolve(buildGame(List.of(p)));
        assertEquals(-2, p.getPrestigePoints());
    }

    @Test
    void eraII_2Artists_gain4PP() {
        Player p = playerWithArtists(2);
        new CavePaintingsEventCard(92, Era.II, false, 2, 2, 2).resolve(buildGame(List.of(p)));
        assertEquals(4, p.getPrestigePoints());
    }

    @Test
    void eraII_3Artists_gain6PP() {
        Player p = playerWithArtists(3);
        new CavePaintingsEventCard(92, Era.II, false, 2, 2, 2).resolve(buildGame(List.of(p)));
        assertEquals(6, p.getPrestigePoints());
    }

    // ── Era III (id 94) — minArtists = 3, ppPenalty = 2, ppPerArtist = 3 ────

    @Test
    void eraIII_0Artists_lose2PP() {
        Player p = playerWithArtists(0);
        new CavePaintingsEventCard(94, Era.III, false, 3, 2, 3).resolve(buildGame(List.of(p)));
        assertEquals(-2, p.getPrestigePoints());
    }

    @Test
    void eraIII_2Artists_lose2PP() {
        Player p = playerWithArtists(2);
        new CavePaintingsEventCard(94, Era.III, false, 3, 2, 3).resolve(buildGame(List.of(p)));
        assertEquals(-2, p.getPrestigePoints());
    }

    @Test
    void eraIII_3Artists_gain9PP() {
        Player p = playerWithArtists(3);
        new CavePaintingsEventCard(94, Era.III, false, 3, 2, 3).resolve(buildGame(List.of(p)));
        assertEquals(9, p.getPrestigePoints());
    }

    @Test
    void eraIII_5Artists_gain15PP() {
        Player p = playerWithArtists(5);
        new CavePaintingsEventCard(94, Era.III, false, 3, 2, 3).resolve(buildGame(List.of(p)));
        assertEquals(15, p.getPrestigePoints());
    }
}
