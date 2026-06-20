package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Encapsulation tests focused on Game getters. Verifies that getters exposing
 * internal collections (tribute deck, building decks, current round views)
 * return defensive copies or unmodifiable views: external code must not be
 * able to mutate the game-internal state by writing to the returned
 * collection.
 */
class GameAndClientMutabilityTest {

    private Game minimalGame() {
        List<Player> players = List.of(
                new Player("A", new Totem(TotemColor.RED,  "OFFER")),
                new Player("B", new Totem(TotemColor.BLUE, "OFFER"))
        );
        List<Card> tribute = new ArrayList<>();
        tribute.add(new HunterCard(900, Era.I, 0, false));
        tribute.add(new HunterCard(901, Era.I, 0, false));
        return new Game(players, 1, null, Era.I, LocalDateTime.now(),
                BoardFactory.createBoard(2),
                tribute,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Game.getPlayers() returns an unmodifiable view — modifying it must
     * throw. This is a regression guard.
     */
    @Test
    void getPlayersIsUnmodifiable() {
        Game game = minimalGame();
        List<Player> players = game.getPlayers();
        assertThrows(UnsupportedOperationException.class,
                () -> players.add(new Player("X", new Totem(TotemColor.PURPLE, "OFFER"))),
                "Game.getPlayers() must return an unmodifiable list");
    }

    /**
     * Game.getTributeDeck() returns an unmodifiable view, preventing external
     * code from draining the internal deck — regression guard.
     */
    @Test
    void getTributeDeckMustBeUnmodifiable() {
        Game game = minimalGame();
        List<Card> exposed = game.getTributeDeck();

        assertThrows(UnsupportedOperationException.class, exposed::clear,
                "Game.getTributeDeck() exposes the live internal list. " +
                "External callers can drain it, breaking the snapshot of " +
                "remaining cards.  Recommended fix: return Collections.unmodifiableList " +
                "or List.copyOf.");
    }

    /**
     * Round.getUpperRowTribute() and friends already return unmodifiable
     * views — guard against regression.
     */
    @Test
    void roundCardListGettersAreUnmodifiable() {
        Round round = new Round(1, minimalGame(), Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>());

        assertThrows(UnsupportedOperationException.class,
                () -> round.getUpperRowTribute().add(new HunterCard(99, Era.I, 0, false)));
        assertThrows(UnsupportedOperationException.class,
                () -> round.getLowerRowTribute().add(new HunterCard(98, Era.I, 0, false)));
        assertThrows(UnsupportedOperationException.class,
                () -> round.getUpperRowEvents().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> round.getLowerRowEvents().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> round.getUpperRowBuildings().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> round.getLowerRowBuildings().clear());
    }

    /**
     * Tribe.getCharacterCards() and Tribe.getBuildingCards() must not allow
     * direct mutation of the underlying collection — guard against
     * regression.
     */
    @Test
    void tribeCardListGettersAreUnmodifiable() {
        Tribe tribe = new Tribe();

        assertThrows(UnsupportedOperationException.class,
                () -> tribe.getCharacterCards().add(new HunterCard(1, Era.I, 0, false)));
        assertThrows(UnsupportedOperationException.class,
                () -> tribe.getBuildingCards().add(
                        new BuildingCard(1, Era.I, 0, null, 0)));
    }
}
