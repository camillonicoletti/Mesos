package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Bug hunter tests for model encapsulation.
 * These tests verify that external code cannot corrupt core model collections directly.
 */
class ModelEncapsulationBugHunterTest {

    /**
     * External code must not be able to clear the game's player list through getPlayers().
     */
    @Test
    void getPlayersDoesNotExposeMutableInternalList() {
        Player alice = new Player("Alice", new Totem(TotemColor.RED, "TURN_ORDER"));
        Player bob = new Player("Bob", new Totem(TotemColor.BLUE, "TURN_ORDER"));

        Game game = new Game(
                new ArrayList<>(List.of(alice, bob)),
                1,
                null,
                Era.I,
                LocalDateTime.now(),
                null,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );

        List<Player> exposedPlayers = game.getPlayers();

        assertThrows(UnsupportedOperationException.class, exposedPlayers::clear);
        assertEquals(2, game.getPlayers().size());
    }

    /**
     * External code must not be able to clear a player's character cards through getCharacterCards().
     */
    @Test
    void getCharacterCardsDoesNotExposeMutableInternalList() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "TURN_ORDER"));

        HunterCard hunter = new HunterCard(1, Era.I, 2, false);
        player.addCharacterCard(hunter);

        List<?> exposedCharacters = player.getTribe().getCharacterCards();

        assertThrows(UnsupportedOperationException.class, exposedCharacters::clear);
        assertEquals(1, player.getTribe().countCharacterCards());
    }

    /**
     * External code must not be able to clear a player's building cards through getBuildingCards().
     */
    @Test
    void getBuildingCardsDoesNotExposeMutableInternalList() {
        Player player = new Player("Alice", new Totem(TotemColor.RED, "TURN_ORDER"));

        BuildingCard building = new BuildingCard(100, Era.I, 2, null, 5);
        player.addBuildingCard(building);

        List<?> exposedBuildings = player.getTribe().getBuildingCards();

        assertThrows(UnsupportedOperationException.class, exposedBuildings::clear);
        assertEquals(1, player.getTribe().getBuildingCards().size());
    }
}