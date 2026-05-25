package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.view.gui.util.GuiIcons;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;

import java.util.List;
import java.util.Map;

public final class EndGameView {
    private final ClientController controller;
    private final ClientModel model;
    private VBox content;
    private VBox standings;

    public EndGameView(ClientController controller, ClientModel model) {
        this.controller = controller;
        this.model = model;
    }

    public Node render() {
        if (content != null) {
            refresh();
            return content;
        }

        content = new VBox(20);
        content.setPadding(new Insets(32));
        content.setAlignment(Pos.TOP_CENTER);

        standings = new VBox(6);
        standings.setMaxWidth(560);

        content.getChildren().addAll(standings, disconnectButton());
        refresh();
        return content;
    }

    public void refresh() {
        if (standings == null) return;
        standings.getChildren().clear();

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
        standings.getChildren().add(winnerSectionLabel());
        standings.getChildren().add(winnerNameLabel(winner));
        standings.getChildren().add(tableHeader());

        int rank = 1;
        for (Map.Entry<String, Integer> e : sorted) {
            standings.getChildren().add(scoreRow(rank, e.getKey(), e.getValue()));
            rank++;
        }
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

        Label ppLabel = new Label(score + " ★");
        ppLabel.getStyleClass().add("endgame-stat");
        ppLabel.setStyle("-fx-text-fill: " + hex + ";");
        ppLabel.setMinWidth(120);
        ppLabel.setAlignment(Pos.CENTER);

        Label foodLabel = new Label(food + " 🍖");
        foodLabel.getStyleClass().add("endgame-stat");
        foodLabel.setMinWidth(120);
        foodLabel.setAlignment(Pos.CENTER);

        HBox row = new HBox(nameCell, ppLabel, foodLabel);
        row.getStyleClass().add("endgame-score-row");
        if (rank == 1) row.getStyleClass().add("endgame-score-row-first");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        return row;
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
        Button quit = new Button("Quit");
        quit.getStyleClass().add("secondary-button");
        GuiIcons.decorateButton(quit, MaterialDesignL.LOGOUT);
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
}
