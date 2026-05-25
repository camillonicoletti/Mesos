package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.view.gui.asset.CardAsset;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import it.polimi.ingsw.view.gui.util.GuiAnimations;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

final class PlayerDockView {
    private static final int MAX_VISIBLE_SUMMARIES = 3;

    private static final String ICON_HUNTER = "assets/icons/summary/hunter.png";
    private static final String ICON_BUILDER = "assets/icons/summary/builder.png";
    private static final String ICON_BUILDER_DISCOUNT = "assets/icons/summary/builder_discount.png";
    private static final String ICON_GATHERER = "assets/icons/summary/gatherer.png";
    private static final String ICON_SUSTENANCE_DISCOUNT = "assets/icons/summary/sustenance_discount.png";
    private static final String ICON_ARTIST = "assets/icons/summary/artist.png";
    private static final String ICON_INVENTOR = "assets/icons/summary/inventor.png";
    private static final String ICON_SHAMAN = "assets/icons/summary/shaman.png";
    private static final String ICON_STAR = "assets/icons/summary/star.png";

    private final ClientController controller;
    private final ClientModel model;
    private final GuiAssetCatalog assetCatalog;
    private final CardDescriptionService cardDescriptions;
    private final TribeSummaryService tribeSummaries;
    private final TotemMarkerFactory totemMarkers;
    private final CardImageProvider cardImages;
    private final IntConsumer showCardDetails;
    private final Function<String, VBox> panelFactory;
    private final BiFunction<String, Boolean, Button> buttonFactory;
    private final BiConsumer<Node, Pos> showInlinePanel;
    private final Runnable hideInlinePanel;
    private int summaryStartIndex;

    PlayerDockView(ClientController controller,
                   ClientModel model,
                   GuiAssetCatalog assetCatalog,
                   CardDescriptionService cardDescriptions,
                   TotemMarkerFactory totemMarkers,
                   CardImageProvider cardImages,
                   IntConsumer showCardDetails,
                   Function<String, VBox> panelFactory,
                   BiFunction<String, Boolean, Button> buttonFactory,
                   BiConsumer<Node, Pos> showInlinePanel,
                   Runnable hideInlinePanel) {
        this.controller = controller;
        this.model = model;
        this.assetCatalog = assetCatalog;
        this.cardDescriptions = cardDescriptions;
        this.tribeSummaries = new TribeSummaryService(assetCatalog);
        this.totemMarkers = totemMarkers;
        this.cardImages = cardImages;
        this.showCardDetails = showCardDetails;
        this.panelFactory = panelFactory;
        this.buttonFactory = buttonFactory;
        this.showInlinePanel = showInlinePanel;
        this.hideInlinePanel = hideInlinePanel;
    }

    VBox render() {
        VBox dock = new VBox(6);
        dock.getStyleClass().add("player-dock");
        dock.setAlignment(Pos.CENTER);

        HBox status = new HBox(12);
        status.getStyleClass().add("table-status");
        status.getChildren().addAll(
                smallStatus("Era", nullSafe(model.getCurrentEra())),
                smallStatus("Round", String.valueOf(model.getRoundNumber())),
                smallStatus("Turn", nullSafe(currentTurnNickname()))
        );

        Button exit = new Button("Quit");
        exit.getStyleClass().add("dock-exit");
        exit.setOnAction(event -> {
            controller.disconnect();
            Platform.exit();
        });

        Button debugEnd = new Button("⚑ End game [DEBUG]");
        debugEnd.getStyleClass().add("dock-exit");
        debugEnd.setOnAction(event -> controller.send(
                new it.polimi.ingsw.network.protocol.serverbound.ForceEndGameMessage()));

HBox statusRow = new HBox(14, status, spacer());
        if (model.isExtraPickAvailableForMe()) {
            Button skipExtra = new Button("Skip extra pick");
            skipExtra.getStyleClass().add("dock-exit");
            skipExtra.setOnAction(event -> controller.send(
                    new it.polimi.ingsw.network.protocol.serverbound.GameActionMessage(
                            new it.polimi.ingsw.controller.action.SkipExtraPickAction())));
            statusRow.getChildren().add(skipExtra);
        }
        statusRow.getChildren().addAll(debugEnd, exit);
        statusRow.getStyleClass().add("player-dock-status-row");
        statusRow.setAlignment(Pos.CENTER);

        dock.getChildren().addAll(statusRow, playerSummaryRows());
        return dock;
    }

    private VBox playerSummaryRows() {
        VBox rows = new VBox(7);
        rows.getStyleClass().add("player-summary-rows");
        rows.setAlignment(Pos.CENTER);

        List<PlayerView> players = new ArrayList<>(model.getPlayers().values());
        if (players.size() <= MAX_VISIBLE_SUMMARIES) {
            HBox row = playerSummaryStrip();
            for (PlayerView player : players) {
                row.getChildren().add(playerSummary(player, players.size()));
            }
            rows.getChildren().add(row);
        } else {
            rows.getChildren().add(playerSummaryCarousel(players));
        }
        return rows;
    }

    private Node playerSummaryCarousel(List<PlayerView> players) {
        summaryStartIndex = clampSummaryStart(players);

        HBox carousel = new HBox(18);
        carousel.getStyleClass().add("player-summary-carousel");
        carousel.setAlignment(Pos.CENTER);

        Button previous = carouselButton("<");
        Button next = carouselButton(">");
        HBox visibleSummaries = playerSummaryStrip();
        visibleSummaries.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(visibleSummaries, Priority.ALWAYS);

        Runnable refreshVisibleSummaries = () -> {
            visibleSummaries.getChildren().clear();
            int end = Math.min(players.size(), summaryStartIndex + MAX_VISIBLE_SUMMARIES);
            for (int index = summaryStartIndex; index < end; index++) {
                visibleSummaries.getChildren().add(playerSummary(players.get(index), players.size()));
            }
            previous.setDisable(summaryStartIndex == 0);
            next.setDisable(summaryStartIndex >= players.size() - MAX_VISIBLE_SUMMARIES);
        };

        previous.setOnAction(event -> {
            summaryStartIndex = Math.max(0, summaryStartIndex - 1);
            refreshVisibleSummaries.run();
        });
        next.setOnAction(event -> {
            summaryStartIndex = Math.min(players.size() - MAX_VISIBLE_SUMMARIES, summaryStartIndex + 1);
            refreshVisibleSummaries.run();
        });

        refreshVisibleSummaries.run();
        carousel.getChildren().addAll(previous, visibleSummaries, next);
        return carousel;
    }

    private HBox playerSummaryStrip() {
        HBox row = new HBox(9);
        row.setAlignment(Pos.CENTER);
        row.getStyleClass().add("player-summary-strip");
        return row;
    }

    private Button carouselButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("player-summary-carousel-button");
        button.setFocusTraversable(false);
        GuiAnimations.addHoverScale(button);
        return button;
    }

    private int clampSummaryStart(List<PlayerView> players) {
        int maxStart = Math.max(0, players.size() - MAX_VISIBLE_SUMMARIES);
        if (summaryStartIndex < 0) {
            return 0;
        }
        return Math.min(summaryStartIndex, maxStart);
    }

    private Node playerSummary(PlayerView player, int playerCount) {
        boolean disconnected = model.isPlayerDisconnected(player.getNickname());
        boolean current = player.getNickname().equals(currentTurnNickname());
        boolean me = player.getNickname().equals(model.getMyNickname());

        VBox card = new VBox(3);
        card.getStyleClass().add("player-summary");
        card.getStyleClass().add(playerCount <= 3 ? "player-summary-roomy" : "player-summary-many");

        if (current) {
            card.getStyleClass().add("player-summary-current");
        }
        if (me) {
            card.getStyleClass().add("player-summary-me");
        }
        if (disconnected) {
            card.getStyleClass().add("player-summary-disconnected");
        }

        card.setOnMouseClicked(event -> showTribePanel(player));
        GuiAnimations.addHoverScale(card);

        HBox head = new HBox(7);
        head.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label((me ? "You · " : "") + player.getNickname());
        name.getStyleClass().add("player-summary-name");
        head.getChildren().addAll(totemDot(player), name);

        Label resources = new Label("Food " + player.getFood() + "   PP " + player.getPrestigePoints()
                + "   Buildings " + player.getBuildingCardIds().size());
        resources.getStyleClass().add("player-summary-stat");

        card.getChildren().addAll(head, resources, tribeStatsGrid(player, true));
        Node buildings = buildingSummaryStrip(player);
        if (buildings != null) {
            card.getChildren().add(buildings);
        }

        return card;
    }

    private Node buildingSummaryStrip(PlayerView player) {
        List<Integer> buildingIds = player.getBuildingCardIds();
        if (buildingIds.isEmpty()) {
            return null;
        }

        HBox strip = new HBox(5);
        strip.getStyleClass().add("player-building-strip");
        strip.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Buildings");
        title.getStyleClass().add("player-building-title");
        strip.getChildren().add(title);

        int visibleCount = Math.min(5, buildingIds.size());
        int hiddenCount = buildingIds.size() - visibleCount;
        List<Integer> visibleBuildings = buildingIds.subList(buildingIds.size() - visibleCount, buildingIds.size());
        for (Integer buildingId : visibleBuildings) {
            strip.getChildren().add(stackedMiniCardNode(buildingId, 30, 42));
        }

        if (hiddenCount > 0) {
            Label more = new Label("+" + hiddenCount);
            more.getStyleClass().add("player-summary-more");
            strip.getChildren().add(more);
        }

        return strip;
    }

    private void showTribePanel(PlayerView player) {
        VBox panel = panelFactory.apply("Tribe of " + player.getNickname());
        panel.getStyleClass().add("tribe-focus-panel");

        VBox content = new VBox(12);
        content.getStyleClass().add("tribe-panel-content");

        FlowPane largeCards = tribeCards(player, 108, 152);
        largeCards.getStyleClass().add("tribe-panel-cards");
        content.getChildren().addAll(
                tribePanelHeader(player),
                tribeStatsGrid(player, false),
                summaryPill("Characters", countSummary(player.getCharacterCardIds())),
                summaryPill("Buildings", countSummary(player.getBuildingCardIds())),
                largeCards
        );
        content.setMaxWidth(1080);

        Button close = buttonFactory.apply("Close", false);
        close.setOnAction(event -> hideInlinePanel.run());
        HBox actions = new HBox(close);
        actions.setAlignment(Pos.CENTER_RIGHT);

        panel.getChildren().addAll(content, actions);
        showInlinePanel.accept(panel, Pos.CENTER);
    }

    private Node tribePanelHeader(PlayerView player) {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(player.getNickname());
        name.getStyleClass().add("tribe-title");
        Label resources = new Label("Food " + player.getFood() + "   PP " + player.getPrestigePoints());
        resources.getStyleClass().add("tribe-resources");
        header.getChildren().addAll(totemDot(player), name, resources);
        return header;
    }

    private FlowPane tribeCards(PlayerView player, double width, double height) {
        FlowPane cards = new FlowPane(6, 6);
        cards.setAlignment(Pos.CENTER);
        for (Integer id : player.getCharacterCardIds()) {
            cards.getChildren().add(miniCardNode(id, width, height));
        }
        for (Integer id : player.getBuildingCardIds()) {
            cards.getChildren().add(miniCardNode(id, width, height));
        }
        if (cards.getChildren().isEmpty()) {
            Label empty = new Label("No cards in tribe");
            empty.getStyleClass().add("muted");
            cards.getChildren().add(empty);
        }
        return cards;
    }

    private Node miniCardNode(int cardId, double width, double height) {
        StackPane card = new StackPane();
        card.getStyleClass().add("mini-card");
        card.setPrefSize(width, height);
        card.getChildren().add(cardImages.create(cardId, width, height));
        card.setOnMouseClicked(event -> showCardDetails.accept(cardId));
        GuiAnimations.addHoverScale(card);
        return card;
    }

    private Label summaryPill(String label, String value) {
        Label pill = new Label(label + ": " + value);
        pill.getStyleClass().add("tribe-summary-pill");
        return pill;
    }

    private Node tribeStatsGrid(PlayerView player, boolean compact) {
        TribeSummaryStats stats = tribeSummaries.statsFor(player);

        if (compact) {
            HBox grid = new HBox(6);
            grid.setAlignment(Pos.CENTER);
            grid.getStyleClass().addAll("tribe-stat-grid", "tribe-stat-grid-compact");
            addTribeStatCells(grid, player, stats, true);
            return grid;
        }

        FlowPane grid = new FlowPane(7, 7);
        grid.setAlignment(Pos.CENTER);
        grid.getStyleClass().add("tribe-stat-grid");
        addTribeStatCells(grid, player, stats, false);
        return grid;
    }

    private void addTribeStatCells(Pane grid, PlayerView player, TribeSummaryStats stats, boolean compact) {
        grid.getChildren().add(tribeStatCell("gatherer",
                List.of(ICON_GATHERER, ICON_SUSTENANCE_DISCOUNT),
                String.valueOf(stats.gatherers()),
                "-" + stats.gathererFoodDiscount(),
                compact,
                player));
        grid.getChildren().add(tribeStatCell("hunter",
                List.of(ICON_HUNTER),
                String.valueOf(stats.hunters()),
                "",
                compact,
                player));
        grid.getChildren().add(tribeStatCell("builder",
                List.of(ICON_BUILDER, ICON_BUILDER_DISCOUNT),
                String.valueOf(stats.builders()),
                "-" + stats.builderFoodDiscount(),
                compact,
                player));
        grid.getChildren().add(tribeStatCell("shaman",
                List.of(ICON_SHAMAN, ICON_STAR),
                String.valueOf(stats.shamans()),
                String.valueOf(stats.shamanStars()),
                compact,
                player));
        grid.getChildren().add(tribeStatCell("artist",
                List.of(ICON_ARTIST),
                String.valueOf(stats.artists()),
                "",
                compact,
                player));
        grid.getChildren().add(tribeStatCell("inventor",
                List.of(ICON_INVENTOR),
                String.valueOf(stats.inventors()),
                String.valueOf(stats.distinctInventorIcons()),
                compact,
                player));
    }

    private Node tribeStatCell(String type, List<String> iconPaths, String primary, String secondary,
                               boolean compact, PlayerView player) {
        VBox cell = new VBox(3);
        cell.getStyleClass().addAll("tribe-stat-cell", "tribe-stat-" + type);
        if (iconPaths.size() > 1) {
            cell.getStyleClass().add("tribe-stat-cell-wide");
        }
        cell.setAlignment(Pos.CENTER);

        HBox top = new HBox(3);
        top.getStyleClass().add("tribe-stat-icons");
        top.setAlignment(Pos.CENTER);
        for (String iconPath : iconPaths) {
            double iconSize = compact ? (iconPaths.size() > 1 ? 27 : 31) : (iconPaths.size() > 1 ? 64 : 68);
            top.getChildren().add(statIcon(iconPath, iconSize));
        }

        HBox values = new HBox(5);
        values.setAlignment(Pos.CENTER);
        Label main = new Label(primary);
        main.getStyleClass().add("tribe-stat-main");
        values.getChildren().add(main);
        if (secondary != null && !secondary.isBlank()) {
            Label sub = new Label(secondary);
            sub.getStyleClass().add("tribe-stat-sub");
            values.getChildren().add(sub);
        }

        cell.getChildren().addAll(top, values);
        if (compact) {
            cell.getChildren().add(characterCardsForType(player, type));
        }
        return cell;
    }

    private Node characterCardsForType(PlayerView player, String type) {
        Pane cards = new Pane();
        cards.getStyleClass().add("tribe-stat-card-strip");
        double stripWidth = 34;
        double stripHeight = 110;
        cards.setMinSize(stripWidth, stripHeight);
        cards.setPrefSize(stripWidth, stripHeight);
        cards.setMaxSize(stripWidth, stripHeight);
        cards.setClip(new Rectangle(stripWidth, stripHeight));

        List<Integer> matching = player.getCharacterCardIds().stream()
                .filter(cardId -> assetCatalog.findCard(cardId)
                        .flatMap(CardAsset::getType)
                        .map(type::equals)
                        .orElse(false))
                .toList();

        if (matching.isEmpty()) {
            Region empty = new Region();
            empty.getStyleClass().add("tribe-stat-empty-card-slot");
            empty.setLayoutX(3);
            empty.setLayoutY(32);
            cards.getChildren().add(empty);
            return cards;
        }

        int visibleCount = Math.min(10, matching.size());
        int hiddenCount = matching.size() - visibleCount;
        List<Integer> visibleCards = matching.subList(matching.size() - visibleCount, matching.size());

        double cardWidth = 32;
        double cardHeight = 45;
        double reservedBadgeHeight = hiddenCount > 0 ? 14 : 0;
        double availableStackHeight = stripHeight - reservedBadgeHeight;
        double step = visibleCards.size() == 1
                ? 0
                : Math.min(12, (availableStackHeight - cardHeight) / (visibleCards.size() - 1));
        double stackHeight = cardHeight + Math.max(0, visibleCards.size() - 1) * step;
        double startX = (stripWidth - cardWidth) / 2;
        double startY = Math.max(0, availableStackHeight - stackHeight);

        for (int index = 0; index < visibleCards.size(); index++) {
            Node card = stackedMiniCardNode(visibleCards.get(index), cardWidth, cardHeight);
            card.setLayoutX(startX);
            card.setLayoutY(startY + index * step);
            card.setViewOrder(visibleCards.size() - index);
            cards.getChildren().add(card);
        }

        if (hiddenCount > 0) {
            Label more = new Label("+" + hiddenCount);
            more.getStyleClass().add("tribe-stat-card-more");
            more.setLayoutX(7);
            more.setLayoutY(stripHeight - reservedBadgeHeight);
            cards.getChildren().add(more);
        }
        return cards;
    }

    private Node stackedMiniCardNode(int cardId, double width, double height) {
        StackPane card = new StackPane();
        card.getStyleClass().addAll("mini-card", "mini-card-stacked");
        card.setMinSize(width, height);
        card.setPrefSize(width, height);
        card.setMaxSize(width, height);
        card.getChildren().add(cardImages.create(cardId, width, height));
        card.setOnMouseClicked(event -> showCardDetails.accept(cardId));
        return card;
    }

    private Node statIcon(String path, double size) {
        try (InputStream stream = assetCatalog.openResource(path).orElse(null)) {
            if (stream != null) {
                ImageView image = new ImageView(new Image(stream));
                image.setFitWidth(size);
                image.setFitHeight(size);
                image.setPreserveRatio(true);
                image.getStyleClass().add("tribe-stat-icon");
                return image;
            }
        } catch (Exception ignored) {
            // Fallback dot below.
        }

        Region fallback = new Region();
        fallback.getStyleClass().add("tribe-stat-icon-fallback");
        return fallback;
    }

    private String countSummary(List<Integer> cardIds) {
        if (cardIds.isEmpty()) {
            return "0";
        }

        Map<String, Long> counts = cardIds.stream()
                .map(cardDescriptions::summaryLabelFor)
                .collect(Collectors.groupingBy(label -> label, LinkedHashMap::new, Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(" · "));
    }

    private HBox smallStatus(String label, String value) {
        Label key = new Label(label);
        key.getStyleClass().add("table-status-key");

        Label val = new Label(value);
        val.getStyleClass().add("table-status-value");

        HBox row = new HBox(8, key, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private Region totemDot(PlayerView player) {
        Region dot = new Region();
        dot.getStyleClass().add("totem-dot");
        dot.setStyle("-fx-background-color: " + totemMarkers.colorHex(player.getTotemColor()) + ";");
        return dot;
    }

    private String currentTurnNickname() {
        if ("TotemPlacementState".equals(model.getGameState())) {
            return model.getCurrentTotemPlacementPlayer();
        }

        if ("ActionResolutionState".equals(model.getGameState())) {
            return model.getCurrentActionResolutionPlayer();
        }

        return null;
    }

    private String nullSafe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    @FunctionalInterface
    interface CardImageProvider {
        Node create(int cardId, double width, double height);
    }
}
