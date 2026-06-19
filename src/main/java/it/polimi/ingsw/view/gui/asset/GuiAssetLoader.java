package it.polimi.ingsw.view.gui.asset;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GuiAssetLoader {
    public static final String ASSETS_DIR_PROPERTY = "mesos.assets.dir";
    public static final String ASSETS_DIR_ENV = "MESOS_ASSETS_DIR";

    private GuiAssetLoader() {
    }

    public static Optional<InputStream> open(String resourcePath) {
        Optional<Path> local = findLocal(resourcePath);
        if (local.isPresent()) {
            try {
                return Optional.of(Files.newInputStream(local.get()));
            } catch (IOException ignored) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(classLoader().getResourceAsStream(normalize(resourcePath)));
    }

    public static Optional<URL> url(String resourcePath) {
        Optional<Path> local = findLocal(resourcePath);
        if (local.isPresent()) {
            try {
                return Optional.of(local.get().toUri().toURL());
            } catch (IOException ignored) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(classLoader().getResource(normalize(resourcePath)));
    }

    public static boolean exists(String resourcePath) {
        return url(resourcePath).isPresent();
    }

    private static Optional<Path> findLocal(String resourcePath) {
        String normalized = normalize(resourcePath);
        String withoutAssetsPrefix = stripPrefix(normalized, "assets/");
        String withoutGuiPrefix = stripPrefix(normalized, "gui/");

        for (Path root : localRoots()) {
            for (String candidate : candidates(normalized, withoutAssetsPrefix, withoutGuiPrefix)) {
                Path path = root.resolve(candidate).normalize();
                if (Files.isRegularFile(path)) {
                    return Optional.of(path);
                }
            }
        }
        return Optional.empty();
    }

    private static List<String> candidates(String normalized, String withoutAssetsPrefix, String withoutGuiPrefix) {
        List<String> candidates = new ArrayList<>();
        candidates.add(normalized);
        if (!withoutAssetsPrefix.equals(normalized)) {
            candidates.add(withoutAssetsPrefix);
        }
        if (!withoutGuiPrefix.equals(normalized)) {
            candidates.add(withoutGuiPrefix);
        }
        return candidates;
    }

    private static List<Path> localRoots() {
        List<Path> roots = new ArrayList<>();
        addConfiguredRoot(roots, System.getProperty(ASSETS_DIR_PROPERTY));
        addConfiguredRoot(roots, System.getenv(ASSETS_DIR_ENV));

        roots.add(Path.of("assets"));
        roots.add(Path.of("src/main/resources/assets"));
        roots.add(Path.of("src/main/resources"));

        jarDirectory().ifPresent(jarDir -> {
            roots.add(jarDir.resolve("assets"));
            roots.add(jarDir.resolve("src/main/resources/assets"));
            roots.add(jarDir.resolve("src/main/resources"));
        });
        return roots;
    }

    private static void addConfiguredRoot(List<Path> roots, String configuredRoot) {
        if (configuredRoot != null && !configuredRoot.isBlank()) {
            roots.add(Path.of(configuredRoot));
        }
    }

    private static Optional<Path> jarDirectory() {
        try {
            CodeSource source = GuiAssetLoader.class.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) {
                return Optional.empty();
            }
            Path location = Path.of(source.getLocation().toURI());
            return Optional.of(Files.isRegularFile(location) ? location.getParent() : location);
        } catch (URISyntaxException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static String stripPrefix(String path, String prefix) {
        return path.startsWith(prefix) ? path.substring(prefix.length()) : path;
    }

    private static String normalize(String resourcePath) {
        String normalized = resourcePath == null ? "" : resourcePath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static ClassLoader classLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
