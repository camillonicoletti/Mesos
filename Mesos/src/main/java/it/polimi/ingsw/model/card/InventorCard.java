package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.InventorIcons;

public class InventorCard extends CharacterCard{
    private static final long serialVersionUID = 1L;
    private final InventorIcons inventionIcon;

    public InventorCard(int id, Era era, int minPlayers, InventorIcons inventionIcon) {

        super(id, era, minPlayers);
        this.inventionIcon = inventionIcon;

    }

    public InventorIcons getInventionIcon() {
        return inventionIcon;
    }
}

