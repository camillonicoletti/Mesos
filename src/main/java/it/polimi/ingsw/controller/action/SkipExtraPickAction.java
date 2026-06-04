package it.polimi.ingsw.controller.action;

import java.io.Serializable;

/**
 * Action sent by a player to decline the optional extra pick granted by
 * building #13 ({@code ExtraCardUpperRowEffect}). The rulebook marks the
 * extra pick as optional; this action is the player's way to skip it
 * without picking a card or building.
 *
 * <p>Only meaningful during the extra-pick phase of
 * {@code ActionResolutionState}; rejected in any other state.
 */
public class SkipExtraPickAction implements GameAction {
    private static final long serialVersionUID = 1L;
}
