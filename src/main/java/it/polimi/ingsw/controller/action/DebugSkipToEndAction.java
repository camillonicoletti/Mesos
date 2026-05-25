package it.polimi.ingsw.controller.action;

import java.io.Serial;

/** Debug-only action: forces the game to end immediately (requires -Dgame.debug=true). */
public class DebugSkipToEndAction implements GameAction {
    @Serial
    private static final long serialVersionUID = 1L;
}
