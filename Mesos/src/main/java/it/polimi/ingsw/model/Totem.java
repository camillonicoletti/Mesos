package it.polimi.ingsw.model;

import it.polimi.ingsw.model.enums.TotemColor;

public class Totem {
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