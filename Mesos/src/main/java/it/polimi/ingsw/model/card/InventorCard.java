package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.InventorIcons;

public class InventorCard extends CharacterCard{
    private final InventorIcons inventionIcon;

    public InventorCard(int id, Era era, int minPlayers, InventorIcons inventionIcon) {

        super(id, era, minPlayers);
        this.inventionIcon = inventionIcon;

    }

    public InventorIcons getInventionIcon() {
        return inventionIcon;
    }
}

// TODO : matching icons logic
// TODO : different icons logic