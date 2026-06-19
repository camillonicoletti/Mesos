package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.model.Player;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// Internal helper of ActionResolutionState that schedules the extra upper-row picks granted by
// ExtraCardUpperRow buildings, preserving the turn-order of the players entitled to them.
final class ExtraPickQueue implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final class ExtraPick implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Player player;
        private final int picks;

        private ExtraPick(Player player, int picks) {
            this.player = player;
            this.picks = picks;
        }
    }

    private final List<ExtraPick> entries = new ArrayList<>();
    private int currentIndex;
    private int remainingPicks;
    private boolean started;

    void clear() {
        entries.clear();
        currentIndex = 0;
        remainingPicks = 0;
        started = false;
    }

    void add(Player player, int picks) {
        if (picks > 0) {
            entries.add(new ExtraPick(player, picks));
        }
    }

    void start() {
        started = true;
        currentIndex = 0;
        remainingPicks = 0;
    }

    boolean isActive() {
        return started && currentIndex < entries.size();
    }

    Player currentPlayer() {
        if (!isActive()) {
            return null;
        }
        return entries.get(currentIndex).player;
    }

    int prepareCurrentPicks() {
        if (!isActive()) {
            remainingPicks = 0;
            return 0;
        }
        remainingPicks = entries.get(currentIndex).picks;
        return remainingPicks;
    }

    int remainingPicks() {
        return remainingPicks;
    }

    void consumePick() {
        if (remainingPicks > 0) {
            remainingPicks--;
        }
    }

    boolean currentPicksDepleted() {
        return remainingPicks == 0;
    }

    void advance() {
        if (isActive()) {
            currentIndex++;
        }
        remainingPicks = 0;
    }

    boolean isCurrentPlayer(String nickname) {
        Player player = currentPlayer();
        return player != null && player.getNickname().equals(nickname);
    }

    boolean hasPendingPlayer(String nickname) {
        if (nickname == null) {
            return false;
        }
        for (int i = currentIndex; i < entries.size(); i++) {
            if (nickname.equals(entries.get(i).player.getNickname())) {
                return true;
            }
        }
        return false;
    }
}
