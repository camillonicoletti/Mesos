package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.*;
import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.card.effect.*;
import it.polimi.ingsw.model.enums.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for all building effects, grouped by effect type.
 * Each section verifies that the effect is correctly triggered
 * in the right moment of the game.
 */
public class BuildingEffectsTest {

    private Game buildGame(List<Player> players) {
        return new Game(players, 1, null, Era.I, null, null,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    private Game buildGameWithBoard(List<Player> players, Board board) {
        return new Game(players, 1, null, Era.I, null, board,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    // -------------------------------------------------------
    // FullSetFoodEffect
    // triggered in: Player.addCharacterCard()
    // -------------------------------------------------------

    @Test
    void fullSetFoodEffectGivesFoodWhenSetCompleted() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addBuildingCard(new BuildingCard(1, Era.I, 0, new FullSetFoodEffect(), 0));

        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new BuilderCard(2, Era.I, 2, 0, 0));
        player.addCharacterCard(new GathererCard(3, Era.I, 2));
        player.addCharacterCard(new HunterCard(4, Era.I, 2, false));
        player.addCharacterCard(new InventorCard(5, Era.I, 2, InventorIcons.INVENTION_1));
        player.addCharacterCard(new ShamanCard(6, Era.I, 2, 2)); // completes set

        assertEquals(5, player.getFood()); // +5 per set completato
    }

    @Test
    void fullSetFoodEffectDoesNotTriggerWithoutBuilding() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));

        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new BuilderCard(2, Era.I, 2, 0, 0));
        player.addCharacterCard(new GathererCard(3, Era.I, 2));
        player.addCharacterCard(new HunterCard(4, Era.I, 2, false));
        player.addCharacterCard(new InventorCard(5, Era.I, 2, InventorIcons.INVENTION_1));
        player.addCharacterCard(new ShamanCard(6, Era.I, 2, 2));

        assertEquals(0, player.getFood());
    }

    // -------------------------------------------------------
    // InventorPairFoodEffect
    // triggered in: Player.addCharacterCard()
    // -------------------------------------------------------

    @Test
    void inventorPairFoodEffectGivesFoodWhenPairFormed() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addBuildingCard(new BuildingCard(1, Era.I, 0, new InventorPairFoodEffect(), 0));

        player.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        player.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_1)); // coppia!

        assertEquals(3, player.getFood()); // +3 per coppia
    }

    @Test
    void inventorPairFoodEffectDoesNotTriggerWithDifferentIcons() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addBuildingCard(new BuildingCard(1, Era.I, 0, new InventorPairFoodEffect(), 0));

        player.addCharacterCard(new InventorCard(1, Era.I, 2, InventorIcons.INVENTION_1));
        player.addCharacterCard(new InventorCard(2, Era.I, 2, InventorIcons.INVENTION_2)); // icone diverse

        assertEquals(0, player.getFood());
    }

    // -------------------------------------------------------
    // TotemSlotFoodBonusEffect
    // triggered in: Round.placeTotemOrderForPlayer() only on GAIN_FOOD slot
    // -------------------------------------------------------

    @Test
    void totemSlotFoodBonusEffectGivesFoodOnGainFoodSlot() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addBuildingCard(new BuildingCard(1, Era.I, 0, new TotemSlotFoodBonusEffect(), 0));

        List<TurnSlot> slots = new ArrayList<>();
        slots.add(new TurnSlot(1, new GainFoodSlotEffect(2)));
        slots.add(new TurnSlot(2, new NoSlotEffect()));

        Board board = new Board(new ArrayList<>(), new TurnOrderTile(2, slots));
        Game game = buildGameWithBoard(List.of(player), board);

        Round round = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        game.setCurrentRound(round);

        round.placeTotemOrderForPlayer(player); // slot 1 → GAIN_FOOD 2 + 1 building bonus

        assertEquals(3, player.getFood()); // 2 base + 1 building bonus
    }

    @Test
    void totemSlotFoodBonusEffectDoesNotTriggerOnPayFoodSlot() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addFood(5);
        player.addBuildingCard(new BuildingCard(1, Era.I, 0, new TotemSlotFoodBonusEffect(), 0));

        List<TurnSlot> slots = new ArrayList<>();
        slots.add(new TurnSlot(1, new PayFoodSlotEffect(1, 2)));
        slots.add(new TurnSlot(2, new NoSlotEffect()));

        Board board = new Board(new ArrayList<>(), new TurnOrderTile(2, slots));
        Game game = buildGameWithBoard(List.of(player), board);

        Round round = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        game.setCurrentRound(round);

        round.placeTotemOrderForPlayer(player); // slot 1 → PAY_FOOD, no building bonus

        assertEquals(4, player.getFood()); // 5 - 1, no bonus
    }

    @Test
    void totemSlotFoodBonusEffectDoesNotTriggerWithoutBuilding() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));

        List<TurnSlot> slots = new ArrayList<>();
        slots.add(new TurnSlot(1, new GainFoodSlotEffect(2)));
        slots.add(new TurnSlot(2, new NoSlotEffect()));

        Board board = new Board(new ArrayList<>(), new TurnOrderTile(2, slots));
        Game game = buildGameWithBoard(List.of(player), board);

        Round round = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        game.setCurrentRound(round);

        round.placeTotemOrderForPlayer(player);

        assertEquals(2, player.getFood()); // base only, no bonus
    }

    // -------------------------------------------------------
    // Hunt1Food1PPEffect
    // triggered in: HuntEventCard.resolve()
    // -------------------------------------------------------

    @Test
    void hunt1Food1PPEffectAddsExtraFoodAndPPPerHunter() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addCharacterCard(new HunterCard(1, Era.I, 2, true));
        player.addCharacterCard(new HunterCard(2, Era.I, 2, true));
        player.addBuildingCard(new BuildingCard(1, Era.I, 0, new Hunt1Food1PPEffect(), 0));

        Game game = buildGame(List.of(player));
        new HuntEventCard(1, Era.I, false, 1, 2).resolve(game);

        // base: 2×1 food + 2×2 PP = 2 food + 4 PP
        // building bonus: 2×1 food + 2×1 PP = 2 food + 2 PP
        assertEquals(4, player.getFood());
        assertEquals(6, player.getPrestigePoints());
    }

    @Test
    void hunt1Food1PPEffectDoesNotTriggerWithoutBuilding() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addCharacterCard(new HunterCard(1, Era.I, 2, true));
        player.addCharacterCard(new HunterCard(2, Era.I, 2, true));

        Game game = buildGame(List.of(player));
        new HuntEventCard(1, Era.I, false, 1, 2).resolve(game);

        assertEquals(2, player.getFood());
        assertEquals(4, player.getPrestigePoints());
    }

    // -------------------------------------------------------
    // CavePaintingsFoodEffect
    // triggered in: CavePaintingsEventCard.resolve() — always, regardless of minimum
    // -------------------------------------------------------

    @Test
    void cavePaintingsFoodEffectAddsOneFoodPerArtist() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new ArtistCard(2, Era.I, 2));
        player.addCharacterCard(new ArtistCard(3, Era.I, 2));
        player.addBuildingCard(new BuildingCard(1, Era.I, 0, new CavePaintingsFoodEffect(), 0));

        Game game = buildGame(List.of(player));
        new CavePaintingsEventCard(1, Era.I, false, 2, 3, 1).resolve(game);

        // base: 3×1 PP = 3 PP
        // building bonus: 3×1 food = 3 food
        assertEquals(3, player.getFood());
        assertEquals(3, player.getPrestigePoints());
    }

    @Test
    void cavePaintingsFoodEffectTriggersEvenWhenBelowMinimum() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addCharacterCard(new ArtistCard(1, Era.I, 2)); // 1, minimum is 2
        player.addBuildingCard(new BuildingCard(1, Era.I, 0, new CavePaintingsFoodEffect(), 0));

        Game game = buildGame(List.of(player));
        new CavePaintingsEventCard(1, Era.I, false, 2, 3, 1).resolve(game);

        assertEquals(1, player.getFood());        // 1 artist × 1 food, even below threshold
        assertEquals(-3, player.getPrestigePoints()); // below threshold → loses PP
    }

    // -------------------------------------------------------
    // SustenanceFoodDiscountEffect
    // triggered in: SustenanceEventCard.resolve()
    // -------------------------------------------------------

    @Test
    void sustenanceFoodDiscountEffectReducesFoodRequired() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addFood(10);

        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new ArtistCard(2, Era.I, 2));
        player.addCharacterCard(new ArtistCard(3, Era.I, 2));
        player.addCharacterCard(new ArtistCard(4, Era.I, 2)); // 4 characters

        // 1 discount per Artist → total discount 4 → food required 0
        player.addBuildingCard(new BuildingCard(1, Era.I, 0,
                new SustenanceFoodDiscountEffect(ArtistCard.class), 0));

        Game game = buildGame(List.of(player));
        new SustenanceEventCard(1, Era.I, false, 2).resolve(game);

        assertEquals(10, player.getFood()); // pays nothing
        assertEquals(0, player.getPrestigePoints());
    }

    @Test
    void sustenanceFoodDiscountEffectDoesNotTriggerWithoutBuilding() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addFood(2);

        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new ArtistCard(2, Era.I, 2));
        player.addCharacterCard(new ArtistCard(3, Era.I, 2));
        player.addCharacterCard(new ArtistCard(4, Era.I, 2));

        Game game = buildGame(List.of(player));
        new SustenanceEventCard(1, Era.I, false, 2).resolve(game);

        // 4 required, 2 available → pays 2 food + 2 PP per missing
        assertEquals(0, player.getFood());
        assertEquals(-4, player.getPrestigePoints());
    }

    // -------------------------------------------------------
    // Shaman3ExtraStarsEffect
    // ShamanicRitualDoublePPEffect
    // ShamanicRitualImmunityEffect
    // triggered in: ShamanicRitualEventCard.resolve()
    // -------------------------------------------------------

    @Test
    void shaman3ExtraStarsEffectMakesPlayerWin() {
        Player p1 = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        Player p2 = new Player("Bob", new Totem(TotemColor.BLUE, "OFFER_TRACK"));

        // p1 has 0 real stars but +3 from the building → beats p2 who has 1 star
        p1.addBuildingCard(new BuildingCard(1, Era.I, 0, new Shaman3ExtraStarsEffect(), 0));
        p2.addCharacterCard(new ShamanCard(1, Era.I, 2, 1)); // 1 star

        Game game = buildGame(List.of(p1, p2));
        new ShamanicRitualEventCard(1, Era.I, false, 4, 2).resolve(game);

        assertEquals(4, p1.getPrestigePoints());
        assertEquals(-2, p2.getPrestigePoints());
    }

    @Test
    void shamanicRitualDoublePPEffectDoublesPPForWinner() {
        Player p1 = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        Player p2 = new Player("Bob", new Totem(TotemColor.BLUE, "OFFER_TRACK"));

        p1.addCharacterCard(new ShamanCard(1, Era.I, 2, 3)); // 3 stars
        p1.addBuildingCard(new BuildingCard(2, Era.I, 0, new ShamanicRitualDoublePPEffect(), 0));
        p2.addCharacterCard(new ShamanCard(2, Era.I, 2, 1)); // 1 star

        Game game = buildGame(List.of(p1, p2));
        new ShamanicRitualEventCard(1, Era.I, false, 4, 2).resolve(game);

        assertEquals(8, p1.getPrestigePoints()); // 4 × 2
        assertEquals(-2, p2.getPrestigePoints());
    }

    @Test
    void shamanicRitualImmunityEffectProtectsLoser() {
        Player p1 = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        Player p2 = new Player("Bob", new Totem(TotemColor.BLUE, "OFFER_TRACK"));

        p1.addCharacterCard(new ShamanCard(1, Era.I, 2, 3)); // 3 stars
        p2.addBuildingCard(new BuildingCard(2, Era.I, 0, new ShamanicRitualImmunityEffect(), 0));

        Game game = buildGame(List.of(p1, p2));
        new ShamanicRitualEventCard(1, Era.I, false, 4, 2).resolve(game);

        assertEquals(4, p1.getPrestigePoints());
        assertEquals(0, p2.getPrestigePoints()); // immune, non perde PP
    }

    // -------------------------------------------------------
    // EndPPEffect
    // EndBuilderPPEffect
    // EndFullSetPPEffect
    // EndCharacterCountPPEffect
    // triggered in: Game.calculateFinalScores()
    // -------------------------------------------------------

    @Test
    void endPPEffectAdds25PPAtEndOfGame() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addBuildingCard(new BuildingCard(1, Era.I, 0, new EndPPEffect(), 0));

        Game game = buildGame(List.of(player));
        game.calculateFinalScores();

        assertEquals(25, game.getFinalScores().get(player));
    }

    @Test
    void endBuilderPPEffectAddsBuilderPointsAtEndOfGame() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addCharacterCard(new BuilderCard(1, Era.I, 2, 3, 0)); // pointsEndOfMatch=3
        player.addBuildingCard(new BuildingCard(2, Era.I, 0, new EndBuilderPPEffect(), 0));

        Game game = buildGame(List.of(player));
        game.calculateFinalScores();

        // calculateBuilderPoints aggiunge 3, EndBuilderPPEffect aggiunge altri 3.
        assertEquals(6, game.getFinalScores().get(player));
    }

    @Test
    void endFullSetPPEffectAdds6PPPerFullSet() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new BuilderCard(2, Era.I, 2, 0, 0));
        player.addCharacterCard(new GathererCard(3, Era.I, 2));
        player.addCharacterCard(new HunterCard(4, Era.I, 2, false));
        player.addCharacterCard(new InventorCard(5, Era.I, 2, InventorIcons.INVENTION_1));
        player.addCharacterCard(new ShamanCard(6, Era.I, 2, 2)); // 1 set completo
        player.addBuildingCard(new BuildingCard(7, Era.I, 0, new EndFullSetPPEffect(), 0));

        Game game = buildGame(List.of(player));
        game.calculateFinalScores();

        assertTrue(game.getFinalScores().get(player) >= 6); // almeno 1 set × 6 PP
    }

    @Test
    void endCharacterCountPPEffectAddsPointsPerCard() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new ArtistCard(2, Era.I, 2));
        player.addCharacterCard(new ArtistCard(3, Era.I, 2)); // 3 artists → 2 PP each
        player.addBuildingCard(new BuildingCard(4, Era.I, 0,
                new EndCharacterCountPPEffect(2, ArtistCard.class), 0));

        Game game = buildGame(List.of(player));
        game.calculateFinalScores();

        assertTrue(game.getFinalScores().get(player) >= 6); // at least 3×2 = 6 PP from the effect
    }
}
