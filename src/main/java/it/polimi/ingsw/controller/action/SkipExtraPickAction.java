package it.polimi.ingsw.controller.action;

import java.io.Serializable;

// Player declines the optional extra upper-row pick granted by the ExtraCardUpperRow building effect.
// Only meaningful during the extra-pick phase of ActionResolutionState; rejected in any other state.
public class SkipExtraPickAction implements GameAction {
    private static final long serialVersionUID = 1L;
}
