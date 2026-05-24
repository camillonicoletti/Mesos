package it.polimi.ingsw.controller.listener;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.Persistence;
import it.polimi.ingsw.controller.state.EndGameState;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.enums.Era;

import java.util.Map;

/**
 * Game listener that persists the controller to disk on every event that
 * meaningfully changes game state.
 *
 * <p>This listener is the only bridge between the in-memory game and
 * {@link Persistence}: model and controller stay unaware of disk I/O,
 * preserving MVC separation. Events that are pure consequences of an already-
 * saved action (food/points changes, lobby join notifications) are skipped to
 * avoid redundant writes.
 *
 * <p>When the match ends ({@link #onGameEnded}), the on-disk file is cleared
 * instead of saved: the next server start should begin with a clean slate.
 */
public class PersistenceListener implements GameListener {

    private final GameController controller;

    /**
     * Creates a listener bound to the given controller. The controller is the
     * one that will be serialized on every save trigger.
     *
     * @param controller the controller whose state must be persisted
     */
    public PersistenceListener(GameController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("Controller cannot be null");
        }
        this.controller = controller;
    }

    private void persist() {
        Persistence.save(controller);
    }

    // ── Save triggers ───────────────────────────────────────────────────────

    @Override
    public void onGameStarted() {
        persist();
    }

    @Override
    public void onStateChanged(GameState newState) {
        if (newState instanceof EndGameState) {
            return;  // skip: clear() already called by onGameEnded
        }
        persist();
    }

    @Override
    public void onTotemPlaced(String nickname, char tileId) {
        // Saved post-handler by GameController.onMessageReceived.
    }

    @Override
    public void onCharacterCardTaken(String nickname, String cardId, boolean fromUpperRow) {
        // Saved post-handler by GameController.onMessageReceived.
    }

    @Override
    public void onBuildingCardTaken(String nickname, String cardId, boolean fromUpperRow) {
        // Saved post-handler by GameController.onMessageReceived.
    }

    @Override
    public void onRoundEnded(int roundNumber) {
        persist();
    }

    @Override
    public void onEraChanged(Era newEra) {
        persist();
    }

    @Override
    public void onPlayerDisconnected(String nickname) {
        persist();
    }

    @Override
    public void onPlayerReconnected(String nickname) {
        persist();
    }

    @Override
    public void onGameSuspended() {
        persist();
    }

    @Override
    public void onGameResumed() {
        persist();
    }

    // ── Match end: clear the save file ──────────────────────────────────────

    @Override
    public void onGameEnded(Player winner, Map<Player, Integer> finalScores) {
        Persistence.clear();
    }

    // ── Skipped events (covered upstream or non-state-changing) ─────────────

    @Override
    public void onPlayerJoined(String nickname) {
        // Covered by the subsequent onStateChanged when the lobby fills up.
    }

    @Override
    public void onPlayersCountSet(int count) {
        // Covered by the subsequent onStateChanged when the lobby fills to the chosen count.
    }

    @Override
    public void onFoodChanged(String nickname, int newAmount, int delta) {
        // Consequence of an already-persisted action (card pick, event resolve).
    }

    @Override
    public void onPointsChanged(String nickname, int newAmount, int delta) {
        // Consequence of an already-persisted action (event resolve, end-of-game).
    }

    @Override
    public void onExtraCardPickAvailable(String nickname, int picks) {
        // Pure UI notification; no state change to persist.
    }
}