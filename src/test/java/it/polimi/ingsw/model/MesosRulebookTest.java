package it.polimi.ingsw.model;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.controller.listener.GameListener;
import it.polimi.ingsw.controller.state.ActionResolutionState;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.board.TurnOrderTile;
import it.polimi.ingsw.model.board.TurnSlot;
import it.polimi.ingsw.model.card.ArtistCard;
import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.EventCard;
import it.polimi.ingsw.model.card.HuntEventCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.ShamanicRitualEventCard;
import it.polimi.ingsw.model.card.SustenanceEventCard;
import it.polimi.ingsw.model.card.CavePaintingsEventCard;
import it.polimi.ingsw.model.card.effect.EndPPEffect;
import it.polimi.ingsw.model.card.effect.ShamanicRitualImmunityEffect;
import it.polimi.ingsw.model.card.effect.TotemSlotFoodBonusEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge-case rule tests aligned with the official Mesos rulebook.
 *
 * Each test cites the rulebook clause it verifies.  Tests fail if the
 * implementation drifts from the rules.
 */
class MesosRulebookTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private GameController createStartedTwoPlayerGame() {
        GameController c = new GameController();
        c.onMessageReceived("Alice", new JoinGameAction(new Totem(TotemColor.RED,  "OFFER")));
        c.onMessageReceived("Bob",   new JoinGameAction(new Totem(TotemColor.BLUE, "OFFER")));
        c.onMessageReceived("Alice", new SetPlayerCountAction(2));
        c.onMessageReceived("Alice", new ReadyAction(true));
        c.onMessageReceived("Bob",   new ReadyAction(true));
        return c;
    }

    private Player findPlayer(GameController ctrl, String nick) {
        return ctrl.getGame().getPlayers().stream()
                .filter(p -> p.getNickname().equals(nick))
                .findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> roundList(Round round, String field) {
        try {
            Field f = Round.class.getDeclaredField(field);
            f.setAccessible(true);
            return (List<T>) f.get(round);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot access Round." + field, e);
        }
    }

    private Game minimalTwoPlayerGame() {
        List<Player> players = new ArrayList<>(List.of(
                new Player("Alice", new Totem(TotemColor.RED,  "OFFER")),
                new Player("Bob",   new Totem(TotemColor.BLUE, "OFFER"))
        ));
        Game game = new Game(players, 1, null, Era.I, LocalDateTime.now(),
                BoardFactory.createBoard(2),
                new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        for (Player p : players) p.setGame(game);
        Round round = new Round(1, game, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>());
        game.setCurrentRound(round);
        return game;
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║ T1 — TotemSlotFoodBonusEffect (Building "+1🍖 on turn-order bonuses")║
    // ║                                                                      ║
    // ║ Rule (Rulebook p.8): "If, at the end of your turn ... you place      ║
    // ║ your totem on a slot that grants a food bonus, take 1 additional     ║
    // ║ food. If you place it on the last slot, you pay the food normally   ║
    // ║ and the building has no effect."                                     ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * With the bonus building, when the player's totem lands on slot-1
     * (GainFoodSlotEffect, +1 food in 2-player), the player receives an
     * EXTRA +1 food from the building, totalling +2.
     *
     * Lower row is replaced with a single Hunter that has NO food icon, so
     * the pick itself triggers no immediate food effect — isolating the
     * food gain to slot-1 + building bonus.
     */
    @Test
    void totemSlotFoodBonusBuildingAddsExtraFoodOnGainSlot() {
        GameController ctrl = createStartedTwoPlayerGame();

        // First placer goes B (lower pick → resolves first, lands on slot-1)
        String first  = ctrl.getCurrentTurnPlayerNickname();
        String second = "Alice".equals(first) ? "Bob" : "Alice";
        ctrl.onMessageReceived(first,  new PlaceTotemAction('B'));
        ctrl.onMessageReceived(second, new PlaceTotemAction('C'));

        Player resolverPlayer = findPlayer(ctrl, ctrl.getCurrentActionPlayerNickname());

        // Give the resolver the totem-bonus building BEFORE they finish their turn.
        resolverPlayer.addBuildingCard(new BuildingCard(
                900, Era.I, 0, new TotemSlotFoodBonusEffect(), 0));

        // Replace lower row with a deterministic non-effect card.
        Round round = ctrl.getGame().getCurrentRound();
        List<CharacterCard> lower = roundList(round, "lowerRowTribute");
        lower.clear();
        lower.add(new HunterCard(950, Era.I, 0, false));

        int foodBefore = resolverPlayer.getFood();

        ctrl.onMessageReceived(resolverPlayer.getNickname(),
                new PickCardAction("950", false));

        // Slot-1 GainFoodSlotEffect(1) gives +1, building bonus gives another +1
        assertEquals(foodBefore + 2, resolverPlayer.getFood(),
                "Slot-1 (+1 food) + TotemSlotFoodBonus building (+1 food) must yield +2 food");
    }

    /**
     * The bonus building must NOT trigger when the slot is a
     * PayFoodSlotEffect (last slot) — only on gain-food slots.
     */
    @Test
    void totemSlotFoodBonusBuildingDoesNotTriggerOnLastSlot() {
        GameController ctrl = createStartedTwoPlayerGame();

        String first  = ctrl.getCurrentTurnPlayerNickname();
        String second = "Alice".equals(first) ? "Bob" : "Alice";
        // First on B, second on C.  B resolves first (slot-1), C resolves second (slot-2 = pay).
        ctrl.onMessageReceived(first,  new PlaceTotemAction('B'));
        ctrl.onMessageReceived(second, new PlaceTotemAction('C'));

        // Replace both rows with deterministic non-effect cards so the pick
        // itself does not move food (no Hunter "+" immediate effect, no
        // FullSet/InventorPair triggers).
        Round round = ctrl.getGame().getCurrentRound();
        List<CharacterCard> lower = roundList(round, "lowerRowTribute");
        lower.clear();
        lower.add(new HunterCard(960, Era.I, 0, false));
        List<CharacterCard> upper = roundList(round, "upperRowTribute");
        upper.clear();
        upper.add(new HunterCard(961, Era.I, 0, false));

        // First player resolves
        ctrl.onMessageReceived(first, new PickCardAction("960", false));

        // Now C is active and will land on slot-2 (PayFoodSlotEffect).
        // Give C the totem-bonus building — it must NOT fire on a pay slot.
        Player cPlayer = findPlayer(ctrl, second);
        cPlayer.addBuildingCard(new BuildingCard(
                901, Era.I, 0, new TotemSlotFoodBonusEffect(), 0));

        int foodBefore = cPlayer.getFood();
        int ppBefore   = cPlayer.getPrestigePoints();
        assertTrue(foodBefore >= 1, "Sanity: C must have food to pay the slot-2 penalty");

        ctrl.onMessageReceived(second, new PickCardAction("961", true));

        // Slot-2 PayFoodSlotEffect(1, 2): if affordable, -1 food.  Building must NOT fire.
        assertEquals(foodBefore - 1, cPlayer.getFood(),
                "Slot-2 must spend exactly 1 food; the totem-bonus building must NOT trigger here");
        assertEquals(ppBefore, cPlayer.getPrestigePoints(),
                "PP must not change when player can pay the food penalty");
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║ T2 — Sustenance: cannot choose to skip food payment                  ║
    // ║                                                                      ║
    // ║ Rule (Rulebook p.6): "You cannot choose to lose Prestige Points      ║
    // ║ instead of paying Food (for example, if you have 2 Characters and    ║
    // ║ only 1 Food, you must still pay 1 Food and lose 2 PP; you cannot     ║
    // ║ decide to lose 4 PP and keep the Food)."                             ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    @Test
    void sustenanceForcesFoodPaymentBeforeFallingBackToPP() {
        Game game = minimalTwoPlayerGame();
        Player alice = game.getPlayers().get(0);

        alice.addCharacterCard(new HunterCard(1, Era.I, 0, false));
        alice.addCharacterCard(new HunterCard(2, Era.I, 0, false));
        alice.addFood(1);

        new SustenanceEventCard(500, Era.I, false, 2).resolve(game);

        // 2 characters, 1 food, 0 gatherers -> required = 2, paid = 1, missing = 1
        // Expected: food = 0, PP = -2 (1 missing × 2 ppPenaltyPerUnfed)
        assertEquals(0, alice.getFood(),
                "All available food (1) must be spent before applying PP penalty");
        assertEquals(-2, alice.getPrestigePoints(),
                "Missing food (1) must trigger PP loss = 1 × ppPenalty(2) = -2");
    }

    /**
     * If the player can fully pay food, NO PP penalty must apply.
     */
    @Test
    void sustenanceWithEnoughFoodAppliesNoPPPenalty() {
        Game game = minimalTwoPlayerGame();
        Player alice = game.getPlayers().get(0);

        alice.addCharacterCard(new HunterCard(1, Era.I, 0, false));
        alice.addCharacterCard(new HunterCard(2, Era.I, 0, false));
        alice.addFood(5);

        new SustenanceEventCard(501, Era.I, false, 2).resolve(game);

        assertEquals(3, alice.getFood(), "5 - 2 (one per character) = 3");
        assertEquals(0, alice.getPrestigePoints(),
                "No PP penalty when food covers all characters");
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║ T3 — Shamanic Ritual: all tied + ImmunityEffect                      ║
    // ║                                                                      ║
    // ║ Rule (Rulebook p.6): "in the rare case of a tie among all players,   ║
    // ║ everyone first gains PP and then loses PP."                          ║
    // ║                                                                      ║
    // ║ Team policy on ImmunityEffect under all-tied: the building text      ║
    // ║ reads "you do not lose PP if you have fewer icons than the others".  ║
    // ║ When everyone is tied nobody has fewer icons, so the precondition is ║
    // ║ not met and the immunity is bypassed. (Contrast with DoublePP, whose ║
    // ║ printed text explicitly extends the bonus to ties.)                  ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    @Test
    void shamanicRitualAllTiedImmunityIsBypassedSoLossStillApplies() {
        Game game = minimalTwoPlayerGame();
        Player alice = game.getPlayers().get(0);
        Player bob   = game.getPlayers().get(1);

        // No shamans → both have 0 stars → all-tied scenario
        alice.addBuildingCard(new BuildingCard(
                700, Era.I, 0, new ShamanicRitualImmunityEffect(), 0));

        new ShamanicRitualEventCard(800, Era.I, false, 10, 5).resolve(game);

        // Both gain ppGain (10) because both are in maxStars.
        // Both are also in minStars; immunity does NOT trigger under all-tied
        // (Alice does not have fewer icons than Bob), so both lose ppLoss (5).
        assertEquals(5, alice.getPrestigePoints(),
                "Alice gains 10 then loses 5 = 5: immunity bypassed in all-tied");
        assertEquals(5, bob.getPrestigePoints(),
                "Bob gains 10 then loses 5 = 5 net");
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║ T4 — Cave Paintings boundary on minArtists                           ║
    // ║                                                                      ║
    // ║ Rule (Rulebook p.6): "if you have zero or one Artist you lose 3 PP"  ║
    // ║ (i.e. minArtists = 2: penalty when artists < minArtists).            ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    @Test
    void cavePaintingsPenaltyAppliesStrictlyBelowMinArtists() {
        // minArtists = 2, ppPenalty = 3, ppPerArtist = 2

        // Case A — 0 artists → -3 PP
        {
            Game g = minimalTwoPlayerGame();
            Player alice = g.getPlayers().get(0);
            new CavePaintingsEventCard(900, Era.I, false, 2, 3, 2).resolve(g);
            assertEquals(-3, alice.getPrestigePoints(),
                    "0 artists < 2 minArtists → -3 PP");
        }

        // Case B — 1 artist → -3 PP
        {
            Game g = minimalTwoPlayerGame();
            Player alice = g.getPlayers().get(0);
            alice.addCharacterCard(new ArtistCard(10, Era.I, 0));
            new CavePaintingsEventCard(901, Era.I, false, 2, 3, 2).resolve(g);
            assertEquals(-3, alice.getPrestigePoints(),
                    "1 artist < 2 minArtists → -3 PP");
        }

        // Case C — 2 artists → +4 PP (2 × ppPerArtist=2)
        {
            Game g = minimalTwoPlayerGame();
            Player alice = g.getPlayers().get(0);
            alice.addCharacterCard(new ArtistCard(11, Era.I, 0));
            alice.addCharacterCard(new ArtistCard(12, Era.I, 0));
            new CavePaintingsEventCard(902, Era.I, false, 2, 3, 2).resolve(g);
            assertEquals(4, alice.getPrestigePoints(),
                    "2 artists ≥ minArtists → +2 × 2 = 4 PP");
        }

        // Case D — 3 artists → +6 PP
        {
            Game g = minimalTwoPlayerGame();
            Player alice = g.getPlayers().get(0);
            alice.addCharacterCard(new ArtistCard(13, Era.I, 0));
            alice.addCharacterCard(new ArtistCard(14, Era.I, 0));
            alice.addCharacterCard(new ArtistCard(15, Era.I, 0));
            new CavePaintingsEventCard(903, Era.I, false, 2, 3, 2).resolve(g);
            assertEquals(6, alice.getPrestigePoints(),
                    "3 artists ≥ minArtists → +3 × 2 = 6 PP");
        }
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║ T5 — Initial food per turn-order position                            ║
    // ║                                                                      ║
    // ║ Rule (Rulebook p.3): "the 1st player receives 2 Food, the 2nd and    ║
    // ║ 3rd player receive 3 Food, while the 4th and 5th player receive      ║
    // ║ 4 Food."                                                             ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    @Test
    void initialFoodFollowsTurnOrderPositionRule() {
        // Build a 5-player game and start it manually so we can inspect the
        // turn-order mapping (assignRandomTurnOrder is non-deterministic).
        List<Player> players = new ArrayList<>(List.of(
                new Player("P0", new Totem(TotemColor.RED,    "OFFER")),
                new Player("P1", new Totem(TotemColor.BLUE,   "OFFER")),
                new Player("P2", new Totem(TotemColor.PURPLE, "OFFER")),
                new Player("P3", new Totem(TotemColor.YELLOW, "OFFER")),
                new Player("P4", new Totem(TotemColor.WHITE,  "OFFER"))
        ));
        Game game = new Game(players, 0, null, Era.I, LocalDateTime.now(),
                null,
                buildSyntheticTributeDeck(40),
                buildSyntheticBuildingDeck(10), new ArrayList<>(), new ArrayList<>());

        game.start();

        int[] expectedFoodByPos = {2, 3, 3, 4, 4};
        TurnOrderTile turnTile = game.getBoard().getTurnOrderTile();
        int position = 0;
        for (TurnSlot slot : turnTile.getSlots()) {
            if (!slot.isOccupied()) continue;
            Totem occupier = slot.getOccupiedBy();
            Player owner = players.stream()
                    .filter(p -> p.getTotem() == occupier)
                    .findFirst().orElseThrow();
            assertEquals(expectedFoodByPos[position], owner.getFood(),
                    "Player at position " + position + " must have "
                            + expectedFoodByPos[position] + " food (rulebook p.3)");
            position++;
        }
        assertEquals(5, position, "Sanity: 5 turn-order slots must be occupied");
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║ T6 — Builder discount cannot reduce cost below zero                  ║
    // ║                                                                      ║
    // ║ Rule (Rulebook p.5): "If you have multiple Builders, sum the         ║
    // ║ discounts from all the cards, but you can never reduce the cost      ║
    // ║ below zero."                                                         ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    @Test
    void buildingFinalCostNeverGoesNegativeWithExcessiveBuilderDiscount() {
        Game game = minimalTwoPlayerGame();
        Player alice = game.getPlayers().get(0);

        // Two builders, 5 + 5 = 10 total discount
        alice.addCharacterCard(new BuilderCard(60, Era.I, 0, 1, 5));
        alice.addCharacterCard(new BuilderCard(61, Era.I, 0, 1, 5));

        // Building costs only 2 -> finalCost should be max(0, 2 - 10) = 0
        BuildingCard cheap = new BuildingCard(80, Era.I, 2, new EndPPEffect(), 0);
        // Inject into upper row buildings
        roundList(game.getCurrentRound(), "upperRowBuildings").add(cheap);

        int foodBefore = alice.getFood();
        // Even with 0 food, the player should be able to buy this building.
        // Drain alice's food to 0 first.
        if (foodBefore > 0) alice.spendFood(foodBefore);

        assertTrue(game.canAffordBuilding(alice, cheap),
                "With discount ≥ cost, finalCost must be 0 → always affordable");

        game.takeBuildingCard(alice, game.getCurrentRound().getUpperRowBuildings(), cheap);

        assertEquals(0, alice.getFood(),
                "Player food must remain 0 (no negative spend)");
        assertTrue(alice.getTribe().getBuildingCards().contains(cheap),
                "Building must be added to the tribe");
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║ T7 — Last round resolves upper-row events                            ║
    // ║                                                                      ║
    // ║ Rule (Rulebook p.7): "At the end of this round resolve all visible   ║
    // ║ Events, including those in the upper row."                           ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    @Test
    void getOrderedEventsForResolutionIncludesUpperOnlyOnLastRound() {
        Game game = minimalTwoPlayerGame();
        Round round = game.getCurrentRound();

        EventCard lowerEvent = new HuntEventCard(1000, Era.I, false, 1, 1);
        EventCard upperEvent = new HuntEventCard(1001, Era.II, false, 1, 1);
        roundList(round, "lowerRowEventCards").add(lowerEvent);
        roundList(round, "upperRowEventCards").add(upperEvent);

        // Non-last round: only lower events
        List<EventCard> nonLast = round.getOrderedEventsForResolution(false);
        assertEquals(List.of(lowerEvent), nonLast,
                "Non-last round must NOT resolve upper-row events");

        // Last round: both
        List<EventCard> last = round.getOrderedEventsForResolution(true);
        assertTrue(last.contains(lowerEvent), "Last round must include lower events");
        assertTrue(last.contains(upperEvent), "Last round must include upper events");
        assertEquals(2, last.size());
    }

    // ── Synthetic deck helpers ───────────────────────────────────────────────

    private List<Card> buildSyntheticTributeDeck(int size) {
        List<Card> deck = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            deck.add(new HunterCard(2000 + i, Era.I, 0, false));
        }
        return deck;
    }

    private List<BuildingCard> buildSyntheticBuildingDeck(int size) {
        List<BuildingCard> deck = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            deck.add(new BuildingCard(3000 + i, Era.I, 0, new EndPPEffect(), 0));
        }
        return deck;
    }
}
