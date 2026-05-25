package it.polimi.ingsw.network.protocol.clientbound;

import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Bug hunter tests for client-bound snapshots and messages.
 * DTOs sent to clients must not expose mutable internal collections.
 */
class SnapshotImmutabilityRegressionTest {

    /**
     * GameSnapshot must not expose its players list as a mutable internal collection.
     */
    @Test
    void gameSnapshotPlayersAreNotExternallyMutable() {
        GameSnapshot snapshot = createGameSnapshot();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.getPlayers().clear());
        assertEquals(1, snapshot.getPlayers().size());
    }

    /**
     * GameSnapshot must not expose upper row character/event ids as a mutable internal collection.
     */
    @Test
    void gameSnapshotUpperRowCharacterAndEventIdsAreNotExternallyMutable() {
        GameSnapshot snapshot = createGameSnapshot();

        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.getUpperRowCharacterAndEventIds().clear());

        assertEquals(2, snapshot.getUpperRowCharacterAndEventIds().size());
    }

    /**
     * GameSnapshot must not expose lower row character/event ids as a mutable internal collection.
     */
    @Test
    void gameSnapshotLowerRowCharacterAndEventIdsAreNotExternallyMutable() {
        GameSnapshot snapshot = createGameSnapshot();

        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.getLowerRowCharacterAndEventIds().clear());

        assertEquals(2, snapshot.getLowerRowCharacterAndEventIds().size());
    }

    /**
     * GameSnapshot must not expose upper row building ids as a mutable internal collection.
     */
    @Test
    void gameSnapshotUpperRowBuildingIdsAreNotExternallyMutable() {
        GameSnapshot snapshot = createGameSnapshot();

        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.getUpperRowBuildingIds().clear());

        assertEquals(1, snapshot.getUpperRowBuildingIds().size());
    }

    /**
     * GameSnapshot must not expose lower row building ids as a mutable internal collection.
     */
    @Test
    void gameSnapshotLowerRowBuildingIdsAreNotExternallyMutable() {
        GameSnapshot snapshot = createGameSnapshot();

        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.getLowerRowBuildingIds().clear());

        assertEquals(1, snapshot.getLowerRowBuildingIds().size());
    }

    /**
     * GameSnapshot must not expose its turn order as a mutable internal collection.
     */
    @Test
    void gameSnapshotTurnOrderIsNotExternallyMutable() {
        GameSnapshot snapshot = createGameSnapshot();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.getTurnOrder().clear());
        assertEquals(2, snapshot.getTurnOrder().size());
    }

    /**
     * GameSnapshot must not expose its totem map as a mutable internal collection.
     */
    @Test
    void gameSnapshotTotemMapIsNotExternallyMutable() {
        GameSnapshot snapshot = createGameSnapshot();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.getTotemOnTiles().clear());
        assertEquals(1, snapshot.getTotemOnTiles().size());
    }

    /**
     * PlayerSnapshot must not expose character card ids as a mutable internal collection.
     */
    @Test
    void playerSnapshotCharacterCardIdsAreNotExternallyMutable() {
        PlayerSnapshot player = createPlayerSnapshot();

        assertThrows(UnsupportedOperationException.class, () -> player.getCharacterCardIds().clear());
        assertEquals(2, player.getCharacterCardIds().size());
    }

    /**
     * PlayerSnapshot must not expose building card ids as a mutable internal collection.
     */
    @Test
    void playerSnapshotBuildingCardIdsAreNotExternallyMutable() {
        PlayerSnapshot player = createPlayerSnapshot();

        assertThrows(UnsupportedOperationException.class, () -> player.getBuildingCardIds().clear());
        assertEquals(1, player.getBuildingCardIds().size());
    }

    /**
     * LobbyUpdateMessage must not expose its players list as a mutable internal collection.
     */
    @Test
    void lobbyUpdatePlayersAreNotExternallyMutable() {
        LobbyUpdateMessage message = new LobbyUpdateMessage(
                new ArrayList<>(List.of(new LobbyPlayerSnapshot(
                        "Alice",
                        TotemColor.RED,
                        true,
                        true
                ))),
                2
        );

        assertThrows(UnsupportedOperationException.class, () -> message.getPlayers().clear());
        assertEquals(1, message.getPlayers().size());
    }

    private GameSnapshot createGameSnapshot() {
        Map<Character, String> totemOnTiles = new HashMap<>();
        totemOnTiles.put('A', "Alice");

        return new GameSnapshot(
                new ArrayList<>(List.of(createPlayerSnapshot())),
                new ArrayList<>(List.of(10, 11)),
                new ArrayList<>(List.of(12, 13)),
                new ArrayList<>(List.of(100)),
                new ArrayList<>(List.of(101)),
                Era.I,
                1,
                totemOnTiles,
                new ArrayList<>(List.of("Alice", "Bob"))
        );
    }

    private PlayerSnapshot createPlayerSnapshot() {
        return new PlayerSnapshot(
                "Alice",
                TotemColor.RED,
                3,
                5,
                new ArrayList<>(List.of(1, 2)),
                new ArrayList<>(List.of(100))
        );
    }
}