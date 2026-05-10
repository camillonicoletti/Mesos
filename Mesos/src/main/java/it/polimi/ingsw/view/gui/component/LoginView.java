package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReconnectMessage;
import it.polimi.ingsw.view.gui.util.GuiIcons;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;

import java.util.function.Consumer;

public final class LoginView {
    private final ClientController controller;
    private final ClientModel model;
    private final String pendingNickname;
    private final Consumer<String> onNicknameSubmitted;

    public LoginView(ClientController controller, ClientModel model,
                     String pendingNickname, Consumer<String> onNicknameSubmitted) {
        this.controller = controller;
        this.model = model;
        this.pendingNickname = pendingNickname;
        this.onNicknameSubmitted = onNicknameSubmitted;
    }

    public Node render() {
        VBox panel = new VBox(16);
        panel.getStyleClass().add("login-panel");
        panel.setMaxWidth(420);
        panel.setAlignment(Pos.CENTER_LEFT);

        Label title = title("Mesos");
        Label subtitle = new Label("Entra nella partita o riconnettiti a una partita gia avviata.");
        subtitle.getStyleClass().add("muted");

        TextField nickname = new TextField(pendingNickname != null ? pendingNickname : "");
        nickname.setPromptText("Nickname");
        nickname.setMaxWidth(Double.MAX_VALUE);

        ComboBox<TotemColor> color = new ComboBox<>();
        color.getItems().addAll(TotemColor.values());
        color.setMaxWidth(Double.MAX_VALUE);
        color.setPromptText("Colore totem");

        CheckBox reconnect = new CheckBox("Riconnettiti");
        reconnect.selectedProperty().addListener((obs, oldValue, selected) -> color.setDisable(selected));

        Button enter = primaryButton("Entra");
        enter.setMaxWidth(Double.MAX_VALUE);
        enter.setOnAction(event -> submit(nickname, reconnect, color));

        Label error = errorLabel();
        panel.getChildren().addAll(title, subtitle, nickname, color, reconnect, enter, error);
        return panel;
    }

    private void submit(TextField nickname, CheckBox reconnect, ComboBox<TotemColor> color) {
        String name = nickname.getText().trim();
        if (name.isBlank()) {
            return;
        }
        onNicknameSubmitted.accept(name);
        if (reconnect.isSelected()) {
            controller.send(new ReconnectMessage(name));
        } else if (color.getValue() != null) {
            controller.send(new LoginMessage(name, color.getValue()));
        }
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        GuiIcons.decorateButton(button, MaterialDesignL.LOGIN);
        return button;
    }

    private Label errorLabel() {
        Label label = new Label(model.getLastError() == null ? "" : model.getLastError());
        label.getStyleClass().add("error");
        label.setWrapText(true);
        return label;
    }

    private Label title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("title");
        return label;
    }
}
