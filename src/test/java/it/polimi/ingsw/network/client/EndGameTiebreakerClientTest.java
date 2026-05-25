package it.polimi.ingsw.network.client;

import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.GameSnapshot;
import it.polimi.ingsw.network.protocol.clientbound.GameEndedMessage;
import it.polimi.ingsw.network.protocol.clientbound.GameSnapshot;
import it.polimi.ingsw.network.protocol.clientbound.PlayerSnapshot;
import it.polimi.ingsw.view.EndGameHelper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that EndGameHelper.resolveWinners uses the server-authoritative
 * finalFood from GameEndedMessage, not the stale food cached in PlayerView.
 *
 * Scenario: both players have equal PP. PlayerView has stale food values that
 * would produce the wrong winner. finalFood in the message carries the correct
 * values. The tiebreaker must use finalFood.
 */
class EndGameTiebreakerClientTest {

    private ClientModel modelWithStaleFoodAndFinalScores(
            String nickA, int staleFoodA,
            String nickB, int staleFoodB,
            int scoreA, int scoreB,
            int finalFoodA, int finalFoodB) {

        ClientModel model = new ClientModel();

        // Populate PlayerView with intentionally stale food values
        model.applySnapshot(new GameSnapshot(
                List.of(
                        new PlayerSnapshot(nickA, TotemColor.RED,   staleFoodA, scoreA, List.of(), List.of()),
                        new PlayerSnapshot(nickB, TotemColor.BLUE,  staleFoodB, scoreB, List.of(), List.of())
                ),
                List.of(), List.of(), List.of(), List.of(),
                Era.I, 1,
                Map.of(), List.of()
        ));

        // Apply end-of-game message with correct server-side food
        model.applyGameEnded(new GameEndedMessage(
                null,
                Map.of(nickA, scoreA, nickB, scoreB),
                Map.of(nickA, finalFoodA, nickB, finalFoodB)
        ));

        return model;
    }

    /**
     * Equal PP, stale PlayerView food is misleading (Bob appears to have more),
     * but finalFood says Alice has more → Alice wins.
     */
    @Test
    void equalPP_finalFoodBreaksTie_ignoreStalePlayerView() {
        ClientModel model = modelWithStaleFoodAndFinalScores(
                "Alice", 1,   // stale: Alice has 1 food (looks like she'd lose)
                "Bob",   4,   // stale: Bob has 4 food (looks like he'd win)
                10, 10,       // equal PP
                5, 3          // finalFood: Alice 5, Bob 3 → Alice wins
        );

        List<String> winners = EndGameHelper.resolveWinners(model);

        assertEquals(1, winners.size(), "Exactly one winner expected");
        assertEquals("Alice", winners.get(0), "Alice should win via finalFood tiebreaker");
    }

    /**
     * Equal PP and equal finalFood → both players share the victory,
     * regardless of stale PlayerView food values.
     */
    @Test
    void equalPP_equalFinalFood_sharedVictory() {
        ClientModel model = modelWithStaleFoodAndFinalScores(
                "Alice", 1,   // stale: different food
                "Bob",   4,
                10, 10,       // equal PP
                5, 5          // finalFood: equal → shared
        );

        List<String> winners = EndGameHelper.resolveWinners(model);

        assertEquals(2, winners.size(), "Both players should share victory");
        assertTrue(winners.contains("Alice"));
        assertTrue(winners.contains("Bob"));
    }
}
