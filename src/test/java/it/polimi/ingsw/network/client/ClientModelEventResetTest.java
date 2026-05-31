package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.protocol.clientbound.EventResolvedMessage;
import it.polimi.ingsw.network.protocol.clientbound.StateChangedMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the reset of the "last resolved event" snapshot
 * performed by {@link ClientModel#applyStateChanged(StateChangedMessage)}
 * when the new state is {@code EventResolutionState}.
 *
 * <p>Rationale: when the server enters a new event-resolution phase the
 * client must clear the previously shown event banner, otherwise the UI
 * would display the previous round's outcome on top of a fresh phase.
 * The reset must happen only for {@code EventResolutionState}: transitions
 * to other states must not wipe the cached event so the banner can keep
 * fading out across unrelated phase changes.
 */
class ClientModelEventResetTest {

    @Test
    void transitionToEventResolutionStateClearsCachedEvent() {
        ClientModel model = new ClientModel();

        // Populate the "last resolved event" fields the same way the server
        // would via EventResolvedMessage.
        model.applyEventResolved(new EventResolvedMessage(
                42, "Hunt", "A hunt event description",
                List.<EventResolvedMessage.PlayerEffect>of()));

        // Sanity: fields are populated before the state change.
        assertEquals(42, model.getLastResolvedEventCardId());
        assertEquals("Hunt", model.getLastResolvedEventTitle());
        assertEquals("A hunt event description", model.getLastResolvedEventDescription());
        assertNotNull(model.getLastResolvedEventEffects());

        model.applyStateChanged(new StateChangedMessage("EventResolutionState"));

        // The newly entered EventResolutionState must wipe the previous banner.
        assertEquals(-1, model.getLastResolvedEventCardId(),
                "Card id must be reset to the sentinel -1 when entering EventResolutionState");
        assertNull(model.getLastResolvedEventTitle(),
                "Title must be cleared when entering EventResolutionState");
        assertNull(model.getLastResolvedEventDescription(),
                "Description must be cleared when entering EventResolutionState");
        assertNotNull(model.getLastResolvedEventEffects(),
                "Effects list must remain non-null after reset");
        assertTrue(model.getLastResolvedEventEffects().isEmpty(),
                "Effects list must be empty (List.of()) after reset");
    }

    @Test
    void transitionToOtherStatesDoesNotClearCachedEvent() {
        ClientModel model = new ClientModel();
        model.applyEventResolved(new EventResolvedMessage(
                7, "Sustenance", "Pay food or lose PP",
                List.<EventResolvedMessage.PlayerEffect>of()));

        // The reset must be gated specifically on EventResolutionState. Any
        // other state must leave the cached event untouched, otherwise the
        // UI would lose the banner during unrelated transitions (e.g. an
        // intermediate ActionResolutionState before the next event phase).
        model.applyStateChanged(new StateChangedMessage("ActionResolutionState"));
        assertEquals(7, model.getLastResolvedEventCardId());
        assertEquals("Sustenance", model.getLastResolvedEventTitle());
        assertEquals("Pay food or lose PP", model.getLastResolvedEventDescription());

        model.applyStateChanged(new StateChangedMessage("TotemPlacementState"));
        assertEquals(7, model.getLastResolvedEventCardId());
        assertEquals("Sustenance", model.getLastResolvedEventTitle());

        model.applyStateChanged(new StateChangedMessage("EndRoundState"));
        assertEquals(7, model.getLastResolvedEventCardId());
        assertEquals("Sustenance", model.getLastResolvedEventTitle());
    }
}
