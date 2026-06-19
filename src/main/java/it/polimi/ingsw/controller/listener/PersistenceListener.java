package it.polimi.ingsw.controller.listener;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.Persistence;
import it.polimi.ingsw.controller.state.EndGameState;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.enums.Era;

import java.util.Map;

// Game listener that persists the controller to disk on every event that meaningfully changes game state.
// It is the only bridge between the in-memory game and Persistence: model and controller stay unaware
// of disk I/O, preserving MVC separation. Events that are consequences of an already-persisted action
// (food/points changes, lobby join notifications) are skipped here to avoid redundant writes.
// When the match ends (onGameEnded), the on-disk file is cleared instead of saved.
public class PersistenceListener implements GameListener {

    private final GameController controller;

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
            return;  // skip: EndGameState.onEnter() fires onGameEnded next, which clears the save file instead
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
        // Saved post-handler by GameController.onMessageReceived, even when this join doesn't fill the lobby.
    }

    @Override
    public void onPlayersCountSet(int count) {
        // Saved post-handler by GameController.onMessageReceived.
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