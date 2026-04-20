package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.card.effect.*;
import it.polimi.ingsw.model.enums.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ShamanicRitualEventCard.resolve() covering all three building effects:
 * 1. Shaman3ExtraStarsEffect      — adds 3 virtual stars to the player's count
 * 2. ShamanicRitualDoublePPEffect — doubles PP awarded to the player with most stars
 * 3. ShamanicRitualImmunityEffect — protects the player with fewest stars from losing PP
 */
public class ShamanicRitualEventCardTest {

    private Game buildGame(List<Player> players) {
        return new Game(
                players,
                0,
                null,
                null,
                null,
                null,
                new java.util.ArrayList<>(),
                new java.util.ArrayList<>(),
                new java.util.ArrayList<>(),
                new java.util.ArrayList<>()
        );
    }

    private ShamanicRitualEventCard buildCard() {
        return new ShamanicRitualEventCard(1, Era.I, false, 4, 2);
    }

    // -------------------------------------------------------
    // base behavior
    // -------------------------------------------------------

    @Test
    void mostStarsPlayerGainsPP() {
        Player p1 = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        Player p2 = new Player("Bob", new Totem(TotemColor.BLUE, "OFFER_TRACK"));

        p1.addCharacterCard(new ShamanCard(1, Era.I, 2, 3)); // 3 stars
        p2.addCharacterCard(new ShamanCard(2, Era.I, 2, 1)); // 1 star

        Game game = buildGame(List.of(p1, p2));
        buildCard().resolve(game);

        assertEquals(4, p1.getPrestigePoints());
        assertEquals(-2, p2.getPrestigePoints());
    }

    // -------------------------------------------------------
    // Shaman3ExtraStarsEffect
    // -------------------------------------------------------

    @Test
    void shaman3ExtraStarsEffectAddsVirtualStars() {
        Player p1 = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        Player p2 = new Player("Bob", new Totem(TotemColor.BLUE, "OFFER_TRACK"));

        // p1 has 0 real stars but +3 from building → 3 total
        p1.addBuildingCard(new BuildingCard(1, Era.I, 0, new Shaman3ExtraStarsEffect(), 0));

        // p2 has 1 real star
        p2.addCharacterCard(new ShamanCard(2, Era.I, 2, 1));

        // p1 has 3 virtual stars > p2's 1 real star → p1 wins
        Game game = buildGame(List.of(p1, p2));
        buildCard().resolve(game);

        assertEquals(4, p1.getPrestigePoints());
        assertEquals(-2, p2.getPrestigePoints());
    }

    // -------------------------------------------------------
    // ShamanicRitualDoublePPEffect
    // -------------------------------------------------------

    @Test
    void shamanicRitualDoublePPEffectDoublesPPForWinner() {
        Player p1 = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        Player p2 = new Player("Bob", new Totem(TotemColor.BLUE, "OFFER_TRACK"));

        p1.addCharacterCard(new ShamanCard(1, Era.I, 2, 3)); // 3 stars
        p1.addBuildingCard(new BuildingCard(2, Era.I, 0, new ShamanicRitualDoublePPEffect(), 0));
        p2.addCharacterCard(new ShamanCard(3, Era.I, 2, 1)); // 1 star

        Game game = buildGame(List.of(p1, p2));
        buildCard().resolve(game); // card gives 4 PP to winner

        assertEquals(8, p1.getPrestigePoints()); // 4 * 2 = 8
        assertEquals(-2, p2.getPrestigePoints());
    }

    // -------------------------------------------------------
    // ShamanicRitualImmunityEffect
    // -------------------------------------------------------

    @Test
    void shamanicRitualImmunityEffectProtectsLoser() {
        Player p1 = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        Player p2 = new Player("Bob", new Totem(TotemColor.BLUE, "OFFER_TRACK"));

        p1.addCharacterCard(new ShamanCard(1, Era.I, 2, 3)); // 3 stars
        p2.addBuildingCard(new BuildingCard(2, Era.I, 0, new ShamanicRitualImmunityEffect(), 0));

        Game game = buildGame(List.of(p1, p2));
        buildCard().resolve(game);

        assertEquals(4, p1.getPrestigePoints());
        assertEquals(0, p2.getPrestigePoints()); // immune — no PP lost
    }
}
