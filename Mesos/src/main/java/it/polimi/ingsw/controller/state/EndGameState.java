package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;

import java.io.Serial;
import java.time.Instant;
import java.util.Map;
import it.polimi.ingsw.controller.leaderboard.LeaderboardService;
import it.polimi.ingsw.controller.leaderboard.NoOpLeaderboardService;
import java.time.Instant;
import java.util.HashMap;
public class EndGameState extends AbstractGameState {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public void onEnter(GameController ctx) {
        Game game = ctx.getGame();
        game.calculateFinalScores();
        Player winner = ctx.consumeForcedWinner();
        if (winner == null) {
            winner = game.getWinner();
        }
        Player finalWinner = winner;
        Map<Player, Integer> scores = game.getFinalScores();

        ctx.notifyListeners(l -> l.onGameEnded(finalWinner, scores));

        persistToLeaderboard(ctx, scores);

        game.end();
    }

    private void persistToLeaderboard(GameController ctx, Map<Player, Integer> scores) {
        if (scores == null || scores.isEmpty()) {
            return;
        }

        Map<String, Integer> byNickname = new HashMap<>();
        for (Map.Entry<Player, Integer> e : scores.entrySet()) {
            byNickname.put(e.getKey().getNickname(), e.getValue());
        }
        int playersCount = byNickname.size();

        Map<String, Integer> ranks = ctx.getLeaderboardService().recordMatch(
                ctx.getMatchId(),
                Instant.now(),
                playersCount,
                byNickname);

        if (!ranks.isEmpty()) {
            ctx.notifyListeners(l -> {
                for (Map.Entry<String, Integer> e : ranks.entrySet()) {
                    l.onLeaderboardRank(e.getKey(), e.getValue(), playersCount);
                }
            });
        }
    }
    @Override
    public void handleAction(GameController ctx, String playerNickname, GameAction action) {
        rejectAction(action);
    }

    @Override
    public void onPlayerDisconnected(GameController ctx, String playerNickname) {
        if (!ctx.containsGamePlayer(playerNickname)) {
            return;
        }

        ctx.markPlayerDisconnected(playerNickname);
    }

    @Override
    public void onPlayerReconnected(GameController ctx, String playerNickname) {
        if (!ctx.containsGamePlayer(playerNickname)) {
            return;
        }
    }
}