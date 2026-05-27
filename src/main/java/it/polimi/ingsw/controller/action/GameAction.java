package it.polimi.ingsw.controller.action;

import java.io.Serializable;

/**
 * Marker for the Command pattern: every concrete subtype is a serializable
 * client intent (place totem, pick card, build, ready, ...) sent to the
 * {@link it.polimi.ingsw.controller.GameController} and dispatched by the
 * current {@link it.polimi.ingsw.controller.state.GameState} via
 * {@code handleAction}.
 */
public interface GameAction extends Serializable {
}
