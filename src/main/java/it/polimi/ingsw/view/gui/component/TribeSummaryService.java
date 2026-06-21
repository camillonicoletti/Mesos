package it.polimi.ingsw.view.gui.component;

import com.google.gson.JsonObject;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.view.card.CharacterTally;
import it.polimi.ingsw.view.card.CharacterType;
import it.polimi.ingsw.view.gui.asset.CardAsset;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

// Counts a player's character cards to build a summary of their tribe
// (how many hunters, builders, shamans... and the bonuses they give).
final class TribeSummaryService {
    private static final int FOOD_DISCOUNT_PER_GATHERER = 3;

    private final GuiAssetCatalog assetCatalog;

    TribeSummaryService(GuiAssetCatalog assetCatalog) {
        this.assetCatalog = assetCatalog;
    }

    TribeSummaryStats statsFor(PlayerView player) {
        Tally tally = new Tally();
        for (Integer cardId : player.getCharacterCardIds()) {
            Optional<CardAsset> asset = assetCatalog.findCard(cardId);
            if (asset.isEmpty()) {
                continue;
            }
            String type = asset.get().getType().orElse("");
            JsonObject details = assetCatalog.findCardDetails(cardId).orElse(null);
            CharacterType.from(type).tally(tally, details);
        }
        return tally.toStats();
    }

    private static final class Tally implements CharacterTally {
        private int gatherers;
        private int hunters;
        private int builders;
        private int builderFoodDiscount;
        private int shamans;
        private int shamanStars;
        private int artists;
        private int inventors;
        private final Set<String> inventionIcons = new HashSet<>();

        @Override
        public void addHunter() {
            hunters++;
        }

        @Override
        public void addGatherer() {
            gatherers++;
        }

        @Override
        public void addBuilder(int foodDiscount) {
            builders++;
            builderFoodDiscount += foodDiscount;
        }

        @Override
        public void addShaman(int stars) {
            shamans++;
            shamanStars += stars;
        }

        @Override
        public void addArtist() {
            artists++;
        }

        @Override
        public void addInventor(String inventionIcon) {
            inventors++;
            if (inventionIcon != null && !inventionIcon.isBlank()) {
                inventionIcons.add(inventionIcon);
            }
        }

        TribeSummaryStats toStats() {
            return new TribeSummaryStats(
                    gatherers,
                    gatherers * FOOD_DISCOUNT_PER_GATHERER,
                    hunters,
                    builders,
                    builderFoodDiscount,
                    shamans,
                    shamanStars,
                    artists,
                    inventors,
                    inventionIcons.size());
        }
    }
}
