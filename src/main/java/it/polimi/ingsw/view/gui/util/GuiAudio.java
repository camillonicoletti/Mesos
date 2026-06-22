package it.polimi.ingsw.view.gui.util;

import it.polimi.ingsw.view.gui.asset.GuiAssetLoader;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;

// Handles all the game sound: the menu music and the click sound effects.
// It loads them when first needed and lets the player turn audio on and off.
public final class GuiAudio {
    private static final String MENU_MUSIC_PATH = "assets/audio/menu-music.mp3";
    private static final String ACTION_CLICK_PATH = "assets/audio/action-click.wav";
    private static final double MUSIC_VOLUME = 0.32;
    private static final double EFFECT_VOLUME = 0.72;

    private static MediaPlayer menuMusic;
    private static AudioClip actionClick;
    private static boolean menuMusicUnavailable;
    private static boolean actionClickUnavailable;
    private static boolean enabled = true;

    private GuiAudio() {
    }

    public static void playMenuMusic() {
        MediaPlayer player = menuMusic();
        if (player == null) {
            return;
        }
        player.setVolume(currentMusicVolume());
        if (player.getStatus() != MediaPlayer.Status.PLAYING) {
            player.play();
        }
    }

    public static void stopMenuMusic() {
        if (menuMusic != null) {
            menuMusic.stop();
        }
    }

    public static void playActionClick() {
        if (!enabled) {
            return;
        }
        AudioClip clip = actionClick();
        if (clip != null) {
            clip.play(EFFECT_VOLUME);
        }
    }

    public static void preloadActionClick() {
        actionClick();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean toggleEnabled() {
        setEnabled(!enabled);
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        GuiAudio.enabled = enabled;
        if (menuMusic != null) {
            menuMusic.setVolume(currentMusicVolume());
        }
    }

    public static void shutdown() {
        stopMenuMusic();
        if (menuMusic != null) {
            menuMusic.dispose();
            menuMusic = null;
        }
        actionClick = null;
    }

    private static MediaPlayer menuMusic() {
        if (menuMusic != null) {
            return menuMusic;
        }
        if (menuMusicUnavailable) {
            return null;
        }

        URL url = GuiAssetLoader.url(MENU_MUSIC_PATH).orElse(null);
        if (url == null) {
            menuMusicUnavailable = true;
            return null;
        }

        try {
            Media media = new Media(url.toExternalForm());
            media.setOnError(() -> {
                menuMusicUnavailable = true;
                disposeMenuMusic();
            });

            menuMusic = new MediaPlayer(media);
            menuMusic.setCycleCount(MediaPlayer.INDEFINITE);
            menuMusic.setVolume(currentMusicVolume());
            menuMusic.setOnError(() -> {
                menuMusicUnavailable = true;
                disposeMenuMusic();
            });
            return menuMusic;
        } catch (RuntimeException ex) {
            menuMusicUnavailable = true;
            disposeMenuMusic();
            return null;
        }
    }

    private static AudioClip actionClick() {
        if (actionClick != null) {
            return actionClick;
        }
        if (actionClickUnavailable) {
            return null;
        }

        URL url = GuiAssetLoader.url(ACTION_CLICK_PATH).orElse(null);
        if (url == null) {
            actionClickUnavailable = true;
            return null;
        }

        try {
            actionClick = new AudioClip(url.toExternalForm());
            return actionClick;
        } catch (RuntimeException ex) {
            actionClickUnavailable = true;
            actionClick = null;
            return null;
        }
    }

    private static void disposeMenuMusic() {
        if (menuMusic != null) {
            menuMusic.dispose();
            menuMusic = null;
        }
    }

    private static double currentMusicVolume() {
        return enabled ? MUSIC_VOLUME : 0.0;
    }
}
