package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.view.gui.asset.GuiAssetLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;

final class MesosLogo {
    private static final String LOGO_PATH = "assets/ui/mesos_title.png";
    private static final Rectangle2D TITLE_VIEWPORT = new Rectangle2D(160, 230, 1220, 430);

    private MesosLogo() {
    }

    static Node create(double fitWidth, String styleClass) {
        try (InputStream stream = GuiAssetLoader.open(LOGO_PATH).orElse(null)) {
            if (stream != null) {
                ImageView logo = new ImageView(new Image(stream));
                logo.setViewport(TITLE_VIEWPORT);
                logo.setPreserveRatio(true);
                logo.setSmooth(true);
                logo.setFitWidth(fitWidth);
                logo.getStyleClass().add("mesos-title-image");
                logo.getStyleClass().add(styleClass);
                return logo;
            }
        } catch (Exception ignored) {
            // Fallback below keeps the GUI usable even without local assets.
        }

        Label fallback = new Label("MESOS");
        fallback.getStyleClass().add("mesos-title-fallback");
        fallback.getStyleClass().add(styleClass);
        return fallback;
    }
}
