package it.polimi.ingsw.view.tui;

import static it.polimi.ingsw.view.tui.TuiStyle.BOLD;
import static it.polimi.ingsw.view.tui.TuiStyle.GRY;
import static it.polimi.ingsw.view.tui.TuiStyle.RST;
import static it.polimi.ingsw.view.tui.TuiStyle.WHT;
import static it.polimi.ingsw.view.tui.TuiStyle.YLW;

/**
 * Pure text/layout helpers for terminal rendering.
 *
 * <p>These helpers must compute width and alignment against the <em>visible</em>
 * cell width on a terminal, which differs from {@link String#length()} for two
 * reasons:
 * <ul>
 *   <li>ANSI escape sequences (e.g. color/style codes) take zero visible cells
 *       and must be stripped before measuring.</li>
 *   <li>Emoji and keycap sequences typically occupy two cells, and some code
 *       points (variation selectors, combining marks) occupy zero.</li>
 * </ul>
 * All methods are pure and stateless.
 */
final class TuiText {

    private TuiText() { }

    /**
     * Fast visible-width approximation: strips ANSI escapes and returns the
     * remaining {@code String.length()}. Used where emoji width does not matter.
     */
    static int vis(String s) {
        if (s == null) return 0;
        return s.replaceAll("\033\\[[0-9;]*m", "").length();
    }

    /**
     * Accurate terminal visible width: strips ANSI, treats wide emoji and keycap
     * sequences as 2 cells, and ignores variation selectors / non-spacing marks.
     */
    static int visualWidth(String text) {
        if (text == null) return 0;

        String clean = stripAnsi(text);
        int width = 0;

        for (int i = 0; i < clean.length(); ) {
            int cp = clean.codePointAt(i);
            int charCount = Character.charCount(cp);

            if (isKeycapEmoji(clean, i)) {
                width += 2;
                i += charCount;

                if (i < clean.length() && clean.codePointAt(i) == 0xFE0F) {
                    i += Character.charCount(clean.codePointAt(i));
                }
                if (i < clean.length() && clean.codePointAt(i) == 0x20E3) {
                    i += Character.charCount(clean.codePointAt(i));
                }
                continue;
            }

            if (cp == 0xFE0F || cp == 0x20E3 || Character.getType(cp) == Character.NON_SPACING_MARK) {
                i += charCount;
                continue;
            }

            width += isWideEmoji(cp) ? 2 : 1;
            i += charCount;
        }

        return width;
    }

    /** Removes ANSI CSI escape sequences so they do not contribute to width. */
    static String stripAnsi(String text) {
        return text.replaceAll("\\u001B\\[[;\\d]*m", "");
    }

    /**
     * Detects a keycap emoji sequence starting at {@code index}: digit, optional
     * U+FE0F variation selector, then U+20E3 combining enclosing keycap.
     * Such sequences render as a single 2-cell glyph on most terminals.
     */
    static boolean isKeycapEmoji(String text, int index) {
        int cp = text.codePointAt(index);
        if (!Character.isDigit(cp)) return false;

        int next = index + Character.charCount(cp);
        if (next < text.length() && text.codePointAt(next) == 0xFE0F) {
            next += Character.charCount(text.codePointAt(next));
        }

        return next < text.length() && text.codePointAt(next) == 0x20E3;
    }

    /**
     * Heuristic for "wide" code points that occupy 2 terminal cells: the
     * Miscellaneous Symbols / Pictographs / Symbols & Pictographs Extended-A
     * ranges and the Symbols block used by the game's icons.
     */
    static boolean isWideEmoji(int cp) {
        return (cp >= 0x1F300 && cp <= 0x1FAFF)
                || (cp >= 0x2600 && cp <= 0x27BF);
    }

    /** Right-pads using {@link #visualWidth} so emoji-bearing strings align correctly. */
    static String padVisual(String s, int targetW) {
        int w   = visualWidth(stripAnsi(s));
        int pad = Math.max(0, targetW - w);
        return s + " ".repeat(pad);
    }

    static String padRight(String text, int targetVisW) {
        int pad = Math.max(0, targetVisW - vis(text));
        return text + " ".repeat(pad);
    }

    static String ellipsize(String text, int maxVisW) {
        if (text == null) return "";
        if (vis(text) <= maxVisW) return text;
        if (maxVisW <= 1) return ".";
        return text.substring(0, Math.min(text.length(), maxVisW - 1)) + ".";
    }

    static String center(String coloredText, int targetVisW) {
        int textVis = vis(coloredText);
        int pad = Math.max(0, targetVisW - textVis);
        return " ".repeat(pad / 2) + coloredText + " ".repeat(pad - pad / 2);
    }

    static String centerStr(String text, int targetW) {
        if (text == null) text = "";
        if (text.length() > targetW) text = text.substring(0, targetW);
        int pad = targetW - text.length();
        return " ".repeat(pad / 2) + text + " ".repeat(pad - pad / 2);
    }

    static String centerPlain(String text, int width) {
        int pad = Math.max(0, width - vis(text));
        return " ".repeat(pad / 2) + text;
    }

    /**
     * Centers a possibly emoji-bearing string inside {@code targetW} terminal
     * cells. If the visual width exceeds the target, falls back to centering
     * the ANSI-stripped string truncated to fit, to avoid breaking borders.
     */
    static String centerVisual(String text, int targetW) {
        if (text == null) text = "";

        int textW = visualWidth(text);
        if (textW > targetW) {
            return centerStr(stripAnsi(text), targetW);
        }

        int pad = targetW - textW;
        return " ".repeat(pad / 2) + text + " ".repeat(pad - pad / 2);
    }

    static String titledTop(String title, int width, String color) {
        String label = " " + title + " ";
        int dashTotal = Math.max(0, width - vis(label));
        int left = dashTotal / 2;
        int right = dashTotal - left;
        return color + "•" + "-".repeat(left) + label + "-".repeat(right) + "•" + RST;
    }

    static String roundedBottom(int width, String color) {
        return color + "'" + "-".repeat(width) + "'" + RST;
    }

    static String makeAsciiCardTopBorder(String idStr, int cardW, String color) {
        String label = " " + idStr + " ";
        int dashTotal = Math.max(0, cardW - visualWidth(label));
        int left = dashTotal / 2;
        int right = dashTotal - left;

        return color + "+" + "-".repeat(left) + label + "-".repeat(right) + "+" + RST;
    }

    static String makeCardTopBorder(String idStr, int cardW, String bc) {
        String label = " " + idStr + " ";
        int dashTotal = cardW - label.length();
        if (dashTotal < 0) {
            label = idStr;
            dashTotal = cardW - label.length();
        }
        int ld = dashTotal / 2;
        int rd = dashTotal - ld;
        return bc + "+" + "-".repeat(ld) + label + "-".repeat(rd) + "+" + RST;
    }

    static String rankLabel(int rank) {
        String label = rank + ".";
        return switch (rank) {
            case 1 -> YLW + BOLD + label + RST;
            case 2 -> WHT + BOLD + label + RST;
            case 3 -> GRY + BOLD + label + RST;
            default -> GRY + label + RST;
        };
    }

    static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, (millis + 999L) / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }

    static String[] asciiGlyph(char c) {
        return switch (c) {
            case 'A' -> new String[]{"█▀█", "█▀█", "▀ ▀"};
            case 'B' -> new String[]{"█▀▄", "█▀▄", "▀▀ "};
            case 'C' -> new String[]{"█▀▀", "█  ", "▀▀▀"};
            case 'D' -> new String[]{"█▀▄", "█ █", "▀▀ "};
            case 'E' -> new String[]{"█▀▀", "█▀ ", "▀▀▀"};
            case 'F' -> new String[]{"█▀▀", "█▀ ", "▀  "};
            case 'G' -> new String[]{"█▀▀", "█ ▄", "▀▀▀"};
            case 'H' -> new String[]{"█ █", "█▀█", "▀ ▀"};
            case 'I' -> new String[]{"█", "█", "▀"};
            case 'J' -> new String[]{"  █", "  █", "▀▀ "};
            case 'K' -> new String[]{"█ █", "██ ", "▀ ▀"};
            case 'L' -> new String[]{"█  ", "█  ", "▀▀▀"};
            case 'M' -> new String[]{"█▄ ▄█", "█ ▀ █", "▀   ▀"};
            case 'N' -> new String[]{"█▄  █", "█ ▀▄█", "▀   ▀"};
            case 'O' -> new String[]{"█▀█", "█ █", "▀▀▀"};
            case 'P' -> new String[]{"█▀▄", "█▀ ", "▀  "};
            case 'Q' -> new String[]{"█▀█", "█ █", "▀▀█"};
            case 'R' -> new String[]{"█▀▄", "█▀▄", "▀ ▀"};
            case 'S' -> new String[]{"█▀▀", "▀▀█", "▀▀▀"};
            case 'T' -> new String[]{"▀█▀", " █ ", " ▀ "};
            case 'U' -> new String[]{"█ █", "█ █", "▀▀▀"};
            case 'V' -> new String[]{"█ █", "█ █", " ▀ "};
            case 'W' -> new String[]{"█   █", "█ █ █", "▀▀ ▀▀"};
            case 'X' -> new String[]{"█ █", " █ ", "▀ ▀"};
            case 'Y' -> new String[]{"█ █", " ▀█", "  ▀"};
            case 'Z' -> new String[]{"▀▀█", " █ ", "▀▀▀"};
            case '0' -> new String[]{"█▀█", "█ █", "▀▀▀"};
            case '1' -> new String[]{" █ ", " █ ", " ▀ "};
            case '2' -> new String[]{"▀▀█", "█▀ ", "▀▀▀"};
            case '3' -> new String[]{"▀▀█", " ▀█", "▀▀▀"};
            case '4' -> new String[]{"█ █", "▀▀█", "  ▀"};
            case '5' -> new String[]{"█▀▀", "▀▀█", "▀▀▀"};
            case '6' -> new String[]{"█▀▀", "█▀█", "▀▀▀"};
            case '7' -> new String[]{"▀▀█", "  █", "  ▀"};
            case '8' -> new String[]{"█▀█", "█▀█", "▀▀▀"};
            case '9' -> new String[]{"█▀█", "▀▀█", "▀▀▀"};
            case '-' -> new String[]{"   ", "▀▀▀", "   "};
            case '_' -> new String[]{"   ", "   ", "▀▀▀"};
            case ' ' -> new String[]{"   ", "   ", "   "};
            default -> new String[]{"▀▀█", " ▄▀", " ▀ "};
        };
    }

    /**
     * Renders {@code text} as a three-line ASCII banner that fits within
     * {@code maxWidth} columns. Tries spacing 2 first, then 1, then truncates
     * the text if even spacing 1 overflows.
     */
    static String[] asciiWordLines(String text, int maxWidth) {
        String safe = text == null ? "" : text.toUpperCase().replaceAll("[^A-Z0-9 _-]", "");
        if (safe.isBlank()) {
            safe = "?";
        }
        for (int spacing = 2; spacing >= 1; spacing--) {
            String[] lines = buildAsciiWordLines(safe, spacing);
            if (lines[0].length() <= maxWidth) {
                return lines;
            }
        }
        while (safe.length() > 1 && buildAsciiWordLines(safe, 1)[0].length() > maxWidth) {
            safe = safe.substring(0, safe.length() - 1);
        }
        return buildAsciiWordLines(safe, 1);
    }

    /** Builds the three-line ASCII banner for {@code text} at a fixed spacing. */
    static String[] buildAsciiWordLines(String text, int spacing) {
        StringBuilder top = new StringBuilder();
        StringBuilder mid = new StringBuilder();
        StringBuilder bot = new StringBuilder();
        String gap = " ".repeat(spacing);
        for (int i = 0; i < text.length(); i++) {
            if (i > 0) {
                top.append(gap);
                mid.append(gap);
                bot.append(gap);
            }
            String[] glyph = asciiGlyph(text.charAt(i));
            top.append(glyph[0]);
            mid.append(glyph[1]);
            bot.append(glyph[2]);
        }
        return new String[]{top.toString(), mid.toString(), bot.toString()};
    }
}
