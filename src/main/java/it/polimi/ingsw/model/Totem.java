package it.polimi.ingsw.model;

import it.polimi.ingsw.model.enums.TotemColor;

import java.io.Serializable;

/**
 * A player's coloured marker placed on board slots: turn-order tiles during
 * round setup and offer tiles during totem placement. Identifies the owning
 * player wherever it sits.
 */
public class Totem implements Serializable {
    private static final long serialVersionUID = 1L;
    private TotemColor color;
    private String position;

    public Totem(TotemColor color, String position) {
        this.color = color;
        this.position = position;
    }

    public TotemColor getColor() {
        return color;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}