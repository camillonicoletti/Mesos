package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.ArtistCard;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CavePaintingsEventCard;
import it.polimi.ingsw.model.card.effect.CavePaintingsFoodEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Threshold contract test for CavePaintingsEventCard: below minArtists the
 * player loses ppPenalty PP, at or above it the player gains artists times
 * ppPerArtist PP. Uses synthetic parameters (minArtists = 3, ppPenalty = 2,
 * ppPerArtist = 4) to cover both sides of the boundary, and confirms that
 * CavePaintingsFoodEffect (building id 108) grants 1 food per artist
 * regardless of which side of the threshold the player falls on.
 */
class CavePaintingsThresholdTest {

    private static final int MIN_ARTISTS  = 3;
    private static final int PP_PENALTY   = 2;
    private static final int PP_PER_ART   = 4;

    private Game buildGame(List<Player> players) {
        return new Game(players, 1, null, Era.I, null, null,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    private Player makePlayer(String nick, TotemColor color, int artists) {
        Player p = new Player(nick, new Totem(color, "OFFER_TRACK"));
        for (int i = 0; i < artists; i++) {
            p.addCharacterCard(new ArtistCard(1000 + i, Era.I, 2));
        }
        return p;
    }

    private void resolveOn(Player player) {
        Game game = buildGame(List.of(player));
        new CavePaintingsEventCard(88, Era.I, false, MIN_ARTISTS, PP_PENALTY, PP_PER_ART).resolve(game);
    }

    // ── single threshold: below the threshold ⇒ flat penalty ──────────────────

    @Test
    void zeroArtists_losesFlatPenalty() {
        Player p = makePlayer("Alice", TotemColor.RED, 0);
        resolveOn(p);
        assertEquals(-PP_PENALTY, p.getPrestigePoints());
    }

    @Test
    void oneArtistBelowThreshold_losesFlatPenalty() {
        Player p = makePlayer("Alice", TotemColor.RED, 1);
        resolveOn(p);
        assertEquals(-PP_PENALTY, p.getPrestigePoints());
    }

    @Test
    void twoArtistsStillBelowThreshold_losesFlatPenalty() {
        Player p = makePlayer("Alice", TotemColor.RED, 2);
        resolveOn(p);
        // 2 < minArtists (=3) ⇒ flat penalty.
        assertEquals(-PP_PENALTY, p.getPrestigePoints());
    }

    // ── single threshold: at or above ⇒ gain N * ppPerArtist ──────────────────

    @Test
    void artistsExactlyAtThreshold_gainsPP() {
        Player p = makePlayer("Alice", TotemColor.RED, MIN_ARTISTS);
        resolveOn(p);
        assertEquals(MIN_ARTISTS * PP_PER_ART, p.getPrestigePoints());
    }

    @Test
    void artistsAboveThreshold_gainsPPProportional() {
        Player p = makePlayer("Alice", TotemColor.RED, MIN_ARTISTS + 2);
        resolveOn(p);
        assertEquals((MIN_ARTISTS + 2) * PP_PER_ART, p.getPrestigePoints());
    }

    // ── multi-player: each is evaluated independently ─────────────────────────

    @Test
    void multiplayerEvaluatedIndependently() {
        Player a = makePlayer("Alice", TotemColor.RED,  0);            // loses
        Player b = makePlayer("Bob",   TotemColor.BLUE, MIN_ARTISTS);  // gains
        Player c = makePlayer("Carol", TotemColor.PURPLE, 1);          // loses

        Game game = buildGame(List.of(a, b, c));
        new CavePaintingsEventCard(88, Era.I, false, MIN_ARTISTS, PP_PENALTY, PP_PER_ART).resolve(game);

        assertEquals(-PP_PENALTY, a.getPrestigePoints());
        assertEquals(MIN_ARTISTS * PP_PER_ART, b.getPrestigePoints());
        assertEquals(-PP_PENALTY, c.getPrestigePoints());
    }

    // ── building #10 (CavePaintingsFoodEffect) always gives 1 food per artist ─

    @Test
    void cavePaintingsFoodEffect_appliesEvenWhenPlayerIsBelowThreshold() {
        Player p = makePlayer("Alice", TotemColor.RED, 2); // below threshold ⇒ loses PP
        p.addBuildingCard(new BuildingCard(900, Era.I, 0, new CavePaintingsFoodEffect(), 0));

        resolveOn(p);

        // Player loses PP but still receives 1 food per artist from building #10.
        assertEquals(-PP_PENALTY, p.getPrestigePoints());
        assertEquals(2, p.getFood());
    }

    @Test
    void cavePaintingsFoodEffect_appliesWhenPlayerIsAboveThreshold() {
        Player p = makePlayer("Alice", TotemColor.RED, MIN_ARTISTS + 1);
        p.addBuildingCard(new BuildingCard(900, Era.I, 0, new CavePaintingsFoodEffect(), 0));

        resolveOn(p);

        assertEquals((MIN_ARTISTS + 1) * PP_PER_ART, p.getPrestigePoints());
        assertEquals(MIN_ARTISTS + 1, p.getFood());
    }
}
