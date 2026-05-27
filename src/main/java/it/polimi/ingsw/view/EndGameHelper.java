package it.polimi.ingsw.view;

import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.PlayerView;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shared utility for computing the list of co-winners at end of game,
 * applying the official tiebreaker chain: most PP → most food → shared victory.
 * Used by both GUI and TUI to ensure consistent behaviour.
 */
public final class EndGameHelper {

    private EndGameHelper() {
    }

    /**
     * Returns the ordered list of winners (1 element in case of a single winner,
     * multiple elements in case of a draw). Never returns an empty list unless
     * {@code scores} is empty.
     *
     * @param model the current client model (used to read food values)
     * @return list of winning nicknames
     */
    public static List<String> resolveWinners(ClientModel model) {
        // Server is authoritative on the single winner: this covers both the normal end
        // (highest score, tiebreak applied server-side) and the FA4 forced-winner-by-timeout,
        // where the unique still-connected player wins regardless of score. Only fall back to
        // local recomputation when the server signals a tie (winnerNickname == null).
        String declared = model.getWinnerNickname();
        if (declared != null) {
            return List.of(declared);
        }

        Map<String, Integer> scores = model.getFinalScores();
        if (scores.isEmpty()) {
            return List.of();
        }

        int maxScore = scores.values().stream().mapToInt(x -> x).max().orElse(Integer.MIN_VALUE);
        List<String> topScorers = scores.entrySet().stream()
                .filter(e -> e.getValue() == maxScore)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topScorers.size() == 1) {
            return topScorers;
        }

        int maxFood = topScorers.stream().mapToInt(n -> foodOf(n, model)).max().orElse(0);
        List<String> finalTied = topScorers.stream()
                .filter(n -> foodOf(n, model) == maxFood)
                .collect(Collectors.toList());

        return finalTied.isEmpty() ? topScorers : finalTied;
    }

    private static int foodOf(String nickname, ClientModel model) {
        // Prefer server-authoritative finalFood over the cached PlayerView value.
        Map<String, Integer> finalFood = model.getFinalFood();
        if (finalFood.containsKey(nickname)) return finalFood.get(nickname);
        PlayerView pv = model.getPlayers().get(nickname);
        return pv != null ? pv.getFood() : 0;
    }
}
