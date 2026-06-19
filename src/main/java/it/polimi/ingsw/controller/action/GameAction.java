package it.polimi.ingsw.controller.action;

import java.io.Serializable;

// Command pattern: every subtype is a serializable client intent (place totem, pick card,
// build, ready, ...) sent to GameController and dispatched by the current GameState.
public interface GameAction extends Serializable {
}
