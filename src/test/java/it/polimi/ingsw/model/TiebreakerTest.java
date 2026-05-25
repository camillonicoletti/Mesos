package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the tiebreaker chain on the server side (Game.getWinners):
 * 1. equal PP → winner is the player with more food;
 * 2. equal PP and equal food → shared victory.
 */
class TiebreakerTest {

    private Game makeGame(List<Player> players) {
        return new Game(players, 1, null, Era.I, LocalDateTime.now(),
                BoardFactory.createBoard(players.size()),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    private Player player(String name, TotemColor color) {
        return new Player(name, new Totem(color, "OFFER"));
    }

    /** Equal PP, different food: the player with more food wins. */
    @Test
    void equalPP_moreFood_winsBreaker() {
        Player alice = player("Alice", TotemColor.RED);
        Player bob   = player("Bob",   TotemColor.BLUE);

        alice.addPrestigePoints(10);
        bob.addPrestigePoints(10);

        alice.addFood(5);
        bob.addFood(3);

        Game game = makeGame(List.of(alice, bob));
        game.end();

        List<Player> winners = game.getWinners();
        assertEquals(1, winners.size(), "Exactly one winner expected when food breaks the tie");
        assertEquals("Alice", winners.get(0).getNickname());
    }

    /** Equal PP and equal food: both players share the victory. */
    @Test
    void equalPP_equalFood_sharedVictory() {
        Player alice = player("Alice", TotemColor.RED);
        Player bob   = player("Bob",   TotemColor.BLUE);

        alice.addPrestigePoints(10);
        bob.addPrestigePoints(10);

        alice.addFood(5);
        bob.addFood(5);

        Game game = makeGame(List.of(alice, bob));
        game.end();

        List<Player> winners = game.getWinners();
        assertEquals(2, winners.size(), "Both players should share victory when PP and food are equal");
        assertTrue(winners.stream().anyMatch(p -> p.getNickname().equals("Alice")));
        assertTrue(winners.stream().anyMatch(p -> p.getNickname().equals("Bob")));
    }
}
