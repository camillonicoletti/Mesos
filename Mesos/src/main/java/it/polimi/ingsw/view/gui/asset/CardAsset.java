package it.polimi.ingsw.view.gui.asset;

import java.util.Objects;
import java.util.Optional;

public final class CardAsset {
    private final int id;
    private final String kind;
    private final String type;
    private final String era;
    private final String frontPath;
    private final String backPath;
    private final boolean frontAvailable;
    private final boolean backAvailable;

    public CardAsset(int id,
                     String kind,
                     String type,
                     String era,
                     String frontPath,
                     String backPath,
                     boolean frontAvailable,
                     boolean backAvailable) {
        this.id = id;
        this.kind = Objects.requireNonNull(kind);
        this.type = type;
        this.era = era;
        this.frontPath = Objects.requireNonNull(frontPath);
        this.backPath = Objects.requireNonNull(backPath);
        this.frontAvailable = frontAvailable;
        this.backAvailable = backAvailable;
    }

    public int getId() {
        return id;
    }

    public String getKind() {
        return kind;
    }

    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }

    public Optional<String> getEra() {
        return Optional.ofNullable(era);
    }

    public String getFrontPath() {
        return frontPath;
    }

    public String getBackPath() {
        return backPath;
    }

    public boolean isFrontAvailable() {
        return frontAvailable;
    }

    public boolean isBackAvailable() {
        return backAvailable;
    }

    public boolean hasCompleteImages() {
        return frontAvailable && backAvailable;
    }
}
