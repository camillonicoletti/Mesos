package it.polimi.ingsw.view.gui.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.view.gui.asset.CardAsset;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

final class TribeSummaryService {
    private static final int FOOD_DISCOUNT_PER_GATHERER = 3;

    private final GuiAssetCatalog assetCatalog;

    TribeSummaryService(GuiAssetCatalog assetCatalog) {
        this.assetCatalog = assetCatalog;
    }

    TribeSummaryStats statsFor(PlayerView player) {
        int gatherers = 0;
        int hunters = 0;
        int builders = 0;
        int builderFoodDiscount = 0;
        int shamans = 0;
        int shamanStars = 0;
        int artists = 0;
        int inventors = 0;
        Set<String> inventionIcons = new HashSet<>();

        for (Integer cardId : player.getCharacterCardIds()) {
            Optional<CardAsset> asset = assetCatalog.findCard(cardId);
            if (asset.isEmpty()) {
                continue;
            }

            String type = asset.get().getType().orElse("");
            JsonObject details = assetCatalog.findCardDetails(cardId).orElse(null);

            switch (type) {
                case "gatherer" -> gatherers++;
                case "hunter" -> hunters++;
                case "builder" -> {
                    builders++;
                    if (details != null) {
                        builderFoodDiscount += intDetail(details, "foodDiscount");
                    }
                }
                case "shaman" -> {
                    shamans++;
                    if (details != null) {
                        shamanStars += intDetail(details, "stars");
                    }
                }
                case "artist" -> artists++;
                case "inventor" -> {
                    inventors++;
                    if (details != null) {
                        String icon = stringDetail(details, "inventionIcon");
                        if (!icon.isBlank()) {
                            inventionIcons.add(icon);
                        }
                    }
                }
                default -> {
                    // Non-character or unknown card types do not affect the six tribe counters.
                }
            }
        }

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
                inventionIcons.size()
        );
    }

    private String stringDetail(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return "";
        }
        return value.getAsString();
    }

    private int intDetail(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return 0;
        }
        return value.getAsInt();
    }
}
