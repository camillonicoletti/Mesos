package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.enums.InventorIcons;

public class InventorCard extends CharacterCard{
    private final InventorIcons inventionIcon;

    public InventorCard(InventorIcons inventionIcon) {
        super(CharacterType.INVENTOR);
        this.inventionIcon = inventionIcon;
    }

    public InventorIcons getInventionIcon() {
        return inventionIcon;
    }
}
