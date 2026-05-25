package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public final class EndGameView {
    private static final String PRESTIGE_BADGE_PATH = "gui/endgame_prestige_badge.png";
    private static final String FOOD_BADGE_PATH = "gui/endgame_food_badge.png";
    private static final double PRESTIGE_BADGE_SIZE = 48;
    private static final double FOOD_BADGE_SIZE = 48;

    private final ClientController controller;
    private final ClientModel model;
    private final TotemMarkerFactory totemMarkers;
    private VBox content;
    private VBox standings;
    private VBox winnerBox;
    private boolean introPlayed;

    public EndGameView(ClientController controller, ClientModel model, GuiAssetCatalog assetCatalog) {
        this.controller = controller;
        this.model = model;
        this.totemMarkers = new TotemMarkerFactory(assetCatalog);
    }

    public Node render() {
        if (content != null) {
            refresh();
            return content;
        }

        content = new VBox(26);
        content.setPadding(new Insets(32));
        content.setAlignment(Pos.TOP_CENTER);

        standings = new VBox(6);
        standings.setMaxWidth(560);

        winnerBox = new VBox(2);
        winnerBox.getStyleClass().add("endgame-winner-box");
        winnerBox.setAlignment(Pos.CENTER);

        content.getChildren().addAll(titleLabel(), standings, winnerBox, disconnectButton());
        refresh();
        return content;
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

        String winner = sorted.get(0).getKey();
        standings.getChildren().add(tableHeader());

        int rank = 1;
        for (Map.Entry<String, Integer> e : sorted) {
            standings.getChildren().add(scoreRow(rank, e.getKey(), e.getValue()));
            rank++;
        }

        winnerBox.getChildren().addAll(winnerSectionLabel(), winnerStage(winner));
        playIntroOnce();
    }

    private Label titleLabel() {
        Label label = new Label("MESOS");
        label.getStyleClass().add("endgame-title");
        return label;
    }

    private Label winnerSectionLabel() {
        Label label = new Label("WINNER");
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
                headerCell("FOOD", 120)
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

        HBox row = new HBox(nameCell, prestigeCell, foodCell);
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

        try (InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(PRESTIGE_BADGE_PATH)) {
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

        try (InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(FOOD_BADGE_PATH)) {
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

    private Button disconnectButton() {
        Button quit = new Button("Esci");
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
        PlayerView pv = model.getPlayers().get(nickname);
        return pv != null ? pv.getFood() : 0;
    }

    private TotemColor totemOf(String nickname) {
        PlayerView pv = model.getPlayers().get(nickname);
        return pv != null ? pv.getTotemColor() : null;
    }

    private String totemColorHex(TotemColor color) {
        return switch (color == null ? TotemColor.WHITE : color) {
            case RED    -> "#e05a46";
            case BLUE   -> "#5b9fd4";
            case PURPLE -> "#a070cc";
            case YELLOW -> "#e8be5a";
            case WHITE  -> "#f3ebdc";
        };
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
        region.setMinSize(width, height);
        region.setPrefSize(width, height);
        region.setMaxSize(width, height);
    }
}
