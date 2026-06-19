package it.polimi.ingsw.view.gui.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.polimi.ingsw.controller.leaderboard.LeaderboardEntry;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.network.protocol.clientbound.EventResolvedMessage;
import it.polimi.ingsw.view.EndGameHelper;
import it.polimi.ingsw.view.card.BuildingEffect;
import it.polimi.ingsw.view.card.BuildingScoring;
import it.polimi.ingsw.view.card.CharacterType;
import it.polimi.ingsw.view.card.EventType;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import it.polimi.ingsw.view.gui.util.TotemPalette;
import it.polimi.ingsw.view.gui.util.GuiAnimations;
import it.polimi.ingsw.view.gui.util.GuiIcons;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashSet;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EndGameView {
    private static final String PRESTIGE_BADGE_PATH = "assets/icons/endgame_prestige_badge.png";
    private static final String FOOD_BADGE_PATH = "assets/icons/engame_food_badge_prova.png";
    private static final String ICON_BUILDER = "assets/icons/summary/builder.png";
    private static final String ICON_ARTIST = "assets/icons/summary/artist.png";
    private static final String ICON_INVENTOR = "assets/icons/summary/inventor.png";
    private static final double PRESTIGE_BADGE_SIZE = 48;
    private static final double FOOD_BADGE_SIZE = 48;
    private static final double BREAKDOWN_ICON_SIZE = 28;

    private final ClientController controller;
    private final ClientModel model;
    private final GuiAssetCatalog assetCatalog;
    private final TotemMarkerFactory totemMarkers;
    private final LeaderboardView leaderboardView;
    private StackPane root;
    private StackPane breakdownLayer;
    private VBox content;
    private VBox standings;
    private VBox winnerBox;
    private boolean introPlayed;

    public EndGameView(ClientController controller, ClientModel model, GuiAssetCatalog assetCatalog) {
        this.controller = controller;
        this.model = model;
        this.assetCatalog = assetCatalog;
        this.totemMarkers = new TotemMarkerFactory(assetCatalog);
        this.leaderboardView = new LeaderboardView();
    }

    public Node render() {
        if (root != null) {
            refresh();
            return root;
        }

        content = new VBox(26);
        content.setPadding(new Insets(32));
        content.setAlignment(Pos.TOP_CENTER);

        standings = new VBox(6);
        standings.setMaxWidth(720);

        winnerBox = new VBox(2);
        winnerBox.getStyleClass().add("endgame-winner-box");
        winnerBox.setAlignment(Pos.CENTER);

        breakdownLayer = new StackPane();
        breakdownLayer.getStyleClass().add("endgame-breakdown-layer");
        breakdownLayer.setVisible(false);
        breakdownLayer.setManaged(false);
        breakdownLayer.setPickOnBounds(false);

        content.getChildren().addAll(titleHeader(), standings,
                leaderboardView.render(breakdownLayer), winnerBox, disconnectButton());

        root = new StackPane(content, breakdownLayer);
        StackPane.setAlignment(content, Pos.TOP_CENTER);
        StackPane.setAlignment(breakdownLayer, Pos.CENTER);
        refresh();
        return root;
    }

    public void setLeaderboardRank(int rank, int playersCount) {
        leaderboardView.setMyRank(rank, playersCount);
    }

    public void setLeaderboardPlayersCount(int playersCount) {
        leaderboardView.setPlayersCount(playersCount);
    }

    public void setLeaderboardEntries(int playersCount, List<LeaderboardEntry> entries) {
        leaderboardView.setEntries(playersCount, entries);
    }

    public void refresh() {
        if (standings == null || winnerBox == null) return;
        standings.getChildren().clear();
        winnerBox.getChildren().clear();

        Map<String, Integer> scores = model.getFinalScores();
        if (scores.isEmpty()) return;

        List<Map.Entry<String, Integer>> sorted = scores.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    if (cmp != 0) return cmp;
                    return Integer.compare(foodOf(b.getKey()), foodOf(a.getKey()));
                })
                .toList();

        List<String> winners = EndGameHelper.resolveWinners(model);

        standings.getChildren().add(tableHeader());

        int rank = 1;
        for (Map.Entry<String, Integer> e : sorted) {
            standings.getChildren().add(scoreRow(rank, e.getKey(), e.getValue()));
            rank++;
        }

        winnerBox.getChildren().add(winnerSectionLabel(winners.size() > 1));
        for (String winner : winners) {
            winnerBox.getChildren().add(winnerStage(winner));
        }
        playIntroOnce();
    }

    private Label titleLabel() {
        Label label = new Label("MESOS");
        label.getStyleClass().add("endgame-title");
        return label;
    }

    private Node titleHeader() {
        StackPane header = new StackPane();
        header.setMaxWidth(Double.MAX_VALUE);

        Label title = titleLabel();
        Button audio = GuiViews.menuAudioToggleButton();
        audio.getStyleClass().add("endgame-audio-button");

        header.getChildren().addAll(title, audio);
        StackPane.setAlignment(title, Pos.CENTER);
        StackPane.setAlignment(audio, Pos.CENTER_RIGHT);
        StackPane.setMargin(audio, new Insets(0, 10, 0, 0));
        return header;
    }

    private Label winnerSectionLabel(boolean tie) {
        Label label = new Label(tie ? "WINNERS" : "WINNER");
        label.getStyleClass().add("endgame-winner-section");
        return label;
    }

    private Label winnerNameLabel(String winner) {
        Label label = new Label(winner.toUpperCase());
        label.getStyleClass().add("endgame-winner-name");
        label.setStyle("-fx-text-fill: " + totemColorHex(totemOf(winner)) + ";");
        return label;
    }

    private StackPane winnerStage(String winner) {
        Region glow = new Region();
        glow.getStyleClass().add("endgame-winner-glow");

        HBox winnerRow = new HBox(18);
        winnerRow.getStyleClass().add("endgame-winner-row");
        winnerRow.setAlignment(Pos.CENTER);
        winnerRow.getChildren().addAll(
                totemMarkers.create(totemOf(winner), winner, 82),
                winnerNameLabel(winner)
        );

        return new StackPane(glow, winnerRow);
    }

    private HBox tableHeader() {
        HBox row = new HBox();
        row.getStyleClass().add("endgame-header-row");
        row.getChildren().addAll(
                headerCell("PLAYER", 260),
                headerCell("PP", 120),
                headerCell("FOOD", 120),
                headerCell("DETAILS", 160)
        );
        return row;
    }

    private HBox scoreRow(int rank, String nickname, int score) {
        String hex = totemColorHex(totemOf(nickname));
        int food = foodOf(nickname);

        Label rankLabel = new Label(rank + ".");
        rankLabel.getStyleClass().add("endgame-rank");
        rankLabel.setMinWidth(28);

        Label nameLabel = new Label(nickname.toUpperCase());
        nameLabel.getStyleClass().add("endgame-name");
        nameLabel.setStyle("-fx-text-fill: " + hex + ";");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox nameCell = new HBox(6, rankLabel, nameLabel, spacer);
        nameCell.setAlignment(Pos.CENTER_LEFT);
        nameCell.setMinWidth(260);
        nameCell.setPrefWidth(260);

        HBox prestigeCell = new HBox(prestigeBadge(score));
        prestigeCell.getStyleClass().add("endgame-prestige-cell");
        prestigeCell.setMinWidth(120);
        prestigeCell.setPrefWidth(120);
        prestigeCell.setAlignment(Pos.CENTER);

        HBox foodCell = new HBox(foodBadge(food));
        foodCell.getStyleClass().add("endgame-food-cell");
        foodCell.setMinWidth(120);
        foodCell.setPrefWidth(120);
        foodCell.setAlignment(Pos.CENTER);

        Button breakdownButton = new Button("Score breakdown");
        breakdownButton.getStyleClass().add("endgame-breakdown-button");
        GuiAnimations.addHoverScale(breakdownButton);
        breakdownButton.setOnAction(event -> showBreakdown(nickname, score));

        HBox breakdownCell = new HBox(breakdownButton);
        breakdownCell.getStyleClass().add("endgame-breakdown-cell");
        breakdownCell.setMinWidth(160);
        breakdownCell.setPrefWidth(160);
        breakdownCell.setAlignment(Pos.CENTER);

        HBox row = new HBox(nameCell, prestigeCell, foodCell, breakdownCell);
        row.getStyleClass().add("endgame-score-row");
        if (rank == 1) row.getStyleClass().add("endgame-score-row-first");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        return row;
    }

    private Node prestigeBadge(int score) {
        StackPane badge = new StackPane();
        badge.getStyleClass().add("endgame-prestige-badge");
        setFixedSize(badge, PRESTIGE_BADGE_SIZE, PRESTIGE_BADGE_SIZE);

        try (InputStream stream = assetCatalog.openResource(PRESTIGE_BADGE_PATH).orElse(null)) {
            if (stream != null) {
                ImageView image = new ImageView(new Image(stream));
                image.setPreserveRatio(true);
                image.setFitWidth(PRESTIGE_BADGE_SIZE);
                image.setFitHeight(PRESTIGE_BADGE_SIZE);
                badge.getChildren().add(image);
            } else {
                badge.getStyleClass().add("endgame-prestige-fallback");
            }
        } catch (Exception ignored) {
            badge.getStyleClass().add("endgame-prestige-fallback");
        }

        Label value = new Label(String.valueOf(score));
        value.getStyleClass().add("endgame-prestige-value");
        if (Math.abs(score) >= 100) {
            value.getStyleClass().add("endgame-prestige-value-large");
        }
        badge.getChildren().add(value);
        return badge;
    }

    private Node foodBadge(int food) {
        StackPane badge = new StackPane();
        badge.getStyleClass().add("endgame-food-badge");
        setFixedSize(badge, FOOD_BADGE_SIZE, FOOD_BADGE_SIZE);

        try (InputStream stream = assetCatalog.openResource(FOOD_BADGE_PATH).orElse(null)) {
            if (stream != null) {
                ImageView image = new ImageView(new Image(stream));
                image.setPreserveRatio(true);
                image.setFitWidth(FOOD_BADGE_SIZE);
                image.setFitHeight(FOOD_BADGE_SIZE);
                badge.getChildren().add(image);
            } else {
                badge.getStyleClass().add("endgame-food-fallback");
            }
        } catch (Exception ignored) {
            badge.getStyleClass().add("endgame-food-fallback");
        }

        Label value = new Label(String.valueOf(food));
        value.getStyleClass().add("endgame-food-value");
        if (Math.abs(food) >= 100) {
            value.getStyleClass().add("endgame-food-value-large");
        }
        badge.getChildren().add(value);
        return badge;
    }

    private Label headerCell(String text, double width) {
        Label label = new Label(text);
        label.getStyleClass().add("endgame-header-cell");
        label.setMinWidth(width);
        label.setPrefWidth(width);
        if (!text.equals("PLAYER")) label.setAlignment(Pos.CENTER);
        return label;
    }

    private void showBreakdown(String nickname, int finalScore) {
        if (breakdownLayer == null) return;

        VBox panel = new VBox(12);
        panel.getStyleClass().add("endgame-breakdown-panel");
        panel.setMaxWidth(760);
        panel.maxHeightProperty().bind(breakdownLayer.heightProperty().subtract(64));
        panel.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Score breakdown");
        title.getStyleClass().add("endgame-breakdown-title");

        Label subtitle = new Label(nickname.toUpperCase());
        subtitle.getStyleClass().add("endgame-breakdown-subtitle");
        subtitle.setStyle("-fx-text-fill: " + totemColorHex(totemOf(nickname)) + ";");

        VBox rows = new VBox(0);
        rows.getStyleClass().add("endgame-breakdown-table");

        for (ScoreBreakdownRow row : scoreBreakdown(nickname, finalScore)) {
            rows.getChildren().add(breakdownRow(row, false));
        }
        rows.getChildren().add(breakdownRow(new ScoreBreakdownRow("Punteggio totale", finalScore, "Finale",
                PRESTIGE_BADGE_PATH, -1), true));

        ScrollPane scroll = new ScrollPane(rows);
        scroll.getStyleClass().add("endgame-breakdown-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPannable(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button close = new Button("Chiudi");
        close.getStyleClass().addAll("secondary-button", "endgame-breakdown-close");
        GuiAnimations.addHoverScale(close);
        close.setOnAction(event -> hideBreakdown());

        panel.getChildren().addAll(title, subtitle, scroll, close);
        panel.setOpacity(0);
        panel.setScaleX(0.97);
        panel.setScaleY(0.97);

        breakdownLayer.getChildren().setAll(panel);
        breakdownLayer.setVisible(true);
        breakdownLayer.setManaged(true);

        FadeTransition fade = new FadeTransition(Duration.millis(180), panel);
        fade.setFromValue(0);
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(180), panel);
        scale.setToX(1);
        scale.setToY(1);
        new ParallelTransition(fade, scale).play();
    }

    private void hideBreakdown() {
        if (breakdownLayer == null || breakdownLayer.getChildren().isEmpty()) return;
        Node panel = breakdownLayer.getChildren().getFirst();

        FadeTransition fade = new FadeTransition(Duration.millis(140), panel);
        fade.setFromValue(panel.getOpacity());
        fade.setToValue(0);
        ScaleTransition scale = new ScaleTransition(Duration.millis(140), panel);
        scale.setToX(0.97);
        scale.setToY(0.97);
        ParallelTransition transition = new ParallelTransition(fade, scale);
        transition.setOnFinished(event -> {
            breakdownLayer.getChildren().clear();
            breakdownLayer.setVisible(false);
            breakdownLayer.setManaged(false);
        });
        transition.play();
    }

    private HBox breakdownRow(ScoreBreakdownRow item, boolean total) {
        Label label = new Label(item.label());
        label.getStyleClass().add("endgame-breakdown-label");

        Label detail = new Label(item.detail());
        detail.getStyleClass().add("endgame-breakdown-detail");

        VBox text = new VBox(2, label, detail);
        text.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label points = new Label(total ? String.valueOf(item.points()) : formatPoints(item.points()));
        points.getStyleClass().add("endgame-breakdown-points");

        HBox row = new HBox(12, breakdownIcon(item), text, points);
        row.getStyleClass().add(total ? "endgame-breakdown-row-total" : "endgame-breakdown-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Node breakdownIcon(ScoreBreakdownRow item) {
        if (item.cardId() > 0) {
            return assetCatalog.findCard(item.cardId())
                    .map(card -> imageIcon(card.getFrontPath(), 30, 42, "endgame-breakdown-card-icon"))
                    .orElseGet(this::breakdownIconFallback);
        }
        if (item.iconPath() != null && !item.iconPath().isBlank()) {
            return imageIcon(item.iconPath(), BREAKDOWN_ICON_SIZE, BREAKDOWN_ICON_SIZE, "endgame-breakdown-icon");
        }
        return breakdownIconFallback();
    }

    private Node imageIcon(String resourcePath, double width, double height, String styleClass) {
        try (InputStream stream = assetCatalog.openResource(resourcePath).orElse(null)) {
            if (stream != null) {
                ImageView image = new ImageView(new Image(stream));
                image.setFitWidth(width);
                image.setFitHeight(height);
                image.setPreserveRatio(true);
                image.getStyleClass().add(styleClass);
                return image;
            }
        } catch (Exception ignored) {
            // Fallback node below.
        }
        return breakdownIconFallback();
    }

    private Node breakdownIconFallback() {
        Region fallback = new Region();
        fallback.getStyleClass().add("endgame-breakdown-icon-fallback");
        return fallback;
    }

    private List<ScoreBreakdownRow> scoreBreakdown(String nickname, int finalScore) {
        PlayerView player = model.getPlayers().get(nickname);
        List<ScoreBreakdownRow> rows = new ArrayList<>();
        int subtotal = 0;

        List<ScoreBreakdownRow> eventRows = eventBreakdownRows(nickname);
        rows.addAll(eventRows);
        subtotal += sumPoints(eventRows);

        int artists = artistPoints(player);
        rows.add(new ScoreBreakdownRow("Artists", artists, "10 PP per pair of Artists", ICON_ARTIST, -1));
        subtotal += artists;

        int inventors = inventorPoints(player);
        rows.add(new ScoreBreakdownRow("Inventors", inventors, "Inventors × distinct invention icons", ICON_INVENTOR, -1));
        subtotal += inventors;

        int builders = builderCharacterPoints(player);
        rows.add(new ScoreBreakdownRow("Builders", builders, "Printed PP on Builder cards", ICON_BUILDER, -1));
        subtotal += builders;

        List<ScoreBreakdownRow> buildingRows = buildingBreakdownRows(player);
        rows.addAll(buildingRows);
        subtotal += sumPoints(buildingRows);

        int adjustment = finalScore - subtotal;
        if (adjustment != 0) {
            rows.add(new ScoreBreakdownRow("Other", adjustment, "Turn tiles or points not detailed by events",
                    PRESTIGE_BADGE_PATH, -1));
        }
        return rows;
    }

    private List<ScoreBreakdownRow> eventBreakdownRows(String nickname) {
        List<ScoreBreakdownRow> rows = new ArrayList<>();
        for (EventResolvedMessage event : model.getResolvedEvents()) {
            EventResolvedMessage.PlayerEffect effect = eventEffectFor(event, nickname);
            int points = effect != null ? effect.getPointsDelta() : 0;
            int food = effect != null ? effect.getFoodDelta() : 0;
            String title = nullSafe(event.getTitle());
            String label = "Event - " + title;
            String detail = "Card #" + event.getCardId();
            if (food != 0) {
                detail += " | Food " + formatPoints(food);
            }
            rows.add(new ScoreBreakdownRow(label, points, detail, eventIconPath(event.getCardId()), -1));
        }
        return rows;
    }

    private EventResolvedMessage.PlayerEffect eventEffectFor(EventResolvedMessage event, String nickname) {
        if (event.getEffects() == null) return null;
        for (EventResolvedMessage.PlayerEffect effect : event.getEffects()) {
            if (nickname.equals(effect.getNickname())) return effect;
        }
        return null;
    }

    private List<ScoreBreakdownRow> buildingBreakdownRows(PlayerView player) {
        List<ScoreBreakdownRow> rows = new ArrayList<>();
        if (player == null) return rows;
        for (int cardId : player.getBuildingCardIds()) {
            JsonObject details = assetCatalog.findCardDetails(cardId).orElse(null);
            int printed = intDetail(details, "pointsEndOfMatch");
            int bonus = details != null ? endGameEffectPoints(player, details.get("effect")) : 0;
            String detail = "Printed " + printed + " PP";
            if (bonus != 0) {
                detail += " | Bonus " + formatPoints(bonus) + " PP";
            }
            rows.add(new ScoreBreakdownRow(buildingLabel(cardId, details), printed + bonus, detail,
                    buildingIconPath(details), cardId));
        }
        return rows;
    }

    private int sumPoints(List<ScoreBreakdownRow> rows) {
        int sum = 0;
        for (ScoreBreakdownRow row : rows) {
            sum += row.points();
        }
        return sum;
    }

    private int artistPoints(PlayerView player) {
        return (characterTypeCount(player, "artist") / 2) * 10;
    }

    private int inventorPoints(PlayerView player) {
        if (player == null) return 0;
        int inventors = 0;
        Set<String> icons = new HashSet<>();
        for (int cardId : player.getCharacterCardIds()) {
            JsonObject details = assetCatalog.findCardDetails(cardId).orElse(null);
            if (!"inventor".equals(stringDetail(details, "type"))) continue;
            inventors++;
            String icon = stringDetail(details, "inventionIcon");
            if (!icon.isBlank()) icons.add(icon);
        }
        return inventors * icons.size();
    }

    private int builderCharacterPoints(PlayerView player) {
        if (player == null) return 0;
        int points = 0;
        for (int cardId : player.getCharacterCardIds()) {
            JsonObject details = assetCatalog.findCardDetails(cardId).orElse(null);
            if ("builder".equals(stringDetail(details, "type"))) {
                points += intDetail(details, "pointsEndOfMatch");
            }
        }
        return points;
    }

    private int endGameEffectPoints(PlayerView player, JsonElement effect) {
        return BuildingEffect.from(effectName(effect)).endGamePoints(effect, scoringFor(player));
    }

    private BuildingScoring scoringFor(PlayerView player) {
        return new BuildingScoring() {
            @Override
            public int builderCharacterPoints() {
                return EndGameView.this.builderCharacterPoints(player);
            }

            @Override
            public int fullSetCount() {
                return EndGameView.this.fullSetCount(player);
            }

            @Override
            public int characterTypeCount(String characterType) {
                return EndGameView.this.characterTypeCount(player, characterType);
            }
        };
    }

    private int fullSetCount(PlayerView player) {
        if (player == null) return 0;
        int fullSets = Integer.MAX_VALUE;
        for (CharacterType type : CharacterType.playable()) {
            fullSets = Math.min(fullSets, characterTypeCount(player, type.token()));
        }
        return fullSets == Integer.MAX_VALUE ? 0 : fullSets;
    }

    private int characterTypeCount(PlayerView player, String type) {
        if (player == null || type == null || type.isBlank()) return 0;
        int count = 0;
        for (int cardId : player.getCharacterCardIds()) {
            JsonObject details = assetCatalog.findCardDetails(cardId).orElse(null);
            if (type.equals(stringDetail(details, "type"))) count++;
        }
        return count;
    }

    private String eventIconPath(int cardId) {
        String type = assetCatalog.findCardDetails(cardId)
                .map(details -> stringDetail(details, "type"))
                .orElse("");
        return EventType.from(type).iconPath();
    }

    private String buildingIconPath(JsonObject details) {
        JsonElement effect = details != null ? details.get("effect") : null;
        return BuildingEffect.from(effectName(effect)).iconPath(effect);
    }

    private String buildingLabel(int cardId, JsonObject details) {
        JsonElement effect = details != null ? details.get("effect") : null;
        return BuildingEffect.from(effectName(effect)).label(effect, cardId);
    }

    private String formatPoints(int points) {
        if (points > 0) return "+" + points;
        return String.valueOf(points);
    }

    private String nullSafe(String text) {
        return text == null || text.isBlank() ? "Event" : text;
    }

    private String stringDetail(JsonObject object, String key) {
        if (object == null) return "";
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) return "";
        return value.getAsString();
    }

    private int intDetail(JsonObject object, String key) {
        if (object == null) return 0;
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) return 0;
        return value.getAsInt();
    }

    private String effectName(JsonElement effect) {
        if (effect == null || effect.isJsonNull()) return "";
        if (effect.isJsonPrimitive()) return effect.getAsString();
        JsonObject object = effect.getAsJsonObject();
        return stringDetail(object, "name");
    }


    private Button disconnectButton() {
        Button quit = new Button("Quit");
        quit.getStyleClass().addAll("secondary-button", "endgame-quit-button");
        GuiIcons.decorateButton(quit, MaterialDesignL.LOGOUT);
        GuiAnimations.addHoverScale(quit);
        quit.setOnAction(event -> {
            controller.disconnect();
            Platform.exit();
        });
        return quit;
    }

    private int foodOf(String nickname) {
        // Prefer server-authoritative finalFood over the cached PlayerView value.
        Map<String, Integer> finalFood = model.getFinalFood();
        if (finalFood.containsKey(nickname)) return finalFood.get(nickname);
        PlayerView pv = model.getPlayers().get(nickname);
        return pv != null ? pv.getFood() : 0;
    }

    private TotemColor totemOf(String nickname) {
        PlayerView pv = model.getPlayers().get(nickname);
        return pv != null ? pv.getTotemColor() : null;
    }

    private String totemColorHex(TotemColor color) {
        return TotemPalette.textHex(color);
    }

    private void playIntroOnce() {
        if (introPlayed || content == null || standings == null || winnerBox == null) {
            return;
        }
        introPlayed = true;

        content.setOpacity(0);
        standings.setTranslateY(-12);
        winnerBox.setTranslateY(16);
        winnerBox.setScaleX(0.96);
        winnerBox.setScaleY(0.96);

        FadeTransition fade = new FadeTransition(Duration.millis(420), content);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition tableMove = new TranslateTransition(Duration.millis(420), standings);
        tableMove.setToY(0);

        TranslateTransition winnerMove = new TranslateTransition(Duration.millis(520), winnerBox);
        winnerMove.setToY(0);

        ScaleTransition winnerScale = new ScaleTransition(Duration.millis(520), winnerBox);
        winnerScale.setToX(1);
        winnerScale.setToY(1);

        new ParallelTransition(fade, tableMove, winnerMove, winnerScale).play();
    }

    private void setFixedSize(Region region, double width, double height) {
        GuiViews.setFixedSize(region, width, height);
    }

    private record ScoreBreakdownRow(String label, int points, String detail, String iconPath, int cardId) {}
}
