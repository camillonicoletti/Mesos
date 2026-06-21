package it.polimi.ingsw.view.gui.component;

// Simple data holder with the counted numbers of a tribe summary
// (gatherers, hunters, builders, shamans, artists, inventors and their bonuses).
record TribeSummaryStats(
        int gatherers,
        int gathererFoodDiscount,
        int hunters,
        int builders,
        int builderFoodDiscount,
        int shamans,
        int shamanStars,
        int artists,
        int inventors,
        int distinctInventorIcons
) {
}
