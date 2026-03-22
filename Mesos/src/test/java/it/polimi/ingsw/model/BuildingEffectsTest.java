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
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                GameStatus.IN_PROGRESS);
    }

    private Game buildGameWithBoard(List<Player> players, Board board) {
        return new Game(players, 1, null, Era.I, null, board,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                GameStatus.IN_PROGRESS);
    }

    // -------------------------------------------------------
    // FullSetFoodEffect
    // triggered in: Player.addCharacterCard()
    // -------------------------------------------------------

    @Test
    void fullSetFoodEffectGivesFoodWhenSetCompleted() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player.addBuildingCard(new BuildingCard(1, 0, new FullSetFoodEffect(), Era.I, 0, 2));

        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new BuilderCard(2, 1, 1, Era.I, 2));
        player.addCharacterCard(new GathererCard(3, Era.I, 2));
        player.addCharacterCard(new HunterCard(4, false, Era.I, 2));
        player.addCharacterCard(new InventorCard(5, InventorIcons.INVENTION_1, Era.I, 2));
        player.addCharacterCard(new ShamanCard(6, Era.I, 1, 2)); // completes set

        assertEquals(5, player.getFood()); // +5 per set completato
    }

    @Test
    void fullSetFoodEffectDoesNotTriggerWithoutBuilding() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));

        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new BuilderCard(2, 1, 1, Era.I, 2));
        player.addCharacterCard(new GathererCard(3, Era.I, 2));
        player.addCharacterCard(new HunterCard(4, false, Era.I, 2));
        player.addCharacterCard(new InventorCard(5, InventorIcons.INVENTION_1, Era.I, 2));
        player.addCharacterCard(new ShamanCard(6, Era.I, 1, 2));

        assertEquals(0, player.getFood());
    }

    // -------------------------------------------------------
    // InventorPairFoodEffect
    // triggered in: Player.addCharacterCard()
    // -------------------------------------------------------

    @Test
    void inventorPairFoodEffectGivesFoodWhenPairFormed() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player.addBuildingCard(new BuildingCard(1, 0, new InventorPairFoodEffect(), Era.I, 0, 2));

        player.addCharacterCard(new InventorCard(1, InventorIcons.INVENTION_1, Era.I, 2));
        player.addCharacterCard(new InventorCard(2, InventorIcons.INVENTION_1, Era.I, 2)); // coppia!

        assertEquals(3, player.getFood()); // +3 per coppia
    }

    @Test
    void inventorPairFoodEffectDoesNotTriggerWithDifferentIcons() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player.addBuildingCard(new BuildingCard(1, 0, new InventorPairFoodEffect(), Era.I, 0, 2));

        player.addCharacterCard(new InventorCard(1, InventorIcons.INVENTION_1, Era.I, 2));
        player.addCharacterCard(new InventorCard(2, InventorIcons.INVENTION_2, Era.I, 2)); // icone diverse

        assertEquals(0, player.getFood());
    }

    // -------------------------------------------------------
    // TotemSlotFoodBonusEffect
    // triggered in: Round.placeTotemOrderForPlayer()
    // -------------------------------------------------------

    @Test
    void totemSlotFoodBonusEffectGivesFoodOnGainFoodSlot() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player.addBuildingCard(new BuildingCard(1, 0, new TotemSlotFoodBonusEffect(), Era.I, 0, 2));

        List<TurnSlot> slots = new ArrayList<>();
        slots.add(new TurnSlot(1, SlotEffect.GAIN_FOOD, 2, 0));
        slots.add(new TurnSlot(2, SlotEffect.NONE, 0, 0));

        Board board = new Board(new ArrayList<>(), new TurnOrderTile(2, slots));
        Game game = buildGameWithBoard(List.of(player), board);

        Round round = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        game.setCurrentRound(round);

        round.placeTotemOrderForPlayer(player); // slot 1 → GAIN_FOOD 2 + 1 bonus edificio

        assertEquals(3, player.getFood()); // 2 base + 1 bonus
    }

    @Test
    void totemSlotFoodBonusEffectDoesNotTriggerWithoutBuilding() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));

        List<TurnSlot> slots = new ArrayList<>();
        slots.add(new TurnSlot(1, SlotEffect.GAIN_FOOD, 2, 0));
        slots.add(new TurnSlot(2, SlotEffect.NONE, 0, 0));

        Board board = new Board(new ArrayList<>(), new TurnOrderTile(2, slots));
        Game game = buildGameWithBoard(List.of(player), board);

        Round round = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>()
        );
        game.setCurrentRound(round);

        round.placeTotemOrderForPlayer(player);

        assertEquals(2, player.getFood()); // solo base, nessun bonus
    }

    // -------------------------------------------------------
    // Hunt1Food1PPEffect
    // triggered in: HuntEventCard.resolve()
    // -------------------------------------------------------

    @Test
    void hunt1Food1PPEffectAddsExtraFoodAndPPPerHunter() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player.addCharacterCard(new HunterCard(1, true, Era.I, 2));
        player.addCharacterCard(new HunterCard(2, true, Era.I, 2));
        player.addBuildingCard(new BuildingCard(1, 0, new Hunt1Food1PPEffect(), Era.I, 0, 2));

        Game game = buildGame(List.of(player));
        new HuntEventCard(1, Era.I, false, 1, 2, 2).resolve(game);

        // base: 2×1 food + 2×2 PP = 2 food + 4 PP
        // bonus: 2×1 food + 2×1 PP = 2 food + 2 PP
        assertEquals(4, player.getFood());
        assertEquals(6, player.getPrestigePoints());
    }

    @Test
    void hunt1Food1PPEffectDoesNotTriggerWithoutBuilding() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player.addCharacterCard(new HunterCard(1, true, Era.I, 2));
        player.addCharacterCard(new HunterCard(2, true, Era.I, 2));

        Game game = buildGame(List.of(player));
        new HuntEventCard(1, Era.I, false, 1, 2, 2).resolve(game);

        assertEquals(2, player.getFood());
        assertEquals(4, player.getPrestigePoints());
    }

    // -------------------------------------------------------
    // CavePaintingsFoodEffect
    // triggered in: CavePaintingsEventCard.resolve()
    // -------------------------------------------------------

    @Test
    void cavePaintingsFoodEffectAddsOneFoodPerArtist() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new ArtistCard(2, Era.I, 2));
        player.addCharacterCard(new ArtistCard(3, Era.I, 2));
        player.addBuildingCard(new BuildingCard(1, 0, new CavePaintingsFoodEffect(), Era.I, 0, 2));

        Game game = buildGame(List.of(player));
        // minimumArtists=2, ppLostIfBelow=3, ppPerArtist=1
        new CavePaintingsEventCard(1, Era.I, false, 2, 3, 1, 2).resolve(game);

        // base: 3×1 PP = 3 PP
        // bonus edificio: 3×1 food = 3 food
        assertEquals(3, player.getFood());
        assertEquals(3, player.getPrestigePoints());
    }

    @Test
    void cavePaintingsFoodEffectDoesNotTriggerWhenBelowMinimum() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player.addCharacterCard(new ArtistCard(1, Era.I, 2)); // 1, minimo è 2
        player.addBuildingCard(new BuildingCard(1, 0, new CavePaintingsFoodEffect(), Era.I, 0, 2));

        Game game = buildGame(List.of(player));
        new CavePaintingsEventCard(1, Era.I, false, 2, 3, 1, 2).resolve(game);

        assertEquals(0, player.getFood());
        assertEquals(-3, player.getPrestigePoints());
    }

    // -------------------------------------------------------
    // SustenanceFoodDiscountEffect
    // triggered in: SustenanceEventCard.resolve()
    // -------------------------------------------------------

    @Test
    void sustenanceFoodDiscountEffectReducesFoodRequired() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player.addFood(10);

        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new ArtistCard(2, Era.I, 2));
        player.addCharacterCard(new ArtistCard(3, Era.I, 2));
        player.addCharacterCard(new ArtistCard(4, Era.I, 2)); // 4 personaggi

        // sconto 1 per ogni Artista → sconto totale 4 → cibo richiesto 0
        player.addBuildingCard(new BuildingCard(1, 0,
                new SustenanceFoodDiscountEffect(ArtistCard.class), Era.I, 0, 2));

        Game game = buildGame(List.of(player));
        new SustenanceEventCard(1, Era.I, false, 2, 2).resolve(game);

        assertEquals(10, player.getFood()); // non paga nulla
        assertEquals(0, player.getPrestigePoints());
    }

    @Test
    void sustenanceFoodDiscountEffectDoesNotTriggerWithoutBuilding() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player.addFood(2);

        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new ArtistCard(2, Era.I, 2));
        player.addCharacterCard(new ArtistCard(3, Era.I, 2));
        player.addCharacterCard(new ArtistCard(4, Era.I, 2));

        Game game = buildGame(List.of(player));
        new SustenanceEventCard(1, Era.I, false, 2, 2).resolve(game);

        // 4 richiesti, 2 disponibili → paga 2 cibo + 2 PP per mancante
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
        Player p1 = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        Player p2 = new Player("Bob", new Totem(TotemColor.BLUE, TotemPosition.OFFER_TRACK));

        p1.addBuildingCard(new BuildingCard(1, 0, new Shaman3ExtraStarsEffect(), Era.I, 0, 2));
        p2.addCharacterCard(new ShamanCard(1, Era.I, 1, 2));

        Game game = buildGame(List.of(p1, p2));
        new ShamanicRitualEventCard(1, Era.I, false, 4, 2, 2).resolve(game);

        assertEquals(4, p1.getPrestigePoints());
        assertEquals(-2, p2.getPrestigePoints());
    }

    @Test
    void shamanicRitualDoublePPEffectDoublesPPForWinner() {
        Player p1 = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        Player p2 = new Player("Bob", new Totem(TotemColor.BLUE, TotemPosition.OFFER_TRACK));

        p1.addCharacterCard(new ShamanCard(1, Era.I, 3, 2));
        p1.addBuildingCard(new BuildingCard(2, 0, new ShamanicRitualDoublePPEffect(), Era.I, 0, 2));
        p2.addCharacterCard(new ShamanCard(2, Era.I, 1, 2));

        Game game = buildGame(List.of(p1, p2));
        new ShamanicRitualEventCard(1, Era.I, false, 4, 2, 2).resolve(game);

        assertEquals(8, p1.getPrestigePoints()); // 4 × 2
        assertEquals(-2, p2.getPrestigePoints());
    }

    @Test
    void shamanicRitualImmunityEffectProtectsLoser() {
        Player p1 = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        Player p2 = new Player("Bob", new Totem(TotemColor.BLUE, TotemPosition.OFFER_TRACK));

        p1.addCharacterCard(new ShamanCard(1, Era.I, 3, 2));
        p2.addBuildingCard(new BuildingCard(2, 0, new ShamanicRitualImmunityEffect(), Era.I, 0, 2));

        Game game = buildGame(List.of(p1, p2));
        new ShamanicRitualEventCard(1, Era.I, false, 4, 2, 2).resolve(game);

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
        Player player = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player.addBuildingCard(new BuildingCard(1, 0, new EndPPEffect(), Era.I, 0, 2));

        Game game = buildGame(List.of(player));
        game.calculateFinalScores();

        assertEquals(25, game.getFinalScores().get(player));
    }

    @Test
    void endBuilderPPEffectDoublesBuilderPoints() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player.addCharacterCard(new BuilderCard(1, 3, 0, Era.I, 2)); // pointsEndOfMatch=3
        player.addBuildingCard(new BuildingCard(2, 0, new EndBuilderPPEffect(), Era.I, 0, 2));

        Game game = buildGame(List.of(player));
        game.calculateFinalScores();

        // calculateBuilderPoints aggiunge 3, EndBuilderPPEffect aggiunge altri 3 → totale 6
        assertEquals(6, game.getFinalScores().get(player));
    }

    @Test
    void endFullSetPPEffectAdds6PPPerFullSet() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new BuilderCard(2, 0, 0, Era.I, 2));
        player.addCharacterCard(new GathererCard(3, Era.I, 2));
        player.addCharacterCard(new HunterCard(4, false, Era.I, 2));
        player.addCharacterCard(new InventorCard(5, InventorIcons.INVENTION_1, Era.I, 2));
        player.addCharacterCard(new ShamanCard(6, Era.I, 1, 2)); // 1 set completo
        player.addBuildingCard(new BuildingCard(7, 0, new EndFullSetPPEffect(), Era.I, 0, 2));

        Game game = buildGame(List.of(player));
        game.calculateFinalScores();

        assertTrue(game.getFinalScores().get(player) >= 6); // almeno 6 PP dal set
    }

    @Test
    void endCharacterCountPPEffectAddsPointsPerCard() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, TotemPosition.OFFER_TRACK));
        player.addCharacterCard(new ArtistCard(1, Era.I, 2));
        player.addCharacterCard(new ArtistCard(2, Era.I, 2));
        player.addCharacterCard(new ArtistCard(3, Era.I, 2)); // 3 artisti
        player.addBuildingCard(new BuildingCard(4, 0,
                new EndCharacterCountPPEffect(2, ArtistCard.class), Era.I, 0, 2));

        Game game = buildGame(List.of(player));
        game.calculateFinalScores();

        assertTrue(game.getFinalScores().get(player) >= 6); // almeno 3×2 = 6 PP
    }
}
