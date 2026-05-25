package it.polimi.ingsw.model.enums;

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

    public int getNumber() {
        return number;
    }
}
