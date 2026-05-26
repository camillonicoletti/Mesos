package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.card.effect.*;
import it.polimi.ingsw.model.enums.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for the ShamanicRitualImmunityEffect behavior when all players are tied.
 *
 * Rule §19: "Nel raro caso in cui tutti i giocatori siano pari: tutti guadagnano prima i PP
 * previsti; poi tutti perdono i PP previsti."
 * Rule §25.3: The immunity building protects the player "se ha MENO icone degli altri."
 *
 * When all players have the same number of shaman stars, nobody has FEWER stars than
 * the others. Therefore ShamanicRitualImmunityEffect must NOT prevent the loss in this case.
 *
 * Bug: the current implementation places all players in both playersWithMostStars and
 * playersWithLeastStars when all are tied, so the immunity check fires unconditionally
 * and the immune player avoids a loss that the rulebook mandates.
 */
public class ShamanicRitualImmunityAllTiedBugTest {

    private Game buildGame(List<Player> players) {
        return new Game(
                players,
                0,
                null,
                null,
                null,
                null,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );
    }

    /**
     * Both players have 0 shaman stars (all tied).
     * One player owns ShamanicRitualImmunityEffect.
     * Expected: both gain ppGain and both lose ppLoss.
     * The immunity must NOT prevent the loss because no one has fewer stars than the others.
     */
    @Test
    void immunityDoesNotApplyWhenAllPlayersAreTied() {
        Player alice = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        Player bob   = new Player("Bob",   new Totem(TotemColor.BLUE, "OFFER_TRACK"));

        // Bob owns the immunity building — but all are tied so it must not shield him.
        bob.addBuildingCard(new BuildingCard(1, Era.I, 0, new ShamanicRitualImmunityEffect(), 0));

        // card: ppGain=4, ppLoss=2
        ShamanicRitualEventCard card = new ShamanicRitualEventCard(10, Era.I, false, 4, 2);
        Game game = buildGame(List.of(alice, bob));
        card.resolve(game);

        // All-tied: everyone gains 4 then loses 2 → net +2 for each player.
        assertEquals(2, alice.getPrestigePoints(), "Alice (no building) should gain 4 and lose 2 → net +2");
        assertEquals(2, bob.getPrestigePoints(),   "Bob (immunity building) should ALSO lose 2 — immunity must not apply when all are tied");
    }

    /**
     * Three players, all with the same number of stars (1 each).
     * One player owns ShamanicRitualImmunityEffect.
     * Expected: all gain ppGain, all lose ppLoss.
     */
    @Test
    void immunityDoesNotApplyWithThreePlayersTied() {
        Player alice   = new Player("Alice",   new Totem(TotemColor.RED,    "OFFER_TRACK"));
        Player bob     = new Player("Bob",     new Totem(TotemColor.BLUE,   "OFFER_TRACK"));
        Player charlie = new Player("Charlie", new Totem(TotemColor.PURPLE, "OFFER_TRACK"));

        alice.addCharacterCard(new ShamanCard(1, Era.I, 2, 1));
        bob.addCharacterCard(  new ShamanCard(2, Era.I, 2, 1));
        charlie.addCharacterCard(new ShamanCard(3, Era.I, 2, 1));

        // Bob owns the immunity building.
        bob.addBuildingCard(new BuildingCard(4, Era.I, 0, new ShamanicRitualImmunityEffect(), 0));

        // card: ppGain=3, ppLoss=2
        ShamanicRitualEventCard card = new ShamanicRitualEventCard(10, Era.I, false, 3, 2);
        Game game = buildGame(List.of(alice, bob, charlie));
        card.resolve(game);

        // All tied: gain 3, lose 2 → net +1 for each.
        assertEquals(1, alice.getPrestigePoints(),   "Alice should have net +1");
        assertEquals(1, bob.getPrestigePoints(),     "Bob should have net +1 — immunity must not apply when all are tied");
        assertEquals(1, charlie.getPrestigePoints(), "Charlie should have net +1");
    }

    /**
     * Sanity check: immunity DOES apply when Bob genuinely has fewer stars than Alice.
     * This ensures the fix does not break the normal immunity case.
     */
    @Test
    void immunityAppliesWhenPlayerGenuinelyHasFewerStars() {
        Player alice = new Player("Alice", new Totem(TotemColor.RED,  "OFFER_TRACK"));
        Player bob   = new Player("Bob",   new Totem(TotemColor.BLUE, "OFFER_TRACK"));

        alice.addCharacterCard(new ShamanCard(1, Era.I, 2, 3)); // 3 stars — winner
        // Bob has 0 stars — genuine loser, owns immunity
        bob.addBuildingCard(new BuildingCard(2, Era.I, 0, new ShamanicRitualImmunityEffect(), 0));

        ShamanicRitualEventCard card = new ShamanicRitualEventCard(10, Era.I, false, 4, 2);
        Game game = buildGame(List.of(alice, bob));
        card.resolve(game);

        assertEquals(4,  alice.getPrestigePoints(), "Alice (winner) gains 4 PP");
        assertEquals(0,  bob.getPrestigePoints(),   "Bob is shielded by immunity when he genuinely has fewer stars");
    }
}
