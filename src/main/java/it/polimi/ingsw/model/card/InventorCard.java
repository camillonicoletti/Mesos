package it.polimi.ingsw.model.card;

import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.InventorIcons;

/**
 * Character with an invention icon.
 * Inventor scoring uses distinct icons, and some effects count matching icon pairs.
 */
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

    /**
     * Sends this card to the inventor branch of the visitor.
     */
    @Override
    public <R> R accept(CardVisitor<R> visitor) {
        return visitor.visitInventor(this);
    }
}
