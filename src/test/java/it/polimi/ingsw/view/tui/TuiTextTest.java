package it.polimi.ingsw.view.tui;

import org.junit.jupiter.api.Test;

import static it.polimi.ingsw.view.tui.TuiStyle.BOLD;
import static it.polimi.ingsw.view.tui.TuiStyle.CYN;
import static it.polimi.ingsw.view.tui.TuiStyle.RST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure text/layout helpers in TuiText.
 *
 * These guard the byte-for-byte rendering contract of the TUI: any
 * regression in width measurement, padding or centering would silently shift
 * borders by one or more cells.
 */
class TuiTextTest {

    @Test
    void stripAnsiRemovesEscapeSequencesAndKeepsContent() {
        assertEquals("hello", TuiText.stripAnsi(CYN + BOLD + "hello" + RST));
        assertEquals("", TuiText.stripAnsi(RST));
        assertEquals("plain", TuiText.stripAnsi("plain"));
    }

    @Test
    void visIgnoresAnsiButCountsRawChars() {
        // vis is the fast approximation: it strips ANSI then returns length.
        assertEquals(5, TuiText.vis(CYN + "hello" + RST));
        assertEquals(0, TuiText.vis(null));
        assertEquals(0, TuiText.vis(""));
    }

    @Test
    void visualWidthCountsAsciiAsOneCell() {
        assertEquals(5, TuiText.visualWidth("hello"));
        assertEquals(5, TuiText.visualWidth(BOLD + "hello" + RST));
    }

    @Test
    void visualWidthCountsWideEmojiAsTwoCells() {
        // 🍖 (U+1F356) sits in the wide-emoji range.
        assertEquals(2, TuiText.visualWidth("🍖"));
        // "5🍖" = digit (1) + wide emoji (2) = 3 cells.
        assertEquals(3, TuiText.visualWidth("5🍖"));
    }

    @Test
    void visualWidthHandlesKeycapEmojiAsTwoCells() {
        // "1️⃣" is digit '1' + U+FE0F + U+20E3 (keycap combining mark).
        // The whole sequence renders as a single 2-cell glyph.
        assertEquals(2, TuiText.visualWidth("1️⃣"));
        assertEquals(2, TuiText.visualWidth("9️⃣"));
    }

    @Test
    void isWideEmojiCoversGameIcons() {
        assertTrue(TuiText.isWideEmoji(0x1F356));  // 🍖
        assertTrue(TuiText.isWideEmoji(0x1F31F));  // 🌟
        assertTrue(TuiText.isWideEmoji(0x1F3F9));  // 🏹
        assertFalse(TuiText.isWideEmoji('A'));
        assertFalse(TuiText.isWideEmoji('1'));
    }

    @Test
    void isKeycapEmojiRequiresDigitAndU20E3() {
        assertTrue(TuiText.isKeycapEmoji("1️⃣", 0));
        assertTrue(TuiText.isKeycapEmoji("1⃣", 0));         // FE0F is optional
        assertFalse(TuiText.isKeycapEmoji("A️⃣", 0));  // letter, not digit
        assertFalse(TuiText.isKeycapEmoji("1", 0));              // no enclosing mark
    }

    @Test
    void padRightFillsWithSpacesUpToTarget() {
        String s = TuiText.padRight("hi", 5);
        assertEquals("hi   ", s);
        assertEquals(5, s.length());
    }

    @Test
    void padRightIgnoresAnsiWhenMeasuringWidth() {
        String input = CYN + "hi" + RST;
        String s = TuiText.padRight(input, 5);
        // Three trailing spaces because "hi" is 2 visible cells.
        assertTrue(s.endsWith("   "));
        // Original ANSI must be preserved.
        assertTrue(s.contains(CYN));
        assertTrue(s.contains(RST));
    }

    @Test
    void padRightDoesNotShrinkLongerStrings() {
        assertEquals("hello", TuiText.padRight("hello", 3));
    }

    @Test
    void padVisualUsesVisualWidthForEmojiAlignment() {
        // visual width of "🍖" is 2; padding to 5 must add 3 spaces, not 4.
        String s = TuiText.padVisual("🍖", 5);
        assertEquals("🍖   ", s);
    }

    @Test
    void ellipsizeKeepsShortText() {
        assertEquals("hi", TuiText.ellipsize("hi", 10));
    }

    @Test
    void ellipsizeTruncatesAndAppendsDot() {
        assertEquals("hell.", TuiText.ellipsize("hello world", 5));
    }

    @Test
    void ellipsizeWithVeryNarrowWidthReturnsSingleDot() {
        assertEquals(".", TuiText.ellipsize("anything", 1));
    }

    @Test
    void ellipsizeNullReturnsEmpty() {
        assertEquals("", TuiText.ellipsize(null, 10));
    }

    @Test
    void centerStrPadsSymmetricallyForEvenSlack() {
        assertEquals("  hi  ", TuiText.centerStr("hi", 6));
    }

    @Test
    void centerStrFavoursRightSpaceForOddSlack() {
        // (5 - 2) = 3 slack: left=1, right=2.
        assertEquals(" hi  ", TuiText.centerStr("hi", 5));
    }

    @Test
    void centerStrTruncatesOverlongText() {
        assertEquals("hell", TuiText.centerStr("hello", 4));
    }

    @Test
    void centerStrNullBecomesEmpty() {
        assertEquals("   ", TuiText.centerStr(null, 3));
    }

    @Test
    void centerPreservesAnsiAndMeasuresVisibleWidth() {
        String colored = CYN + "hi" + RST;
        String out = TuiText.center(colored, 6);
        assertTrue(out.startsWith("  "));
        assertTrue(out.endsWith("  "));
        assertTrue(out.contains(colored));
    }

    @Test
    void centerVisualHandlesEmojiCellCount() {
        // "🍖" = 2 visible cells, target 6 → pad 4 (2 left, 2 right).
        String out = TuiText.centerVisual("🍖", 6);
        assertEquals("  🍖  ", out);
    }

    @Test
    void centerVisualFallsBackForOverlongInput() {
        // When the visual width exceeds the target, the helper degrades to
        // centering the ANSI-stripped, truncated string rather than breaking
        // the surrounding border.
        String out = TuiText.centerVisual(CYN + "verylong" + RST, 4);
        assertFalse(out.contains(CYN), "ANSI must be stripped in the fallback path");
    }

    @Test
    void titledTopReturnsExpectedShape() {
        String out = TuiText.titledTop("X", 9, CYN);
        // " X " is 3 visible cells; 9 - 3 = 6 dashes split 3 left + 3 right,
        // wrapped by "•" sentinels.
        assertTrue(out.contains("•---"));
        assertTrue(out.contains(" X "));
        assertTrue(out.endsWith("•" + RST));
    }

    @Test
    void roundedBottomReturnsExpectedShape() {
        String out = TuiText.roundedBottom(4, CYN);
        assertEquals(CYN + "'----'" + RST, out);
    }

    @Test
    void makeCardTopBorderHasExpectedShape() {
        // Border is "+", dashes, " #1 ", dashes, "+", with the dash count on
        // each side chosen so the whole label fits within cardW.
        String out = TuiText.makeCardTopBorder("#1", 8, CYN);
        assertTrue(out.startsWith(CYN + "+"));
        assertTrue(out.endsWith("+" + RST));
        assertTrue(out.contains(" #1 "));
    }

    @Test
    void makeAsciiCardTopBorderHasExpectedShape() {
        String out = TuiText.makeAsciiCardTopBorder("42", 8, CYN);
        assertTrue(out.contains(" 42 "));
        assertTrue(out.startsWith(CYN + "+"));
        assertTrue(out.endsWith("+" + RST));
    }

    @Test
    void rankLabelProducesColoredOrdinals() {
        assertTrue(TuiText.rankLabel(1).contains("1."));
        assertTrue(TuiText.rankLabel(2).contains("2."));
        assertTrue(TuiText.rankLabel(3).contains("3."));
        assertTrue(TuiText.rankLabel(7).contains("7."));
    }

    @Test
    void formatDurationProducesMmSs() {
        assertEquals("00:00", TuiText.formatDuration(0L));
        assertEquals("00:01", TuiText.formatDuration(1L));        // 1 ms rounds up to 1 s
        assertEquals("00:30", TuiText.formatDuration(30_000L));
        assertEquals("01:00", TuiText.formatDuration(60_000L));
        assertEquals("02:05", TuiText.formatDuration(125_000L));
    }

    @Test
    void formatDurationClampsNegativeToZero() {
        assertEquals("00:00", TuiText.formatDuration(-1_000L));
    }

    @Test
    void asciiGlyphReturnsThreeLineGlyphs() {
        String[] g = TuiText.asciiGlyph('A');
        assertEquals(3, g.length);
        for (String line : g) {
            assertEquals(line.length(), g[0].length(),
                    "Every glyph row must share the same width");
        }
    }

    @Test
    void asciiWordLinesProducesThreeAlignedRows() {
        String[] lines = TuiText.asciiWordLines("MESOS", 200);
        assertEquals(3, lines.length);
        assertEquals(lines[0].length(), lines[1].length());
        assertEquals(lines[0].length(), lines[2].length());
    }

    @Test
    void asciiWordLinesNormalisesLowercaseAndStripsPunctuation() {
        // Internally upper-cases and keeps only [A-Z0-9 _-]; result must still
        // be three non-empty rows.
        String[] lines = TuiText.asciiWordLines("hi!", 200);
        assertEquals(3, lines.length);
        assertFalse(lines[0].isEmpty());
    }

    @Test
    void asciiWordLinesBlankInputBecomesQuestionMark() {
        String[] lines = TuiText.asciiWordLines("", 200);
        assertEquals(3, lines.length);
        // The "?" fallback glyph has width 3.
        assertEquals(3, lines[0].length());
    }
}
