package it.polimi.ingsw.controller.action;

import java.io.Serial;

/** Debug-only action: skips the current player's turn / round (requires -Dgame.debug=true). */
public class DebugSkipRoundAction implements GameAction {
    @Serial
    private static final long serialVersionUID = 1L;
}
