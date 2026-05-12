package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.view.gui.util.GuiIcons;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;

import java.util.Comparator;
import java.util.Map;

public final class EndGameView {
    private final ClientController controller;
    private final ClientModel model;
    private VBox content;
    private VBox scores;

    public EndGameView(ClientController controller, ClientModel model) {
        this.controller = controller;
        this.model = model;
    }

    public Node render() {
        if (content != null) {
            refresh();
            return content;
        }

        content = new VBox(14);
        content.setPadding(new Insets(28));
        content.getChildren().add(sectionTitle("Classifica finale"));
        scores = new VBox(8);
        content.getChildren().add(scores);
        content.getChildren().add(disconnectButton());
        refresh();
        return content;
    }

    public void refresh() {
        if (scores == null) {
            return;
        }
        scores.getChildren().clear();
        model.getFinalScores().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> scores.getChildren().add(new Label(entry.getKey() + " - " + entry.getValue() + " PP")));
    }

    private Button disconnectButton() {
        Button quit = new Button("Esci");
        quit.getStyleClass().add("secondary-button");
        GuiIcons.decorateButton(quit, MaterialDesignL.LOGOUT);
        quit.setOnAction(event -> {
            controller.disconnect();
            Platform.exit();
        });
        return quit;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }
}
