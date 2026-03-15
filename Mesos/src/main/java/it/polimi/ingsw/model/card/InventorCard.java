package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.InventorIcons;

public class InventorCard extends CharacterCard{
    private final InventorIcons inventionIcon;

    public InventorCard(InventorIcons inventionIcon) {

        this.inventionIcon = inventionIcon;
    }

    public InventorIcons getInventionIcon() {
        return inventionIcon;
    }
}

// TODO : matching icons logic
// TODO : different icons logic