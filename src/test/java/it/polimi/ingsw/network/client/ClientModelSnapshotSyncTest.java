package it.polimi.ingsw.network.client;

import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.GameSnapshot;
import it.polimi.ingsw.network.protocol.clientbound.PlayerSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the ClientModel.applySnapshot(GameSnapshot) contract:
 * synchronization of ClientModel with GameSnapshot / PlayerSnapshot from the
 * clientbound protocol. These tests drive the model only through its public
 * applySnapshot API and assert on the read-only getters used by the GUI/TUI.
 */
class ClientModelSnapshotSyncTest {

    /**
     * Building a snapshot with all the fields needed by the tests in this
     * class. The constructor is invoked here only to keep each test compact;
     * the test itself never modifies the returned snapshot.
     */
    private static GameSnapshot snapshotOf(List<PlayerSnapshot> players,
                                           List<Integer> upperCharacterAndEventIds,
                                           List<Integer> lowerCharacterAndEventIds,
                                           List<Integer> upperBuildingIds,
                                           List<Integer> lowerBuildingIds,
                                           Era era,
                                           int roundNumber,
                                           List<Integer> upperEventIds,
                                           List<Integer> lowerEventIds,
                                           int remainingTribeDeckCards) {
        return new GameSnapshot(
                players,
                upperCharacterAndEventIds,
                lowerCharacterAndEventIds,
                upperBuildingIds,
                lowerBuildingIds,
                era,
                roundNumber,
                Map.of(),
                List.of(),
                List.of(),
                upperEventIds,
                lowerEventIds,
                remainingTribeDeckCards
        );
    }

    /**
     * After applying a snapshot, every player carried by the snapshot must
     * appear in getPlayers() as a PlayerView with food, prestige and card
     * ids matching the snapshot fields, including non-current opponents.
     */
    @Test
    void applySnapshotPopulatesAllVisiblePlayersWithFoodPrestigeAndCardIds() {
        ClientModel model = new ClientModel();
        PlayerSnapshot alice = new PlayerSnapshot(
                "Alice", TotemColor.RED, 4, 7, List.of(11, 12), List.of(21));
        PlayerSnapshot bob = new PlayerSnapshot(
                "Bob",   TotemColor.BLUE, 1, 3, List.of(13),     List.of());
        PlayerSnapshot carl = new PlayerSnapshot(
                "Carl",  TotemColor.PURPLE, 0, 0, List.of(),     List.of(22, 23));

        model.applySnapshot(snapshotOf(
                List.of(alice, bob, carl),
                List.of(), List.of(),
                List.of(), List.of(),
                Era.I, 1,
                List.of(), List.of(),
                30));

        Map<String, PlayerView> players = model.getPlayers();
        assertEquals(3, players.size(), "All three snapshot players must be visible.");

        PlayerView aliceView = players.get("Alice");
        assertEquals(TotemColor.RED, aliceView.getTotemColor());
        assertEquals(4, aliceView.getFood());
        assertEquals(7, aliceView.getPrestigePoints());
        assertEquals(List.of(11, 12), aliceView.getCharacterCardIds());
        assertEquals(List.of(21),     aliceView.getBuildingCardIds());

        PlayerView bobView = players.get("Bob");
        assertEquals(1, bobView.getFood());
        assertEquals(3, bobView.getPrestigePoints());
        assertEquals(List.of(13), bobView.getCharacterCardIds());
        assertTrue(bobView.getBuildingCardIds().isEmpty());

        PlayerView carlView = players.get("Carl");
        assertEquals(TotemColor.PURPLE, carlView.getTotemColor());
        assertEquals(List.of(22, 23), carlView.getBuildingCardIds());
    }

    /**
     * The two row id lists carried by GameSnapshot mix character and event
     * card ids. The client model receives the event ids separately and must
     * classify them via ClientModel.isEventCard(int); character ids in the
     * same lists must not be reported as events.
     */
    @Test
    void eventCardIdsFromSnapshotAreRecognizedAsEventsAndNotCharacters() {
        ClientModel model = new ClientModel();

        // Upper row: characters 101, 102 plus event 900.
        // Lower row: character 201 plus event 901.
        model.applySnapshot(snapshotOf(
                List.of(),
                List.of(101, 102, 900),
                List.of(201, 901),
                List.of(), List.of(),
                Era.I, 1,
                List.of(900),
                List.of(901),
                10));

        assertTrue(model.isEventCard(900),  "900 was sent as an upper-row event id.");
        assertTrue(model.isEventCard(901),  "901 was sent as a lower-row event id.");
        assertFalse(model.isEventCard(101), "101 is a character id, not an event.");
        assertFalse(model.isEventCard(102), "102 is a character id, not an event.");
        assertFalse(model.isEventCard(201), "201 is a character id, not an event.");
        assertFalse(model.isEventCard(9999), "Unknown ids must not be reported as events.");

        // The row lists themselves still carry both character and event ids,
        // because the GUI renders them in display order.
        assertEquals(List.of(101, 102, 900), model.getUpperRowCharacterIds());
        assertEquals(List.of(201, 901),      model.getLowerRowCharacterIds());
    }

    /**
     * Two consecutive applySnapshot calls must replace stale player and row
     * state, not accumulate it: a removed player must disappear, and rows
     * must reflect only the latest snapshot's ids.
     */
    @Test
    void consecutiveSnapshotsReplaceStalePlayerAndRowDataInsteadOfAccumulating() {
        ClientModel model = new ClientModel();

        // First snapshot: 3 players, two upper-row characters, one upper building.
        model.applySnapshot(snapshotOf(
                List.of(
                        new PlayerSnapshot("Alice", TotemColor.RED,    5, 0, List.of(1), List.of()),
                        new PlayerSnapshot("Bob",   TotemColor.BLUE,   2, 0, List.of(2), List.of()),
                        new PlayerSnapshot("Carl",  TotemColor.PURPLE, 0, 0, List.of(3), List.of())
                ),
                List.of(101, 102), List.of(),
                List.of(301),      List.of(),
                Era.I, 1,
                List.of(),         List.of(),
                30));

        assertEquals(3, model.getPlayers().size());
        assertEquals(List.of(101, 102), model.getUpperRowCharacterIds());
        assertEquals(List.of(301),      model.getUpperRowBuildingIds());

        // Second snapshot: Carl is gone, rows are different.
        model.applySnapshot(snapshotOf(
                List.of(
                        new PlayerSnapshot("Alice", TotemColor.RED,  6, 1, List.of(1, 4), List.of()),
                        new PlayerSnapshot("Bob",   TotemColor.BLUE, 2, 0, List.of(2),    List.of())
                ),
                List.of(105), List.of(106),
                List.of(),    List.of(),
                Era.II, 2,
                List.of(),    List.of(),
                28));

        Map<String, PlayerView> players = model.getPlayers();
        assertEquals(2, players.size(), "Carl must be removed from the visible players.");
        assertNull(players.get("Carl"),  "Carl must no longer be retrievable.");

        // Alice is updated, not duplicated.
        PlayerView aliceView = players.get("Alice");
        assertEquals(6, aliceView.getFood(),            "Alice's food must reflect the latest snapshot.");
        assertEquals(1, aliceView.getPrestigePoints(),  "Alice's PP must reflect the latest snapshot.");
        assertEquals(List.of(1, 4), aliceView.getCharacterCardIds(),
                "Alice's card list must replace, not extend, the previous one.");

        // Rows must contain only the latest ids: no stale 101/102/301.
        assertEquals(List.of(105), model.getUpperRowCharacterIds());
        assertEquals(List.of(106), model.getLowerRowCharacterIds());
        assertTrue(model.getUpperRowBuildingIds().isEmpty(),
                "Previous upper building 301 must not survive the second snapshot.");
    }

    /**
     * The global fields exposed by ClientModel (roundNumber, currentEra,
     * remainingTribeDeckCards) must reflect the latest snapshot applied.
     */
    @Test
    void applySnapshotUpdatesGlobalRoundEraAndRemainingDeckFields() {
        ClientModel model = new ClientModel();

        model.applySnapshot(snapshotOf(
                List.of(), List.of(), List.of(), List.of(), List.of(),
                Era.I, 1,
                List.of(), List.of(),
                42));

        assertEquals(Era.I, model.getCurrentEra());
        assertEquals(1,     model.getRoundNumber());
        assertEquals(42,    model.getRemainingTribeDeckCards());

        // A later snapshot from a different era must overwrite all three fields.
        model.applySnapshot(snapshotOf(
                List.of(), List.of(), List.of(), List.of(), List.of(),
                Era.III, 7,
                List.of(), List.of(),
                5));

        assertEquals(Era.III, model.getCurrentEra(),
                "Era must follow the latest snapshot.");
        assertEquals(7, model.getRoundNumber(),
                "Round number must follow the latest snapshot.");
        assertEquals(5, model.getRemainingTribeDeckCards(),
                "Remaining tribe deck size must follow the latest snapshot.");
    }
}
