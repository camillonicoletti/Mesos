package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.effect.EndBuilderPPEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bug-hunter: building with {@link EndBuilderPPEffect} (rulebook building #9)
 * must double the Builder end-of-match PP. The doubling is implemented by
 * adding a second copy of the builder PP via the building's EndGameEffect,
 * on top of the base sum computed by {@code calculateBuilderPoints}.
 *
 * <p>These tests pin down:
 * <ul>
 *   <li>Without the building: builder PP counted once.</li>
 *   <li>With one copy of the building: builder PP counted twice.</li>
 *   <li>With two copies: builder PP counted three times (additive effects).</li>
 *   <li>Final score is idempotent across repeated {@code calculateFinalScores} calls.</li>
 * </ul>
 *
 * <p>If the project intentionally wants to cap the doubling at "x2 even with
 * multiple copies", the third test will start failing and should be revisited.
 */
class FinalScoringDoubleBuilderPrestigeTest {

    private static final int BUILDER_PP = 5;

    private Game buildGame(List<Player> players) {
        return new Game(players, 1, null, Era.I, null, null,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    private Player playerWithTwoBuilders() {
        Player p = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        p.addCharacterCard(new BuilderCard(1, Era.I, 2, BUILDER_PP, 0));
        p.addCharacterCard(new BuilderCard(2, Era.I, 2, BUILDER_PP, 0));
        return p;
    }

    @Test
    void withoutEndBuilderPPBuilding_builderPPCountedOnce() {
        Player p = playerWithTwoBuilders();
        Game game = buildGame(List.of(p));

        int score = game.getFinalScores().get(p);
        // base PP=0, no inventor, no artist, builder=2*5=10, no buildings
        assertEquals(2 * BUILDER_PP, score);
    }

    @Test
    void withOneEndBuilderPPBuilding_builderPPCountedTwice() {
        Player p = playerWithTwoBuilders();
        p.addBuildingCard(new BuildingCard(900, Era.I, 0, new EndBuilderPPEffect(), 0));

        Game game = buildGame(List.of(p));
        int score = game.getFinalScores().get(p);

        // base builder PP (10) + EndBuilderPPEffect contribution (10) = 20
        assertEquals(2 * 2 * BUILDER_PP, score);
    }

    @Test
    void withTwoEndBuilderPPBuildings_builderPPCountedThreeTimes() {
        Player p = playerWithTwoBuilders();
        p.addBuildingCard(new BuildingCard(900, Era.I, 0, new EndBuilderPPEffect(), 0));
        p.addBuildingCard(new BuildingCard(901, Era.I, 0, new EndBuilderPPEffect(), 0));

        Game game = buildGame(List.of(p));
        int score = game.getFinalScores().get(p);

        // Current implementation: each copy of the effect adds full builder PP.
        // Base (10) + Effect#1 (10) + Effect#2 (10) = 30.
        assertEquals(3 * 2 * BUILDER_PP, score);
    }

    @Test
    void calculateFinalScores_isIdempotentAcrossRepeatedCalls() {
        Player p = playerWithTwoBuilders();
        p.addBuildingCard(new BuildingCard(900, Era.I, 0, new EndBuilderPPEffect(), 0));
        Game game = buildGame(List.of(p));

        game.end();           // sets gameEnded=true and caches final scores
        // Mutating the tribe after end() must not change the cached final score.
        p.addCharacterCard(new BuilderCard(3, Era.I, 2, BUILDER_PP, 0));
        game.calculateFinalScores();  // guarded by finalScoresCalculated; must be no-op

        Map<Player, Integer> scores = game.getFinalScores();
        assertEquals(2 * 2 * BUILDER_PP, scores.get(p));
    }
}
