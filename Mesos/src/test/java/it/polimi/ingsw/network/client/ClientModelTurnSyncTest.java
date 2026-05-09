package it.polimi.ingsw.network.client;

import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.CurrentPlayerChangedMessage;
import it.polimi.ingsw.network.protocol.clientbound.GameSnapshot;
import it.polimi.ingsw.network.protocol.clientbound.PlayerDisconnectedMessage;
import it.polimi.ingsw.network.protocol.clientbound.PlayerReconnectedMessage;
import it.polimi.ingsw.network.protocol.clientbound.PlayerSnapshot;
import it.polimi.ingsw.network.protocol.clientbound.StateChangedMessage;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientModelTurnSyncTest {

    @Test
    void totemPlacementDisconnectClearsStaleCurrentPlayerAcrossReconnect() {
        ClientModel model = new ClientModel();

        model.applyStateChanged(new StateChangedMessage("TotemPlacementState"));
        model.applyCurrentPlayerChanged(new CurrentPlayerChangedMessage("Alice"));
        model.applyPlayerDisconnected(new PlayerDisconnectedMessage("Alice"));
        model.applyPlayerReconnected(new PlayerReconnectedMessage("Alice"));

        assertNull(model.getCurrentTotemPlacementPlayer());
    }

    @Test
    void gameSnapshotCarriesDisconnectedPlayersIntoClientModel() {
        ClientModel model = new ClientModel();

        model.applyStateChanged(new StateChangedMessage("TotemPlacementState"));
        model.applySnapshot(new GameSnapshot(
                List.of(
                        player("Alice", TotemColor.RED),
                        player("Bob", TotemColor.BLUE)
                ),
                List.of(), List.of(),
                List.of(), List.of(),
                Era.I,
                1,
                new HashMap<>(),
                List.of("Alice", "Bob"),
                List.of("Alice")
        ));

        assertTrue(model.isPlayerDisconnected("Alice"));
        assertFalse(model.isPlayerDisconnected("Bob"));
        assertEquals("Bob", model.getCurrentTotemPlacementPlayer());
    }

    private static PlayerSnapshot player(String nickname, TotemColor color) {
        return new PlayerSnapshot(nickname, color, 0, 0, List.of(), List.of());
    }
}
