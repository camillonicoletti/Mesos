package it.polimi.ingsw.view.tui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CardRenderer}, the pure card-drawing layer extracted from
 * {@link Renderer}. Focuses on:
 * <ul>
 *   <li>shape invariants ({@link TuiStyle#CARD_H} rows per card);</li>
 *   <li>each card kind picked via {@link CardCatalog} (Character / Building /
 *       Event variants + UNKNOWN fallback);</li>
 *   <li>pure helpers that previously lived as private methods on Renderer.</li>
 * </ul>
 * Card IDs hard-coded below come from the bundled JSON resources
 * (characters.json / buildings.json / events.json).
 */
class CardRendererTest {

    // ── shape: every variant returns exactly CARD_H lines ────────────────────

    @Test
    void drawCardCharacterReturnsCardHLines() {
        // Hunter id=1, foodIcon=true → mini-box variant.
        String[] out = CardRenderer.drawCard(1, false);
        assertEquals(TuiStyle.CARD_H, out.length);
        for (String line : out) assertNotNull(line);
    }

    @Test
    void drawCardBuildingReturnsCardHLines() {
        // Building id=97 (FullSetFood).
        String[] out = CardRenderer.drawCard(97, false);
        assertEquals(TuiStyle.CARD_H, out.length);
    }

    @Test
    void drawCardBuildingEffectFlagSwitchesToEffectLayout() {
        // Same id, but rendered with the effect-side layout when the flag is on.
        // The "EFFECT" label only appears in the effect variant.
        String[] standard = CardRenderer.drawCard(97, false);
        String[] effect   = CardRenderer.drawCard(97, true);
        assertEquals(TuiStyle.CARD_H, effect.length);
        assertTrue(joined(effect).contains("EFFECT"),
                "Effect layout must show the EFFECT header");
        assertFalse(joined(standard).contains("EFFECT"),
                "Standard building layout must not show the EFFECT header");
    }

    @Test
    void drawCardHuntEventHasHuntHeader() {
        // Event id=85 (Hunt, era I).
        String[] out = CardRenderer.drawCard(85, false);
        assertEquals(TuiStyle.CARD_H, out.length);
        assertTrue(joined(out).contains("HUNT"));
    }

    @Test
    void drawCardCavePaintingsHasOrPenaltyLine() {
        // Event id=88 (cavePaintings).
        String[] out = CardRenderer.drawCard(88, false);
        assertEquals(TuiStyle.CARD_H, out.length);
        // The "OR -<N>🌟" penalty line is unique to cavePaintings cards.
        assertTrue(joined(out).contains("OR -"));
    }

    @Test
    void drawCardSustenanceHasSustenanceHeader() {
        // Event id=86 (sustenance).
        String[] out = CardRenderer.drawCard(86, false);
        assertEquals(TuiStyle.CARD_H, out.length);
        assertTrue(joined(out).contains("SUSTENANCE"));
    }

    @Test
    void drawCardShamanicRitualHasShamanicHeader() {
        // Event id=87 (shamanicRitual).
        String[] out = CardRenderer.drawCard(87, false);
        assertEquals(TuiStyle.CARD_H, out.length);
        assertTrue(joined(out).contains("SHAMANIC"));
    }

    @Test
    void drawCardUnknownIdFallsBackToGenericLayoutWithCardHLines() {
        // No id in the JSON → CardCatalog returns Kind.UNKNOWN; CardRenderer
        // must still produce a CARD_H-row block (generic fallback branch).
        String[] out = CardRenderer.drawCard(999_999, false);
        assertEquals(TuiStyle.CARD_H, out.length);
        for (String line : out) assertNotNull(line);
    }

    // ── pure helpers ─────────────────────────────────────────────────────────

    @Test
    void characterNameKnownTypesMapToCanonicalLabels() {
        assertEquals("Hunter",   CardRenderer.characterName("Hunter"));
        assertEquals("Builder",  CardRenderer.characterName("Builder"));
        assertEquals("Gatherer", CardRenderer.characterName("Gatherer"));
        assertEquals("Artist",   CardRenderer.characterName("Artist"));
        assertEquals("Inventor", CardRenderer.characterName("Inventor"));
        assertEquals("Shaman",   CardRenderer.characterName("Shaman"));
    }

    @Test
    void characterNameUnknownTypeFallsBackToInput() {
        assertEquals("xyz", CardRenderer.characterName("xyz"));
    }

    @Test
    void parseBuilderPointsReadsLeadingPlusToken() {
        assertEquals(3, CardRenderer.parseBuilderPoints("+3 -1c"));
        assertEquals(0, CardRenderer.parseBuilderPoints("+0 -2c"));
    }

    @Test
    void parseBuilderPointsReturnsZeroOnBlankOrNull() {
        assertEquals(0, CardRenderer.parseBuilderPoints(""));
        assertEquals(0, CardRenderer.parseBuilderPoints(null));
        assertEquals(0, CardRenderer.parseBuilderPoints("   "));
    }

    @Test
    void parseBuilderDiscountReadsMinusCToken() {
        assertEquals(1, CardRenderer.parseBuilderDiscount("+3 -1c"));
        assertEquals(2, CardRenderer.parseBuilderDiscount("+0 -2c"));
    }

    @Test
    void parseBuilderDiscountReturnsZeroOnBlankOrUnrelated() {
        assertEquals(0, CardRenderer.parseBuilderDiscount(""));
        assertEquals(0, CardRenderer.parseBuilderDiscount(null));
        assertEquals(0, CardRenderer.parseBuilderDiscount("+3"));
    }

    @Test
    void inventorNumberStripsBrackets() {
        assertEquals("9", CardRenderer.inventorNumber("[9]"));
        assertEquals("1", CardRenderer.inventorNumber("[1]"));
    }

    @Test
    void inventorNumberReturnsEmptyOnBlank() {
        assertEquals("", CardRenderer.inventorNumber(null));
        assertEquals("", CardRenderer.inventorNumber(""));
    }

    @Test
    void buildingTargetNameUppercases() {
        assertEquals("GATHERER", CardRenderer.buildingTargetName("gatherer"));
        assertEquals("",         CardRenderer.buildingTargetName(""));
        assertEquals("",         CardRenderer.buildingTargetName(null));
    }

    @Test
    void isSingleCardEmojiAcceptsKnownGameGlyphs() {
        // Whitelist used by centerSingleEmoji. Any character glyph used on a
        // card must remain in this set, otherwise its layout breaks.
        assertTrue(CardRenderer.isSingleCardEmoji("🏹"));
        assertTrue(CardRenderer.isSingleCardEmoji("🔨"));
        assertTrue(CardRenderer.isSingleCardEmoji("🌾"));
        assertTrue(CardRenderer.isSingleCardEmoji("🎨"));
        assertTrue(CardRenderer.isSingleCardEmoji("🔮"));
        assertTrue(CardRenderer.isSingleCardEmoji("1️⃣"));
        assertTrue(CardRenderer.isSingleCardEmoji("9️⃣"));
    }

    @Test
    void isSingleCardEmojiRejectsArbitraryText() {
        assertFalse(CardRenderer.isSingleCardEmoji(""));
        assertFalse(CardRenderer.isSingleCardEmoji("hi"));
        assertFalse(CardRenderer.isSingleCardEmoji("🍖"));   // food, not on this list
        assertFalse(CardRenderer.isSingleCardEmoji("🏹 hi"));
    }

    @Test
    void centerSingleEmojiHasFixedSevenSevenPadding() {
        // Constant total width: 7 spaces + glyph + 7 spaces.
        assertEquals("       🏹       ", CardRenderer.centerSingleEmoji("🏹"));
    }

    // ── tribe helpers ────────────────────────────────────────────────────────

    @Test
    void tribeCardLabelOnBuildingIncludesCostAndPp() {
        // id=97: foodCost=4, pointsEndOfMatch=3.
        String label = CardRenderer.tribeCardLabel(CardCatalog.get(97));
        assertTrue(label.contains("4🍖"), label);
        assertTrue(label.contains("+3PP"), label);
    }

    @Test
    void tribeCardLabelOnHunterWithFoodIconShowsPlusFood() {
        // id=1: hunter, foodIcon=true → "+1🍖" suffix.
        String label = CardRenderer.tribeCardLabel(CardCatalog.get(1));
        assertTrue(label.contains("+1🍖"), label);
    }

    @Test
    void tribeCardLabelOnBuilderShowsDiscountAndPoints() {
        // id=6: builder pp=3 disc=1.
        String label = CardRenderer.tribeCardLabel(CardCatalog.get(6));
        assertTrue(label.contains("-1🍖"), label);
        assertTrue(label.contains("+3🌟"), label);
    }

    @Test
    void tribeCardLabelOnInventorShowsItsNumber() {
        // id=19: inventionIcon="9" → detail "[9]" → " 9".
        String label = CardRenderer.tribeCardLabel(CardCatalog.get(19));
        assertTrue(label.contains(" 9"), label);
    }

    // ── widths: building-effect cards keep CARD_H lines ─────────────────────

    @Test
    void drawBuildingEffectCardKeepsCardHLines() {
        // Pick a building with a rich text effect (id=98 SustenanceFoodDiscount).
        String[] out = CardRenderer.drawCard(98, true);
        assertEquals(TuiStyle.CARD_H, out.length);
        assertTrue(joined(out).contains("EFFECT"));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String joined(String[] lines) {
        return String.join("\n", lines);
    }
}
