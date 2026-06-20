package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.ArtistCard;
import it.polimi.ingsw.model.card.EventCard;
import it.polimi.ingsw.model.card.GathererCard;
import it.polimi.ingsw.model.card.HuntEventCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.ShamanCard;
import it.polimi.ingsw.model.card.ShamanicRitualEventCard;
import it.polimi.ingsw.model.card.SustenanceEventCard;
import it.polimi.ingsw.model.card.CavePaintingsEventCard;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EventResolution in the model layer.
 *
 * Verifies: Hunting, Shamanic Ritual, Cave Paintings and Sustenance event resolution.
 */
class GameEventResolutionTest {

    /**
     * Verifies that the hunt event gives food and prestige points only according
     * to the number of Hunter cards owned by each player.
     */
    @Test
    void huntEventRewardsFoodAndPrestigeOnlyForHunters() {
        Player alice = createPlayer("Alice", TotemColor.RED);
        Player bob = createPlayer("Bob", TotemColor.BLUE);
        Game game = createGame(alice, bob);

        alice.addCharacterCard(new HunterCard(1, Era.I, 2, false));
        alice.addCharacterCard(new HunterCard(2, Era.I, 2, true));

        HuntEventCard hunt = new HuntEventCard(100, Era.I, false, 1, 2);

        hunt.resolve(game);

        assertEquals(2, alice.getFood());
        assertEquals(4, alice.getPrestigePoints());

        assertEquals(0, bob.getFood());
        assertEquals(0, bob.getPrestigePoints());
    }

    /**
     * Verifies that sustenance spends all available food before applying
     * prestige point penalties for unfed characters.
     */
    @Test
    void sustenancePaysFoodBeforeApplyingPrestigePenalty() {
        Player alice = createPlayer("Alice", TotemColor.RED);
        Game game = createGame(alice);

        alice.addCharacterCard(new HunterCard(1, Era.I, 2, false));
        alice.addCharacterCard(new HunterCard(2, Era.I, 2, false));
        alice.addCharacterCard(new ArtistCard(3, Era.I, 2));
        alice.addFood(1);

        SustenanceEventCard sustenance = new SustenanceEventCard(200, Era.I, false, 2);

        sustenance.resolve(game);

        assertEquals(0, alice.getFood(),
                "The player must spend available food before losing prestige points.");
        assertEquals(-4, alice.getPrestigePoints(),
                "With 3 characters and only 1 food, 2 unfed characters cause 4 PP loss.");
    }

    /**
     * Verifies that Gatherers reduce the total food required during sustenance.
     * One Gatherer gives a discount of 3 food.
     */
    @Test
    void gathererDiscountCanReduceSustenanceCostToZero() {
        Player alice = createPlayer("Alice", TotemColor.RED);
        Game game = createGame(alice);

        alice.addCharacterCard(new GathererCard(1, Era.I, 2));
        alice.addCharacterCard(new HunterCard(2, Era.I, 2, false));
        alice.addCharacterCard(new ArtistCard(3, Era.I, 2));

        SustenanceEventCard sustenance = new SustenanceEventCard(201, Era.I, false, 2);

        sustenance.resolve(game);

        assertEquals(0, alice.getFood());
        assertEquals(0, alice.getPrestigePoints(),
                "Three characters are fully covered by one Gatherer discount.");
    }

    /**
     * Verifies normal shamanic ritual resolution:
     * the player with the most stars gains PP and all players tied for the fewest stars lose PP.
     */
    @Test
    void shamanicRitualRewardsMajorityAndPenalizesMinorityTie() {
        Player alice = createPlayer("Alice", TotemColor.RED);
        Player bob = createPlayer("Bob", TotemColor.BLUE);
        Player charlie = createPlayer("Charlie", TotemColor.PURPLE);
        Game game = createGame(alice, bob, charlie);

        alice.addCharacterCard(new ShamanCard(1, Era.I, 2, 2));
        bob.addCharacterCard(new ShamanCard(2, Era.I, 2, 1));
        charlie.addCharacterCard(new ShamanCard(3, Era.I, 2, 1));

        ShamanicRitualEventCard ritual = new ShamanicRitualEventCard(300, Era.I, false, 10, 5);

        ritual.resolve(game);

        assertEquals(10, alice.getPrestigePoints());
        assertEquals(-5, bob.getPrestigePoints());
        assertEquals(-5, charlie.getPrestigePoints());
    }

    /**
     * Verifies the special total tie case in shamanic ritual:
     * if every player is tied, everyone first gains PP and then loses PP.
     */
    @Test
    void shamanicRitualTotalTieAppliesGainAndLossToEveryone() {
        Player alice = createPlayer("Alice", TotemColor.RED);
        Player bob = createPlayer("Bob", TotemColor.BLUE);
        Game game = createGame(alice, bob);

        alice.addCharacterCard(new ShamanCard(1, Era.I, 2, 1));
        bob.addCharacterCard(new ShamanCard(2, Era.I, 2, 1));

        ShamanicRitualEventCard ritual = new ShamanicRitualEventCard(301, Era.I, false, 10, 5);

        ritual.resolve(game);

        assertEquals(5, alice.getPrestigePoints());
        assertEquals(5, bob.getPrestigePoints());
    }

    /**
     * Verifies cave paintings event resolution:
     * players below the artist threshold lose PP, while players meeting the threshold gain PP per artist.
     */
    @Test
    void cavePaintingsPenalizesMissingArtistsAndRewardsEnoughArtists() {
        Player alice = createPlayer("Alice", TotemColor.RED);
        Player bob = createPlayer("Bob", TotemColor.BLUE);
        Game game = createGame(alice, bob);

        alice.addCharacterCard(new ArtistCard(1, Era.I, 2));

        bob.addCharacterCard(new ArtistCard(2, Era.I, 2));
        bob.addCharacterCard(new ArtistCard(3, Era.I, 2));

        CavePaintingsEventCard cavePaintings =
                new CavePaintingsEventCard(400, Era.I, false, 2, 3, 2);

        cavePaintings.resolve(game);

        assertEquals(-3, alice.getPrestigePoints(),
                "Alice has fewer artists than required and must lose PP.");
        assertEquals(4, bob.getPrestigePoints(),
                "Bob has 2 artists and gains 2 PP per artist.");
    }

    /**
     * Verifies the event ordering rule:
     * normal events are resolved by era, while sustenance events are always resolved last.
     */
    @Test
    void orderedEventsResolveNormalEventsByEraAndSustenanceLast() {
        Game game = createGame(createPlayer("Alice", TotemColor.RED));

        HuntEventCard huntEraIII = new HuntEventCard(1, Era.III, false, 1, 1);
        ShamanicRitualEventCard ritualEraII = new ShamanicRitualEventCard(2, Era.II, false, 10, 5);
        HuntEventCard huntEraI = new HuntEventCard(3, Era.I, false, 1, 1);
        SustenanceEventCard sustenanceEraI = new SustenanceEventCard(4, Era.I, false, 2);

        Round round = new Round(
                1,
                game,
                Era.I,
                new ArrayList<>(List.of(huntEraIII, sustenanceEraI, ritualEraII, huntEraI)),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );

        List<EventCard> ordered = round.getOrderedEventsForResolution(false);

        assertEquals(List.of(huntEraI, ritualEraII, huntEraIII, sustenanceEraI), ordered);
    }

    private Player createPlayer(String nickname, TotemColor color) {
        return new Player(nickname, new Totem(color, "OFFER_TRACK"));
    }

    private Game createGame(Player... players) {
        return new Game(
                new ArrayList<>(List.of(players)),
                1,
                null,
                Era.I,
                LocalDateTime.now(),
                (Board) null,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );
    }
}