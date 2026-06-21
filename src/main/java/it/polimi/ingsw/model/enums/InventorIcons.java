package it.polimi.ingsw.model.enums;

/**
 * Icons printed on Inventor cards.
 * Inventor scoring uses these values to count distinct icons and matching pairs.
 */
public enum InventorIcons {
    INVENTION_1(1),
    INVENTION_2(2),
    INVENTION_3(3),
    INVENTION_4(4),
    INVENTION_5(5),
    INVENTION_6(6),
    INVENTION_7(7),
    INVENTION_8(8),
    INVENTION_9(9),
    INVENTION_10(10);

    private final int number;

    InventorIcons(int number) {
        this.number = number;
    }

    /**
     * Returns the numeric id of the invention icon.
     */
    public int getNumber() {
        return number;
    }
}
