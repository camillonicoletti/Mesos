package it.polimi.ingsw.view;

import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.protocol.clientbound.GameEndedMessage;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for EndGameHelper (view).
 *
 * Verifies leaderboard text formatting from final scoring results.
 */
class EndGameHelperTest {

    @Test
    void forcedWinnerByTimeoutBeatsHigherScoringDisconnectedPlayer() {
        // A server-declared forced winner (disconnection timeout) must be trusted
        // as the sole winner even if a disconnected player has a higher raw score.
        ClientModel model = new ClientModel();
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("Alice", 10);
        scores.put("Bob", 30);
        Map<String, Integer> food = new LinkedHashMap<>();
        food.put("Alice", 4);
        food.put("Bob", 6);
        model.applyGameEnded(new GameEndedMessage("Alice", scores, food));

        assertEquals(List.of("Alice"), EndGameHelper.resolveWinners(model));
    }

    @Test
    void normalEndWithDeclaredWinnerIsTrusted() {
        ClientModel model = new ClientModel();
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("Alice", 25);
        scores.put("Bob", 12);
        Map<String, Integer> food = new LinkedHashMap<>();
        food.put("Alice", 3);
        food.put("Bob", 5);
        model.applyGameEnded(new GameEndedMessage("Alice", scores, food));

        assertEquals(List.of("Alice"), EndGameHelper.resolveWinners(model));
    }

    @Test
    void tieWithNullDeclaredWinnerProducesCoWinners() {
        ClientModel model = new ClientModel();
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("Alice", 20);
        scores.put("Bob", 20);
        Map<String, Integer> food = new LinkedHashMap<>();
        food.put("Alice", 5);
        food.put("Bob", 5);
        // null winner = server signals shared victory after applying its own tiebreak chain.
        model.applyGameEnded(new GameEndedMessage(null, scores, food));

        List<String> winners = EndGameHelper.resolveWinners(model);
        assertEquals(2, winners.size());
        assertTrue(winners.contains("Alice") && winners.contains("Bob"));
    }
}
