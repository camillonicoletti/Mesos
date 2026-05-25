package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReconnectMessage;
import it.polimi.ingsw.view.gui.util.GuiIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;

import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class LoginView {
    private final ClientController controller;
    private final ClientModel model;
    private final String pendingNickname;
    private final Consumer<String> onNicknameSubmitted;
    private final Map<TotemColor, Button> totemButtons = new EnumMap<>(TotemColor.class);
    private TotemColor selectedTotem = TotemColor.RED;
    private StackPane panelShell;
    private VBox panel;
    private Label error;
    private VBox totemGroup;
    private Label fullLobbyMessage;
    private CheckBox reconnectCheckBox;
    private Button enterButton;

    public LoginView(ClientController controller, ClientModel model,
                     String pendingNickname, Consumer<String> onNicknameSubmitted) {
        this.controller = controller;
        this.model = model;
        this.pendingNickname = pendingNickname;
        this.onNicknameSubmitted = onNicknameSubmitted;
    }

    public Node render() {
        if (panelShell != null) {
            refresh();
            return panelShell;
        }

        panel = new VBox(12);
        panel.getStyleClass().add("login-panel");
        panel.setPrefWidth(350);
        panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        panel.setAlignment(Pos.CENTER);

        TextField nickname = new TextField(pendingNickname != null ? pendingNickname : "");
        nickname.setPromptText("Enter your totem name");
        nickname.setMaxWidth(Double.MAX_VALUE);

        HBox totemSelector = totemSelector();
        totemGroup = formGroup("CHOOSE YOUR TOTEM", totemSelector);

        reconnectCheckBox = new CheckBox("Reconnect");
        reconnectCheckBox.selectedProperty().addListener((obs, oldValue, selected) -> {
            totemSelector.setDisable(selected);
            refresh();
        });

        enterButton = primaryButton("JOIN");
        enterButton.setMaxWidth(Double.MAX_VALUE);
        enterButton.setOnAction(event -> submit(nickname, reconnectCheckBox));

        fullLobbyMessage = fullLobbyMessage();
        error = errorLabel();
        panel.getChildren().addAll(
                formGroup("TOTEM NAME", nickname),
                totemGroup,
                fullLobbyMessage,
                reconnectCheckBox, enterButton, error);
        refresh();

        panelShell = framedPanel(panel);
        return panelShell;
    }

    public void refresh() {
        boolean fullLobby = areAllTotemsTaken();
        boolean reconnecting = fullLobby || reconnectCheckBox != null && reconnectCheckBox.isSelected();

        if (totemGroup != null) {
            boolean showTotems = !fullLobby && !reconnecting;
            totemGroup.setVisible(showTotems);
            totemGroup.setManaged(showTotems);
        }
        if (fullLobbyMessage != null) {
            fullLobbyMessage.setVisible(fullLobby);
            fullLobbyMessage.setManaged(fullLobby);
        }
        if (reconnectCheckBox != null) {
            reconnectCheckBox.setVisible(!fullLobby);
            reconnectCheckBox.setManaged(!fullLobby);
        }
        if (enterButton != null) {
            enterButton.setText(reconnecting ? "RECONNECT" : "JOIN");
        }

        if (error != null) {
            error.setText(model.getLastError() == null ? "" : model.getLastError());
        }
        Set<TotemColor> taken = new HashSet<>(model.getLobbyPlayerColors().values());
        totemButtons.forEach((color, button) -> {
            boolean isTaken = taken.contains(color);
            button.setVisible(!isTaken);
            button.setManaged(!isTaken);
        });
        if (taken.contains(selectedTotem)) {
            TotemColor fallback = Arrays.stream(TotemColor.values())
                    .filter(c -> !taken.contains(c))
                    .findFirst()
                    .orElse(selectedTotem);
            selectedTotem = fallback;
            refreshTotemSelection();
        }
    }

    private void submit(TextField nickname, CheckBox reconnect) {
        String name = nickname.getText().trim();
        if (name.isBlank()) {
            return;
        }
        onNicknameSubmitted.accept(name);
        if (reconnect.isSelected() || areAllTotemsTaken()) {
            controller.send(new ReconnectMessage(name));
        } else {
            controller.send(new LoginMessage(name, selectedTotem));
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

    private Label fullLobbyMessage() {
        Label label = new Label("Numero massimo di giocatori raggiunto. Puoi rientrare solo con Reconnect.");
        label.getStyleClass().add("login-full-message");
        label.setWrapText(true);
        return label;
    }

    private boolean areAllTotemsTaken() {
        Set<TotemColor> taken = new HashSet<>(model.getLobbyPlayerColors().values());
        return taken.size() >= TotemColor.values().length;
    }

    private Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("login-field-label");
        return label;
    }

    private VBox formGroup(String label, Node control) {
        VBox group = new VBox(7, fieldLabel(label), control);
        group.getStyleClass().add("login-form-group");
        return group;
    }

    private StackPane framedPanel(Node content) {
        StackPane shell = new StackPane(content);
        shell.getStyleClass().add("login-panel-shell");
        shell.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return shell;
    }

    private HBox totemSelector() {
        HBox selector = new HBox(5);
        selector.getStyleClass().add("login-totem-selector");
        selector.setAlignment(Pos.CENTER_LEFT);

        for (TotemColor color : TotemColor.values()) {
            Button button = totemButton(color);
            totemButtons.put(color, button);
            selector.getChildren().add(button);
        }
        refreshTotemSelection();
        return selector;
    }

    private Button totemButton(TotemColor color) {
        Button button = new Button();
        button.getStyleClass().add("login-totem-choice");
        button.setGraphic(totemGraphic(color));
        button.setOnAction(event -> {
            selectedTotem = color;
            refreshTotemSelection();
        });
        return button;
    }

    private Node totemGraphic(TotemColor color) {
        try (InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(totemAssetPath(color))) {
            if (stream != null) {
                ImageView image = new ImageView(new Image(stream));
                image.setPreserveRatio(true);
                image.setFitHeight(52);
                image.setSmooth(true);
                return image;
            }
        } catch (Exception ignored) {
            // Fallback colored marker below.
        }

        Region fallback = new Region();
        fallback.getStyleClass().add("login-totem-fallback");
        fallback.setStyle("-fx-background-color: " + totemColorHex(color) + ";");
        return fallback;
    }


    private void refreshTotemSelection() {
        totemButtons.forEach((color, button) -> {
            button.getStyleClass().remove("login-totem-choice-selected");
            if (color == selectedTotem) {
                button.getStyleClass().add("login-totem-choice-selected");
            }
        });
    }

    private String totemAssetPath(TotemColor color) {
        return switch (color == null ? TotemColor.WHITE : color) {
            case RED -> "assets/tokens/totem_red.png";
            case YELLOW -> "assets/tokens/totem_yellow.png";
            case BLUE -> "assets/tokens/totem_blue.png";
            case PURPLE -> "assets/tokens/totem_purple.png";
            case WHITE -> "assets/tokens/totem_white.png";
        };
    }

    private String totemColorHex(TotemColor color) {
        return switch (color == null ? TotemColor.WHITE : color) {
            case RED -> "#b94a3a";
            case BLUE -> "#3c6f9e";
            case PURPLE -> "#77529a";
            case YELLOW -> "#d7a93d";
            case WHITE -> "#f3ebdc";
        };
    }
}
