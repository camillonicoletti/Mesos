package it.polimi.ingsw.view.tui;

import java.util.List;

import static it.polimi.ingsw.view.tui.TuiStyle.BOLD;
import static it.polimi.ingsw.view.tui.TuiStyle.CARD_H;
import static it.polimi.ingsw.view.tui.TuiStyle.CARD_W;
import static it.polimi.ingsw.view.tui.TuiStyle.DIM;
import static it.polimi.ingsw.view.tui.TuiStyle.GRY;
import static it.polimi.ingsw.view.tui.TuiStyle.MGN;
import static it.polimi.ingsw.view.tui.TuiStyle.RED;
import static it.polimi.ingsw.view.tui.TuiStyle.RST;
import static it.polimi.ingsw.view.tui.TuiStyle.WHT;
import static it.polimi.ingsw.view.tui.TuiStyle.YLW;
import static it.polimi.ingsw.view.tui.TuiText.centerStr;
import static it.polimi.ingsw.view.tui.TuiText.centerVisual;
import static it.polimi.ingsw.view.tui.TuiText.makeAsciiCardTopBorder;
import static it.polimi.ingsw.view.tui.TuiText.makeCardTopBorder;
import static it.polimi.ingsw.view.tui.TuiText.stripAnsi;
import static it.polimi.ingsw.view.tui.TuiText.visualWidth;

/**
 * Renders single cards as fixed-size {@code String[]} blocks (one element per
 * row, exactly {@link TuiStyle#CARD_H} rows, {@link TuiStyle#CARD_W} columns
 * wide). The class is intentionally pure: it never prints and never reads
 * {@link Renderer} state. Every rendering decision must be passed in via
 * parameters so the output is reproducible and testable.
 */
final class CardRenderer {

    private CardRenderer() { }

    /**
     * Draws the card identified by {@code id}.
     *
     * <p>{@code showBuildingEffect} is decided by {@link Renderer} (which owns
     * the user-selected "see effect &lt;id&gt;" state) and passed in as a
     * resolved flag, so this class stays stateless.
     */
    static String[] drawCard(int id, boolean showBuildingEffect) {
        return CardCatalog.get(id).accept(new CardCatalog.CardVisitor<String[]>() {
            @Override
            public String[] visitCharacter(CardCatalog.CardInfo c) {
                return drawCharacterCard(c);
            }

            @Override
            public String[] visitBuilding(CardCatalog.CardInfo c) {
                return showBuildingEffect ? drawBuildingEffectCard(c) : drawBuildingCard(c);
            }

            @Override
            public String[] visitEvent(CardCatalog.CardInfo c) {
                return drawEventCard(c);
            }

            @Override
            public String[] visitCard(CardCatalog.CardInfo c) {
                return drawGenericCard(c, GRY);
            }
        });
    }

    static String[] drawEventCard(CardCatalog.CardInfo c) {
        return switch (c.eventTag) {
            case "Hunt" -> drawHuntEventCard(c);
            case "Cave" -> drawCavePaintEventCard(c);
            case "Sust" -> drawSustenanceEventCard(c);
            case "Sham" -> drawShamanicRitualEventCard(c);
            default -> drawGenericCard(c, MGN);
        };
    }

    static String[] drawGenericCard(CardCatalog.CardInfo c, String bc) {
        String[] out = new String[CARD_H];
        out[0] = makeCardTopBorder("#" + c.id, CARD_W, bc);
        out[1] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[2] = bc + "|" + RST + BOLD + bc + centerStr(c.typeName, CARD_W) + RST + bc + "|" + RST;
        out[3] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[4] = bc + "|" + RST + GRY + centerStr(c.detail, CARD_W) + RST + bc + "|" + RST;
        out[5] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[6] = bc + "|" + RST + DIM + centerStr("Era " + c.era, CARD_W) + RST + bc + "|" + RST;
        out[7] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[8] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[9] = bc + "+" + "-".repeat(CARD_W) + "+" + RST;
        return out;
    }

    static String[] drawBuildingCard(CardCatalog.CardInfo c) {
        String[] out = new String[CARD_H];
        String bc = YLW;

        out[0] = makeCardTopBorder("#" + c.id, CARD_W, bc);
        out[1] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[2] = bc + "|" + RST + BOLD + bc + centerVisual("BUILDING", CARD_W) + RST + bc + "|" + RST;
        out[3] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[4] = bc + "|" + RST + GRY + BOLD
                + centerVisual(c.foodCost + "🍖  +" + c.pointsEnd + "🌟", CARD_W)
                + RST + bc + "|" + RST;
        out[5] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[6] = bc + "|" + RST + DIM + centerVisual("type see effect", CARD_W) + RST + bc + "|" + RST;
        out[7] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[8] = bc + "|" + RST + DIM + centerVisual("Era " + c.era, CARD_W) + RST + bc + "|" + RST;
        out[9] = bc + "+" + "-".repeat(CARD_W) + "+" + RST;

        return out;
    }

    static String[] drawBuildingEffectCard(CardCatalog.CardInfo c) {
        String[] out = new String[CARD_H];
        String bc = YLW;
        List<String> lines = buildingEffectLines(c);

        out[0] = makeCardTopBorder("#" + c.id, CARD_W, bc);
        out[1] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[2] = bc + "|" + RST + BOLD + bc + centerVisual("EFFECT", CARD_W) + RST + bc + "|" + RST;
        out[3] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[4] = buildingEffectLine(lines, 0, WHT + BOLD);
        out[5] = buildingEffectLine(lines, 1, WHT + BOLD);
        out[6] = buildingEffectLine(lines, 2, YLW + BOLD);
        out[7] = buildingEffectLine(lines, 3, YLW + BOLD);
        out[8] = bc + "|" + RST + DIM + centerVisual("Era " + c.era, CARD_W) + RST + bc + "|" + RST;
        out[9] = bc + "+" + "-".repeat(CARD_W) + "+" + RST;

        return out;
    }

    static String buildingEffectLine(List<String> lines, int index, String color) {
        String text = index < lines.size() ? lines.get(index) : "";
        return YLW + "|" + RST + color + centerVisual(text, CARD_W) + RST + YLW + "|" + RST;
    }

    static List<String> buildingEffectLines(CardCatalog.CardInfo c) {
        String target = buildingTargetName(c.buildingEffectTarget);
        return switch (c.buildingEffectName) {
            case "FullSetFood" -> List.of("FULL SET", "+5🍖");
            case "SustenanceFoodDiscount" -> List.of("SUSTENANCE", target, "-1🍖 EACH");
            case "ShamanicRitualImmunity" -> List.of("SHAMANIC", "NO LOSS");
            case "TotemSlotFoodBonus" -> List.of("TOTEM SLOT", "+1🍖");
            case "InventorPairFood" -> List.of("INVENTOR PAIR", "+3🍖");
            case "ShamanicRitualDoublePP" -> List.of("SHAMANIC", "DOUBLE 🌟");
            case "Shaman3ExtraStars" -> List.of("SHAMAN", "+3🌟");
            case "Hunt1Food1PP" -> List.of("HUNT", "EACH HUNTER", "+1🍖 +1🌟");
            case "EndBuilderPP" -> List.of("END GAME", "BUILDER 🌟");
            case "CavePaintingsFood" -> List.of("CAVE PAINT", "EACH ARTIST", "+1🍖");
            case "EndFullSetPP" -> List.of("END GAME", "FULL SET", "+6🌟 EACH");
            case "EndCharacterCountPP" -> List.of("END GAME", target, "+" + c.buildingEffectPointsPerCard + "🌟 EACH");
            case "ExtraCardUpperRow" -> List.of("UPPER ROW", "+1 CARD");
            case "EndPP" -> List.of("END GAME", "+25🌟");
            default -> List.of("SPECIAL", "EFFECT");
        };
    }

    static String buildingTargetName(String target) {
        if (target == null || target.isBlank()) {
            return "";
        }
        return target.toUpperCase();
    }

    static String[] drawHuntEventCard(CardCatalog.CardInfo c) {
        String[] out = new String[CARD_H];
        String bc = MGN;

        out[0] = makeCardTopBorder("#" + c.id, CARD_W, bc);
        out[1] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[2] = bc + "|" + RST + BOLD + bc + centerVisual("EVENT", CARD_W) + RST + bc + "|" + RST;
        out[3] = bc + "|" + RST + BOLD + bc + centerVisual("HUNT", CARD_W) + RST + bc + "|" + RST;
        out[4] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[5] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[6] = bc + "|" + RST + YLW + BOLD
                + centerVisual("+" + c.eventFood + "🍖 +" + c.eventPoints + "🌟", CARD_W)
                + RST + bc + "|" + RST;
        out[7] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[8] = bc + "|" + RST + DIM + centerVisual("Era " + c.era, CARD_W) + RST + bc + "|" + RST;
        out[9] = bc + "+" + "-".repeat(CARD_W) + "+" + RST;

        return out;
    }

    static String[] drawCavePaintEventCard(CardCatalog.CardInfo c) {
        String[] out = new String[CARD_H];
        String bc = MGN;

        out[0] = makeCardTopBorder("#" + c.id, CARD_W, bc);
        out[1] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[2] = bc + "|" + RST + BOLD + bc + centerVisual("EVENT", CARD_W) + RST + bc + "|" + RST;
        out[3] = bc + "|" + RST + BOLD + bc + centerVisual("CAVE PAINT", CARD_W) + RST + bc + "|" + RST;
        out[4] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[5] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[6] = bc + "|" + RST + YLW + BOLD
                + centerVisual(c.eventRequired + "🎨  +" + c.eventPoints + "🌟", CARD_W)
                + RST + bc + "|" + RST;
        out[7] = bc + "|" + RST + RED + BOLD
                + centerVisual("OR -" + c.eventPenalty + "🌟", CARD_W)
                + RST + bc + "|" + RST;
        out[8] = bc + "|" + RST + DIM + centerVisual("Era " + c.era, CARD_W) + RST + bc + "|" + RST;
        out[9] = bc + "+" + "-".repeat(CARD_W) + "+" + RST;

        return out;
    }

    static String[] drawSustenanceEventCard(CardCatalog.CardInfo c) {
        String[] out = new String[CARD_H];
        String bc = MGN;

        out[0] = makeCardTopBorder("#" + c.id, CARD_W, bc);
        out[1] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[2] = bc + "|" + RST + BOLD + bc + centerVisual("EVENT", CARD_W) + RST + bc + "|" + RST;
        out[3] = bc + "|" + RST + BOLD + bc + centerVisual("SUSTENANCE", CARD_W) + RST + bc + "|" + RST;
        out[4] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[5] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[6] = bc + "|" + RST + RED + BOLD
                + centerVisual("-" + c.eventPenalty + "🌟", CARD_W)
                + RST + bc + "|" + RST;
        out[7] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[8] = bc + "|" + RST + DIM + centerVisual("Era " + c.era, CARD_W) + RST + bc + "|" + RST;
        out[9] = bc + "+" + "-".repeat(CARD_W) + "+" + RST;

        return out;
    }

    static String[] drawShamanicRitualEventCard(CardCatalog.CardInfo c) {
        String[] out = new String[CARD_H];
        String bc = MGN;

        out[0] = makeCardTopBorder("#" + c.id, CARD_W, bc);
        out[1] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[2] = bc + "|" + RST + BOLD + bc + centerVisual("EVENT", CARD_W) + RST + bc + "|" + RST;
        out[3] = bc + "|" + RST + BOLD + bc + centerVisual("SHAMANIC", CARD_W) + RST + bc + "|" + RST;
        out[4] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[5] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        String gain = "+" + c.eventPoints + "🌟";
        String loss = "-" + c.eventPenalty + "🌟";
        // The "gain / loss" pair is centered manually: gap accounts for the
        // " / " separator (3 visible cells); halves may differ by one to keep
        // total width == CARD_W when gap is odd.
        int gap = Math.max(1, CARD_W - visualWidth(gain) - visualWidth(loss) - 3);
        out[6] = bc + "|" + RST
                + " ".repeat(gap / 2)
                + YLW + BOLD + gain + RST
                + WHT + BOLD + " / " + RST
                + RED + BOLD + loss + RST
                + " ".repeat(gap - gap / 2)
                + bc + "|" + RST;
        out[7] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[8] = bc + "|" + RST + DIM + centerVisual("Era " + c.era, CARD_W) + RST + bc + "|" + RST;
        out[9] = bc + "+" + "-".repeat(CARD_W) + "+" + RST;

        return out;
    }

    static String[] drawCharacterCard(CardCatalog.CardInfo c) {
        String[] out = new String[CARD_H];

        out[0] = makeAsciiCardTopBorder(String.valueOf(c.id), CARD_W, WHT);
        out[1] = characterLine("");
        out[2] = characterLine(BOLD + characterName(c.typeName) + RST);
        out[3] = characterLine(characterIcon(c));

        switch (c.typeName) {
            case "Hunter" -> {
                out[4] = characterLine("");

                if ("+".equals(c.detail)) {
                    out[5] = characterMiniBoxTop();
                    out[6] = characterMiniBoxText("+1🍖");
                    out[7] = characterMiniBoxBottom();
                } else {
                    out[5] = characterLine("");
                    out[6] = characterLine("");
                    out[7] = characterLine("");
                }

                out[8] = characterLine("");
            }
            case "Builder" -> {
                int pp = parseBuilderPoints(c.detail);
                int discount = parseBuilderDiscount(c.detail);
                out[4] = builderMiniBoxTop();
                out[5] = builderMiniBoxText("-" + discount + "🍖");
                out[6] = builderMiniBoxBottom();
                out[7] = builderMiniBoxText("+" + pp + "🌟");
                out[8] = builderMiniBoxBottom();
            }

            case "Shaman" -> {
                out[4] = characterLine("");
                out[5] = characterMiniBoxTop();
                out[6] = characterMiniBoxText(c.detail.replace("★", " 🔯"));
                out[7] = characterMiniBoxBottom();
                out[8] = characterLine("");
            }

            case "Inventor" -> {
                out[4] = characterLine("");
                out[5] = characterMiniBoxTop();
                out[6] = characterMiniBoxText("No. " + inventorNumber(c.detail));
                out[7] = characterMiniBoxBottom();
                out[8] = characterLine("");
            }

            default -> {
                out[4] = characterLine("");
                out[5] = characterLine("");
                out[6] = characterLine("");
                out[7] = characterLine("");
                out[8] = characterLine("");
            }
        }

        out[9] = WHT + "+" + "-".repeat(CARD_W) + "+" + RST;

        return out;
    }

    static String characterName(String typeName) {
        return switch (typeName) {
            case "Hunter"   -> "Hunter";
            case "Builder"  -> "Builder";
            case "Gatherer" -> "Gatherer";
            case "Artist"   -> "Artist";
            case "Inventor" -> "Inventor";
            case "Shaman"   -> "Shaman";
            default         -> typeName;
        };
    }

    static String characterIcon(CardCatalog.CardInfo c) {
        return switch (c.typeName) {
            case "Hunter"   -> "🏹";
            case "Builder"  -> "🔨";
            case "Gatherer" -> "🌾";
            case "Artist"   -> "🎨";
            case "Inventor" -> "🧠";
            case "Shaman"   -> "🔮";
            default         -> "";
        };
    }

    static int parseBuilderPoints(String detail) {
        if (detail == null || detail.isBlank()) return 0;

        String[] parts = detail.split(" ");
        for (String part : parts) {
            if (part.startsWith("+")) {
                return Integer.parseInt(part.substring(1));
            }
        }

        return 0;
    }

    static int parseBuilderDiscount(String detail) {
        if (detail == null || detail.isBlank()) return 0;

        String[] parts = detail.split(" ");
        for (String part : parts) {
            if (part.startsWith("-") && part.endsWith("c")) {
                return Integer.parseInt(part.substring(1, part.length() - 1));
            }
        }

        return 0;
    }

    static String characterLine(String text) {
        return WHT + "|" + RST + centerCardText(text) + WHT + "|" + RST;
    }

    // Inner box width 8 leaves a 1-cell margin on each side inside CARD_W (=16),
    // when wrapped by the surrounding "|" column delimiters.
    static String characterMiniBoxTop() {
        return characterLine("+" + "-".repeat(8) + "+");
    }

    static String characterMiniBoxBottom() {
        return characterLine("+" + "-".repeat(8) + "+");
    }

    static String characterMiniBoxText(String text) {
        return characterLine("|" + centerVisual(text, 8) + "|");
    }

    // Builder uses a wider 10-cell mini-box because it must fit two stacked
    // values ("-Nc" and "+NPP") instead of a single icon.
    static String builderMiniBoxTop() {
        return characterLine("+" + "-".repeat(10) + "+");
    }

    static String builderMiniBoxBottom() {
        return characterLine("+" + "-".repeat(10) + "+");
    }

    static String builderMiniBoxText(String text) {
        return characterLine("|" + centerVisual(text, 10) + "|");
    }

    static String inventorNumber(String detail) {
        if (detail == null || detail.isBlank()) return "";
        return detail.replace("[", "").replace("]", "");
    }

    static String centerCardText(String text) {
        if (text == null || text.isBlank()) {
            return " ".repeat(CARD_W);
        }

        String clean = stripAnsi(text);

        if (isSingleCardEmoji(clean)) {
            return centerSingleEmoji(text);
        }

        return centerVisual(text, CARD_W);
    }

    /**
     * Hardcoded whitelist of glyphs that {@link #centerSingleEmoji} can center
     * with a fixed 7+7 padding. Limited on purpose: extending it requires
     * checking that the emoji actually renders as 2 cells on the target
     * terminals, otherwise the card border breaks.
     */
    static boolean isSingleCardEmoji(String text) {
        return text.equals("🏹")
                || text.equals("🔨")
                || text.equals("🌾")
                || text.equals("🎨")
                || text.equals("🔮")
                || text.equals("1️⃣")
                || text.equals("2️⃣")
                || text.equals("3️⃣")
                || text.equals("4️⃣")
                || text.equals("5️⃣")
                || text.equals("6️⃣")
                || text.equals("7️⃣")
                || text.equals("8️⃣")
                || text.equals("9️⃣");
    }

    static String centerSingleEmoji(String emoji) {
        return " ".repeat(7) + emoji + " ".repeat(7);
    }

    static String cardIcon(CardCatalog.CardInfo card) {
        return card.accept(new CardCatalog.CardVisitor<String>() {
            @Override
            public String visitCharacter(CardCatalog.CardInfo c) {
                return switch (c.typeName) {
                    case "Hunter"   -> "🏹";
                    case "Builder"  -> "🔨";
                    case "Gatherer" -> "🌾";
                    case "Artist"   -> "🎨";
                    case "Inventor" -> "🧠";
                    case "Shaman"   -> "🔮";
                    default -> "?";
                };
            }

            @Override
            public String visitBuilding(CardCatalog.CardInfo c) {
                return "🏛";
            }

            @Override
            public String visitCard(CardCatalog.CardInfo c) {
                return "📋";
            }
        });
    }

    static String tribeCardLabel(CardCatalog.CardInfo ci) {
        return cardIcon(ci) + " " + ci.typeName + tribeCardExtra(ci);
    }

    static String tribeCardExtra(CardCatalog.CardInfo card) {
        return card.accept(new CardCatalog.CardVisitor<String>() {
            @Override
            public String visitBuilding(CardCatalog.CardInfo c) {
                return "  " + c.foodCost + "🍖 +" + c.pointsEnd + "PP";
            }

            @Override
            public String visitCharacter(CardCatalog.CardInfo c) {
                return characterCardExtra(c);
            }

            @Override
            public String visitCard(CardCatalog.CardInfo c) {
                return c.detail.isEmpty() ? "" : "  " + c.detail;
            }
        });
    }

    static String characterCardExtra(CardCatalog.CardInfo ci) {
        if ("Builder".equals(ci.typeName)) {
            int pp = parseBuilderPoints(ci.detail);
            int discount = parseBuilderDiscount(ci.detail);
            return "  -" + discount + "🍖 +" + pp + "🌟";
        }
        if ("Hunter".equals(ci.typeName) && "+".equals(ci.detail)) {
            return "  +1🍖";
        }
        if ("Inventor".equals(ci.typeName)) {
            return "  " + inventorNumber(ci.detail);
        }
        if ("Shaman".equals(ci.typeName)) {
            return "  " + ci.detail.replace("★", "🌟");
        }
        return ci.detail.isEmpty() ? "" : "  " + ci.detail;
    }
}
