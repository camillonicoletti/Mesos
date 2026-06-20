package it.polimi.ingsw.controller;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Era transition tests for Round.checkEraTransition().
 *
 * Verifies that finding an Era II card in the upper row while in Era I
 * advances the game to Era II, that finding an Era III card while still in
 * Era I does not skip directly to Era III (the state machine advances one
 * era at a time), and that on the Era II to III transition the lower-row
 * buildings are discarded while the upper buildings shift to lower, per
 * rulebook section 13.
 */
class EraTransitionTest {

    @SuppressWarnings("unchecked")
    private <T> List<T> roundList(Round round, String field) {
        try {
            Field f = Round.class.getDeclaredField(field);
            f.setAccessible(true);
            return (List<T>) f.get(round);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private Game buildGame(List<Player> players, List<BuildingCard> era1, List<BuildingCard> era2, List<BuildingCard> era3) {
        Game g = new Game(
                players, 1, null, Era.I, null,
                BoardFactory.createBoard(2),
                new ArrayList<>(),
                era1, era2, era3
        );
        Round round = new Round(1, g, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>());
        g.setCurrentRound(round);
        for (Player p : players) p.setGame(g);
        return g;
    }

    @Test
    void cardOfEraIIInUpperWhileInEraI_advancesOnlyOneStep() {
        Player a = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        Player b = new Player("Bob",   new Totem(TotemColor.BLUE, "OFFER_TRACK"));
        BuildingCard era2Building = new BuildingCard(2001, Era.II, 0, null, 0);
        Game game = buildGame(List.of(a, b), new ArrayList<>(),
                new ArrayList<>(List.of(era2Building)), new ArrayList<>());
        Round round = game.getCurrentRound();

        CharacterCard eraIIChar = new HunterCard(2100, Era.II, 0, false);
        roundList(round, "upperRowTribute").add(eraIIChar);

        assertEquals(Era.I, game.getCurrentEra());
        round.checkEraTransition();

        // The game advanced from Era.I to Era.II — and only by one step.
        assertEquals(Era.II, game.getCurrentEra());
        // The Era II building reveal happened in the upper buildings row.
        assertEquals(1, round.getUpperRowBuildings().size());
        assertSame(era2Building, round.getUpperRowBuildings().get(0));
    }

    @Test
    void cardOfEraIIIInUpperWhileInEraI_doesNotSkipToEraIIIInOneCall() {
        // The current implementation only matches the (I→II) condition in this
        // scenario: a III card while in I never triggers a I→II OR II→III jump
        // directly. This test pins down the conservative behaviour.
        Player a = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        Player b = new Player("Bob",   new Totem(TotemColor.BLUE, "OFFER_TRACK"));
        Game game = buildGame(List.of(a, b),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        Round round = game.getCurrentRound();

        roundList(round, "upperRowTribute").add(new HunterCard(3100, Era.III, 0, false));

        round.checkEraTransition();
        assertEquals(Era.I, game.getCurrentEra(),
                "An Era III card without an Era II precursor must not advance Era I.");
    }

    @Test
    void eraIIToIII_discardsLowerBuildingsAndShiftsUpper() {
        Player a = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        Player b = new Player("Bob",   new Totem(TotemColor.BLUE, "OFFER_TRACK"));
        BuildingCard era3 = new BuildingCard(3001, Era.III, 0, null, 0);
        BuildingCard upperLeftover = new BuildingCard(2002, Era.II, 0, null, 0);
        BuildingCard lowerLeftover = new BuildingCard(1003, Era.I, 0, null, 0);
        Game game = buildGame(List.of(a, b),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(List.of(era3)));
        // Advance to Era II manually before testing II→III.
        game.nextEra();
        Round round = game.getCurrentRound();

        roundList(round, "upperRowBuildings").add(upperLeftover);
        roundList(round, "lowerRowBuildings").add(lowerLeftover);

        // Force the transition by placing an Era III character in upper after refill.
        roundList(round, "upperRowTribute").add(new HunterCard(3200, Era.III, 0, false));

        round.checkEraTransition();

        assertEquals(Era.III, game.getCurrentEra());
        // Lower buildings discarded (the old Era I leftover should be gone).
        assertEquals(0, round.getLowerRowBuildings().stream()
                .filter(b1 -> b1.getId() == 1003).count(),
                "Era I leftover in lower row must be discarded on II→III");
        // Upper Era II building shifted to lower; Era III reveal in upper.
        assertEquals(1, round.getLowerRowBuildings().size());
        assertSame(upperLeftover, round.getLowerRowBuildings().get(0));
        assertEquals(1, round.getUpperRowBuildings().size());
        assertSame(era3, round.getUpperRowBuildings().get(0));
    }
}
