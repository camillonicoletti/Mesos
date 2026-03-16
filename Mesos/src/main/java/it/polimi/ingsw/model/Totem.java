package it.polimi.ingsw.model;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.enums.TotemPosition;

public class Totem {
    private TotemColor color;
    private TotemPosition position;

    public Totem(TotemColor color, TotemPosition position) {
        this.color = color;
        this.position = position;
    }

    public TotemColor getColor() {
        return color;
    }

    public TotemPosition getPosition() {
        return position;
    }

    public void setPosition(TotemPosition position) {
        this.position = position;
    }


}