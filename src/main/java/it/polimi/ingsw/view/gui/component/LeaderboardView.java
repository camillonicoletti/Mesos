package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.controller.leaderboard.LeaderboardEntry;
import it.polimi.ingsw.view.gui.util.GuiAnimations;
import it.polimi.ingsw.view.gui.util.GuiIcons;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Shows the leaderboard: my position plus a popup with the full ranking list.
public final class LeaderboardView {
    private static final double LEADERBOARD_INFO_WIDTH = 500;
    private static final double LEADERBOARD_ACTION_WIDTH = 160;
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault());

    private StackPane overlayLayer;
    private VBox compactRoot;
    private Label rankLabel;
    private Label statusLabel;
    private Button openButton;
    private Integer rank;
    private int rankPlayersCount;
    private int pendingPlayersCount;
    private int entriesPlayersCount;
    private List<LeaderboardEntry> entries;

    public Node render(StackPane overlayLayer) {
        this.overlayLayer = overlayLayer;
        if (compactRoot == null) {
            compactRoot = createCompactRoot();
        }
        refreshCompact();
        return compactRoot;
    }

    public void setMyRank(int rank, int playersCount) {
        this.rank = rank;
        this.rankPlayersCount = playersCount;
        refreshCompact();
    }

    public void setPlayersCount(int playersCount) {
        if (playersCount > 0) {
            this.pendingPlayersCount = playersCount;
        }
        refreshCompact();
    }

    public void setEntries(int playersCount, List<LeaderboardEntry> entries) {
        this.entriesPlayersCount = playersCount;
        this.entries = entries == null ? List.of() : new ArrayList<>(entries);
        refreshCompact();
    }

    private VBox createCompactRoot() {
        rankLabel = new Label();
        rankLabel.getStyleClass().add("endgame-leaderboard-rank");

        statusLabel = new Label();
        statusLabel.getStyleClass().add("endgame-leaderboard-status");

        VBox text = new VBox(2, rankLabel, statusLabel);
        text.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);

        openButton = new Button("View");
        openButton.getStyleClass().addAll("secondary-button", "endgame-leaderboard-button");
        GuiIcons.decorateButton(openButton, MaterialDesignT.TABLE_STAR);
        GuiAnimations.addHoverScale(openButton);
        openButton.setOnAction(event -> showLeaderboardOverlay());

        HBox titleCell = new HBox(12, GuiIcons.icon(MaterialDesignC.CROWN, 22, "#d7a93d"), text);
        titleCell.setAlignment(Pos.CENTER_LEFT);
        titleCell.setMinWidth(LEADERBOARD_INFO_WIDTH);
        titleCell.setPrefWidth(LEADERBOARD_INFO_WIDTH);

        HBox buttonCell = new HBox(openButton);
        buttonCell.setAlignment(Pos.CENTER);
        buttonCell.setMinWidth(LEADERBOARD_ACTION_WIDTH);
        buttonCell.setPrefWidth(LEADERBOARD_ACTION_WIDTH);

        HBox row = new HBox(titleCell, buttonCell);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(row);
        root.getStyleClass().add("endgame-leaderboard-compact");
        root.setMaxWidth(720);
        return root;
    }

    private void refreshCompact() {
        if (compactRoot == null) {
            return;
        }

        boolean hasRank = rank != null;
        boolean hasEntries = entries != null;
        boolean hasPlayersCount = pendingPlayersCount > 0;
        compactRoot.setVisible(hasRank || hasEntries || hasPlayersCount);
        compactRoot.setManaged(hasRank || hasEntries || hasPlayersCount);

        int playersCount = displayedPlayersCount();
        if (playersCount > 0) {
            rankLabel.setText("Leaderboard (" + playersCount + " players)");
        } else {
            rankLabel.setText("");
        }

        if (hasRank) {
            statusLabel.setText("You finished #" + rank + " in the "
                    + rankPlayersCount + "-player leaderboard.");
            openButton.setDisable(!hasEntries);
        } else if (!hasEntries) {
            statusLabel.setText("Loading top 10...");
            openButton.setDisable(true);
        } else if (entries.isEmpty()) {
            statusLabel.setText("No matches recorded yet.");
            openButton.setDisable(false);
        } else {
            statusLabel.setText("Top 10 available.");
            openButton.setDisable(false);
        }
    }

    private void showLeaderboardOverlay() {
        if (overlayLayer == null || entries == null) {
            return;
        }

        VBox panel = new VBox(14);
        panel.getStyleClass().add("endgame-leaderboard-panel");
        panel.setMaxWidth(760);
        panel.maxHeightProperty().bind(overlayLayer.heightProperty().subtract(64));
        panel.setAlignment(Pos.TOP_CENTER);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Leaderboard");
        title.getStyleClass().add("endgame-leaderboard-title");

        Label subtitle = new Label("(" + entriesPlayersCount + " players)");
        subtitle.getStyleClass().add("endgame-leaderboard-subtitle");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = new Button();
        close.getStyleClass().addAll("secondary-button", "endgame-leaderboard-close");
        close.setGraphic(GuiIcons.icon(MaterialDesignC.CLOSE, 16));
        close.setOnAction(event -> hideLeaderboardOverlay());

        header.getChildren().addAll(title, subtitle, spacer, close);

        ScrollPane scroll = new ScrollPane(leaderboardTable());
        scroll.getStyleClass().add("endgame-leaderboard-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPannable(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        panel.getChildren().addAll(header, scroll);
        panel.setOpacity(0);
        panel.setScaleX(0.97);
        panel.setScaleY(0.97);

        overlayLayer.getChildren().setAll(panel);
        overlayLayer.setVisible(true);
        overlayLayer.setManaged(true);

        FadeTransition fade = new FadeTransition(Duration.millis(180), panel);
        fade.setFromValue(0);
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(180), panel);
        scale.setToX(1);
        scale.setToY(1);
        new ParallelTransition(fade, scale).play();
    }

    private VBox leaderboardTable() {
        VBox table = new VBox(0);
        table.getStyleClass().add("endgame-leaderboard-table");
        table.getChildren().add(tableHeader());

        if (entries.isEmpty()) {
            Label empty = new Label("No matches recorded yet.");
            empty.getStyleClass().add("endgame-leaderboard-empty");
            table.getChildren().add(empty);
            return table;
        }

        for (LeaderboardEntry entry : entries) {
            table.getChildren().add(tableRow(entry));
        }
        return table;
    }

    private HBox tableHeader() {
        HBox row = new HBox();
        row.getStyleClass().add("endgame-leaderboard-header-row");
        row.getChildren().addAll(
                headerCell("RANK", 88),
                headerCell("NICKNAME", 260),
                headerCell("SCORE", 120),
                headerCell("DATE", 220)
        );
        return row;
    }

    private HBox tableRow(LeaderboardEntry entry) {
        HBox row = new HBox();
        row.getStyleClass().add("endgame-leaderboard-row");
        if (entry.rank() == 1) {
            row.getStyleClass().add("endgame-leaderboard-row-first");
        }
        row.getChildren().addAll(
                cell("#" + entry.rank(), 88, "endgame-leaderboard-rank-cell", Pos.CENTER_LEFT),
                cell(truncate(entry.nickname(), 22), 260, "endgame-leaderboard-name-cell", Pos.CENTER_LEFT),
                cell(String.valueOf(entry.finalScore()), 120, "endgame-leaderboard-score-cell", Pos.CENTER),
                cell(formatPlayedAt(entry), 220, "endgame-leaderboard-date-cell", Pos.CENTER)
        );
        return row;
    }

    private Label headerCell(String text, double width) {
        Label label = new Label(text);
        label.getStyleClass().add("endgame-leaderboard-header-cell");
        label.setMinWidth(width);
        label.setPrefWidth(width);
        if (!"NICKNAME".equals(text)) {
            label.setAlignment(Pos.CENTER);
        }
        return label;
    }

    private Label cell(String text, double width, String styleClass, Pos alignment) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.setAlignment(alignment);
        return label;
    }

    private String formatPlayedAt(LeaderboardEntry entry) {
        return entry.playedAt() == null ? "-" : DATE_FORMAT.format(entry.playedAt());
    }

    private int displayedPlayersCount() {
        if (rankPlayersCount > 0) {
            return rankPlayersCount;
        }
        if (entriesPlayersCount > 0) {
            return entriesPlayersCount;
        }
        return pendingPlayersCount;
    }

    private void hideLeaderboardOverlay() {
        if (overlayLayer == null || overlayLayer.getChildren().isEmpty()) {
            return;
        }
        Node panel = overlayLayer.getChildren().getFirst();

        FadeTransition fade = new FadeTransition(Duration.millis(140), panel);
        fade.setFromValue(panel.getOpacity());
        fade.setToValue(0);
        ScaleTransition scale = new ScaleTransition(Duration.millis(140), panel);
        scale.setToX(0.97);
        scale.setToY(0.97);
        ParallelTransition transition = new ParallelTransition(fade, scale);
        transition.setOnFinished(event -> {
            overlayLayer.getChildren().clear();
            overlayLayer.setVisible(false);
            overlayLayer.setManaged(false);
        });
        transition.play();
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
