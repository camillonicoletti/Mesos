package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.view.gui.asset.CardAsset;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import it.polimi.ingsw.view.gui.util.GuiAnimations;
import it.polimi.ingsw.view.gui.util.GuiAudio;
import it.polimi.ingsw.view.gui.util.GuiIcons;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;


// Shows each player's area: name, totem, resources and a summary of their tribe.
final class PlayerDockView {
    private static final int MAX_VISIBLE_SUMMARIES = 3;
    private static final String PRESTIGE_BADGE_PATH = "assets/icons/endgame_prestige_badge.png";
    private static final String FOOD_BADGE_PATH = "assets/icons/engame_food_badge_prova.png";
    private static final double PLAYER_RESOURCE_BADGE_SIZE = 50;

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
    private final Map<String, Node> summaryTargets = new HashMap<>();
    private final Map<String, Node> deckCardTargets = new HashMap<>();
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

    // Builds the bottom dock: the status row (era, round, turn, buttons) and the player summaries.
    VBox render() {
        VBox dock = new VBox(6);
        dock.getStyleClass().add("player-dock");
        dock.setAlignment(Pos.CENTER);

        HBox status = new HBox(12);
        status.getStyleClass().add("table-status");
        status.getChildren().addAll(
                smallStatus("Era", GuiViews.nullSafe(model.getCurrentEra())),
                smallStatus("Round", String.valueOf(model.getRoundNumber())),
                smallStatus("Turn", GuiViews.nullSafe(GuiViews.currentTurnNickname(model)))
        );

        Button exit = new Button("Quit");
        exit.getStyleClass().add("dock-exit");
        exit.setOnAction(event -> {
            controller.disconnect();
            Platform.exit();
        });

        Button debugEnd = new Button("⚑ End game [DEBUG]");
        debugEnd.getStyleClass().add("dock-exit");
        debugEnd.setOnAction(event -> controller.forceEndGame());

        HBox statusRow = new HBox(14, status, GuiViews.spacer());
        if (model.isExtraPickAvailableForMe()) {
            Button skipExtra = new Button("Skip extra pick");
            skipExtra.getStyleClass().add("dock-exit");
            skipExtra.setOnAction(event -> controller.skipExtraPick());
            statusRow.getChildren().add(skipExtra);
        }
        statusRow.getChildren().addAll(audioToggleButton(), debugEnd, exit);
        statusRow.getStyleClass().add("player-dock-status-row");
        statusRow.setAlignment(Pos.CENTER);

        dock.getChildren().addAll(statusRow, playerSummaryRows());
        return dock;
    }

    // Builds the button that turns the game audio on and off.
    private Button audioToggleButton() {
        Button button = new Button();
        button.getStyleClass().add("dock-audio-button");
        button.setAccessibleText("Toggle audio");
        button.setTooltip(new Tooltip("Toggle audio"));
        refreshAudioToggleIcon(button);
        GuiAnimations.addHoverScale(button);
        button.setOnAction(event -> {
            GuiAudio.toggleEnabled();
            refreshAudioToggleIcon(button);
        });
        return button;
    }

    // Updates the audio button icon to match the current on/off state.
    private void refreshAudioToggleIcon(Button button) {
        button.setGraphic(GuiIcons.icon(GuiViews.audioIcon(), 16));
    }

    // Builds the player summaries: a simple row if few players, a carousel if many.
    private VBox playerSummaryRows() {
        summaryTargets.clear();
        deckCardTargets.clear();

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

    // Builds a scrollable strip showing a few players at a time with prev/next arrows.
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

    // Builds the empty horizontal box that will hold the player summary cards.
    private HBox playerSummaryStrip() {
        HBox row = new HBox(9);
        row.setAlignment(Pos.CENTER);
        row.getStyleClass().add("player-summary-strip");
        return row;
    }

    // Builds a small left/right arrow button for the carousel.
    private Button carouselButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("player-summary-carousel-button");
        button.setFocusTraversable(false);
        GuiAnimations.addHoverScale(button);
        return button;
    }

    // Keeps the carousel start index inside valid limits.
    private int clampSummaryStart(List<PlayerView> players) {
        int maxStart = Math.max(0, players.size() - MAX_VISIBLE_SUMMARIES);
        if (summaryStartIndex < 0) {
            return 0;
        }
        return Math.min(summaryStartIndex, maxStart);
    }

    // Builds one player's summary card: name, totem, resources, tribe stats and buildings.
    private Node playerSummary(PlayerView player, int playerCount) {
        boolean disconnected = model.isPlayerDisconnected(player.getNickname());
        boolean current = player.getNickname().equals(GuiViews.currentTurnNickname(model));
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
        summaryTargets.put(player.getNickname(), card);

        card.setOnMouseClicked(event -> showTribePanel(player));
        GuiAnimations.addHoverScale(card);

        HBox head = new HBox(7);
        head.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label((me ? "You · " : "") + player.getNickname());
        name.getStyleClass().add("player-summary-name");
        head.getChildren().addAll(playerSummaryTotem(player), name);

        HBox top = new HBox(8, head, GuiViews.spacer(), playerResourceBadges(player));
        top.getStyleClass().add("player-summary-top");
        top.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(top, tribeStatsGrid(player, true));
        Node buildings = buildingSummaryStrip(player);
        if (buildings != null) {
            card.getChildren().add(buildings);
        }

        return card;
    }

    // Builds the small row of the player's building cards (with a "+N" if there are more).
    private Node buildingSummaryStrip(PlayerView player) {
        List<Integer> buildingIds = player.getBuildingCardIds();
        if (buildingIds.isEmpty()) {
            return null;
        }

        HBox strip = new HBox(5);
        strip.getStyleClass().add("player-building-strip");
        strip.setAlignment(Pos.CENTER_LEFT);

        int visibleCount = Math.min(5, buildingIds.size());
        int hiddenCount = buildingIds.size() - visibleCount;
        List<Integer> visibleBuildings = buildingIds.subList(buildingIds.size() - visibleCount, buildingIds.size());
        for (Integer buildingId : visibleBuildings) {
            Node card = stackedMiniCardNode(buildingId, 30, 42);
            rememberDeckCardTarget(player, buildingId, card);
            strip.getChildren().add(card);
        }

        if (hiddenCount > 0) {
            Label more = new Label("+" + hiddenCount);
            more.getStyleClass().add("player-summary-more");
            strip.getChildren().add(more);
        }

        return strip;
    }

    // Opens the big popup with all the details of a player's tribe.
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

    // Builds the popup header: totem, player name and their food/PP.
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

    // Builds the two badges showing the player's food and prestige points.
    private Node playerResourceBadges(PlayerView player) {
        HBox badges = new HBox(7);
        badges.getStyleClass().add("player-resource-badges");
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getChildren().addAll(
                playerResourceBadge(FOOD_BADGE_PATH, player.getFood(), "player-resource-food-value"),
                playerResourceBadge(PRESTIGE_BADGE_PATH, player.getPrestigePoints(), "player-resource-prestige-value")
        );
        return badges;
    }

    // Builds one resource badge: an icon with the number on top of it.
    private Node playerResourceBadge(String iconPath, int value, String valueStyleClass) {
        StackPane badge = new StackPane();
        badge.getStyleClass().add("player-resource-badge");
        badge.setMinSize(PLAYER_RESOURCE_BADGE_SIZE, PLAYER_RESOURCE_BADGE_SIZE);
        badge.setPrefSize(PLAYER_RESOURCE_BADGE_SIZE, PLAYER_RESOURCE_BADGE_SIZE);
        badge.setMaxSize(PLAYER_RESOURCE_BADGE_SIZE, PLAYER_RESOURCE_BADGE_SIZE);

        Node image = resourceBadgeImage(iconPath);
        if (image != null) {
            badge.getChildren().add(image);
        } else {
            badge.getStyleClass().add("player-resource-badge-fallback");
        }

        Label label = new Label(String.valueOf(value));
        label.getStyleClass().add(valueStyleClass);
        if (value < 0) {
            label.getStyleClass().add("player-resource-value-negative");
        }
        if (Math.abs(value) >= 100 || value <= -10) {
            label.getStyleClass().add("player-resource-value-large");
        }
        badge.getChildren().add(label);
        return badge;
    }

    // Loads the icon image for a resource badge (null if missing).
    private Node resourceBadgeImage(String iconPath) {
        Optional<Image> cached = assetCatalog.image(iconPath);
        if (cached.isEmpty()) {
            return null;
        }
        ImageView image = new ImageView(cached.get());
        image.setFitWidth(PLAYER_RESOURCE_BADGE_SIZE);
        image.setFitHeight(PLAYER_RESOURCE_BADGE_SIZE);
        image.setPreserveRatio(true);
        return image;
    }

    // Builds the small totem marker shown next to the player's name.
    private Node playerSummaryTotem(PlayerView player) {
        StackPane marker = totemMarkers.create(player.getTotemColor(), player.getNickname(), 34);
        marker.getStyleClass().add("player-summary-totem");
        return marker;
    }

    // Builds the grid with all the player's character and building cards.
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

    // Builds one small clickable card that opens the card details when clicked.
    private Node miniCardNode(int cardId, double width, double height) {
        StackPane card = new StackPane();
        card.getStyleClass().add("mini-card");
        card.setPrefSize(width, height);
        card.getChildren().add(cardImages.create(cardId, width, height));
        card.setOnMouseClicked(event -> showCardDetails.accept(cardId));
        GuiAnimations.addHoverScale(card);
        return card;
    }

    // Builds a small "label: value" pill (e.g. "Characters: ...").
    private Label summaryPill(String label, String value) {
        Label pill = new Label(label + ": " + value);
        pill.getStyleClass().add("tribe-summary-pill");
        return pill;
    }

    // Builds the grid of tribe stats (one cell per character type), compact or full.
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

    // Adds one stat cell for each character type (gatherer, hunter, builder, shaman, artist, inventor).
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

    // Builds one stat cell: the type icons, the count numbers and (if compact) the card stack.
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

    // Builds the little stacked pile of the player's cards of one character type.
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
            Integer cardId = visibleCards.get(index);
            Node card = stackedMiniCardNode(cardId, cardWidth, cardHeight);
            card.setLayoutX(startX);
            card.setLayoutY(startY + index * step);
            card.setViewOrder(visibleCards.size() - index);
            rememberDeckCardTarget(player, cardId, card);
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

    // Builds one small fixed-size card used inside the stacked piles.
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

    // Finds the on-screen card a deal animation should fly to (used by the animations).
    Node deckTargetFor(String nickname, int cardId) {
        Node cardTarget = deckCardTargets.get(deckTargetKey(nickname, cardId));
        if (cardTarget != null) {
            return cardTarget;
        }
        return summaryTargets.get(nickname);
    }

    // Saves which on-screen node belongs to a player's card, so animations can find it later.
    private void rememberDeckCardTarget(PlayerView player, int cardId, Node target) {
        if (player != null && target != null) {
            deckCardTargets.put(deckTargetKey(player.getNickname(), cardId), target);
        }
    }

    // Builds the map key "nickname:cardId" used to store/find card targets.
    private String deckTargetKey(String nickname, int cardId) {
        return nickname + ":" + cardId;
    }

    // Loads a small stat icon image (or an empty fallback if missing).
    private Node statIcon(String path, double size) {
        Optional<Image> cached = assetCatalog.image(path);
        if (cached.isPresent()) {
            ImageView image = new ImageView(cached.get());
            image.setFitWidth(size);
            image.setFitHeight(size);
            image.setPreserveRatio(true);
            image.getStyleClass().add("tribe-stat-icon");
            return image;
        }

        Region fallback = new Region();
        fallback.getStyleClass().add("tribe-stat-icon-fallback");
        return fallback;
    }

    // Counts how many cards of each kind there are and makes a short text like "A 2 · B 1".
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

    // Builds a small "label + value" status item (e.g. Era, Round, Turn).
    private HBox smallStatus(String label, String value) {
        Label key = new Label(label);
        key.getStyleClass().add("table-status-key");

        Label val = new Label(value);
        val.getStyleClass().add("table-status-value");

        HBox row = new HBox(8, key, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // Builds a small colored dot showing the player's totem color.
    private Region totemDot(PlayerView player) {
        Region dot = new Region();
        dot.getStyleClass().add("totem-dot");
        dot.setStyle("-fx-background-color: " + totemMarkers.colorHex(player.getTotemColor()) + ";");
        return dot;
    }

    // Small interface: something that can build a card image from its id and size.
    @FunctionalInterface
    interface CardImageProvider {
        Node create(int cardId, double width, double height);
    }
}
