package it.polimi.ingsw.view.gui.component;

import javafx.application.Platform;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Keeps the fixed-size board scaled to fit inside the available root area.
 * Installs listeners on the root layer and board bounds and recomputes a
 * uniform scale factor clamped to a sensible range.
 */
final class BoardScaler {
    private static final double MIN_SCALE = 0.72;
    private static final double MAX_SCALE = 1.06;
    private static final double HORIZONTAL_MARGIN = 22;
    private static final double VERTICAL_MARGIN = 16;

    private final StackPane rootLayer;
    private final Region board;
    private boolean installed;

    BoardScaler(StackPane rootLayer, Region board) {
        this.rootLayer = rootLayer;
        this.board = board;
    }

    void install() {
        if (installed) {
            return;
        }
        installed = true;
        rootLayer.widthProperty().addListener((observable, oldValue, newValue) -> update());
        rootLayer.heightProperty().addListener((observable, oldValue, newValue) -> update());
        board.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> update());
        Platform.runLater(this::update);
    }

    void update() {
        double boardWidth = board.getLayoutBounds().getWidth();
        double boardHeight = board.getLayoutBounds().getHeight();
        double availableWidth = rootLayer.getWidth() - HORIZONTAL_MARGIN;
        double availableHeight = rootLayer.getHeight() - VERTICAL_MARGIN;
        if (boardWidth <= 0 || boardHeight <= 0 || availableWidth <= 0 || availableHeight <= 0) {
            return;
        }

        double scale = Math.min(availableWidth / boardWidth, availableHeight / boardHeight);
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
        board.setScaleX(scale);
        board.setScaleY(scale);
    }
}
