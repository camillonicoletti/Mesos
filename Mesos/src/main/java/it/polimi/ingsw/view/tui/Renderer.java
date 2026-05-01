package it.polimi.ingsw.view.tui;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.PlayerView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Renderer {

    private static final String RST  = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String DIM  = "\033[2m";
    private static final String CYN  = "\033[96m";
    private static final String YLW  = "\033[93m";
    private static final String GRN  = "\033[92m";
    private static final String RED  = "\033[91m";
    private static final String MGN  = "\033[95m";
    private static final String BLU  = "\033[94m";
    private static final String WHT  = "\033[97m";
    private static final String GRY  = "\033[90m";

    private static final int W        = 140;
    private static final int CARD_W   = 16;
    private static final int CARD_H   = 10;
    private static final int CARD_GAP = 1;
    private static final int OFFER_W  = 14;
    private static final int OFFER_H  = 10;
    private static final int ORDER_W  = 38;

    private static final List<Character> TILE_ORDER = List.of('A','B','C','D','E','F','G');

    private static final Map<Character, String[]> TILE_ARROWS = new LinkedHashMap<>() {{
        put('A', new String[]{"+3c", ""});
        put('B', new String[]{"down", ""});
        put('C', new String[]{"up", ""});
        put('D', new String[]{"down2", ""});
        put('E', new String[]{"up", "down"});
        put('F', new String[]{"up2", ""});
        put('G', new String[]{"up2", "down"});
    }};

    private static final Map<Character, Integer> TILE_MIN = Map.of(
            'A', 5, 'B', 2, 'C', 2, 'D', 3, 'E', 2, 'F', 2, 'G', 4
    );

    public Renderer(ClientModel model) { }

    public void render(ClientModel model) {
        clear();
        String state = model.getGameState();
        header(state, model);
        body(state, model);
        if (model.getLastConnectionEvent() != null) {
            blank();
            row("  " + CYN + "i  " + model.getLastConnectionEvent() + RST);
        }
        if (model.getLastError() != null) {
            blank();
            row("  " + RED + "!  " + model.getLastError() + RST);
        }
        blank();
        footer(model);
    }

    private void body(String state, ClientModel model) {
        if (state == null) {
            loginScreen();
        } else switch (state) {
            case "LobbyState"            -> lobbyScreen(model);
            case "ReadyCheckState"       -> renderReadyCheck(model);
            case "TotemPlacementState"   -> gameScreen(model, true);
            case "ActionResolutionState" -> gameScreen(model, false);
            case "EventResolutionState"  -> simpleScreen("Il server sta risolvendo gli eventi...", model);
            case "EndRoundState"         -> simpleScreen("Fine del round " + model.getRoundNumber() + ". Preparazione...", model);
            case "SuspendedState"        -> suspendedScreen();
            case "EndGameState"          -> endGameScreen(model);
            default                      -> gameScreen(model, false);
        }
    }

    private void clear() {
        System.out.print("\033[2J\033[H");
        System.out.flush();
    }

    private void header(String state, ClientModel model) {
        boxLine('╔', '═', '╗');
        row(center(WHT + BOLD + "M E S O S" + RST, W));
        if (model.getMyNickname() != null && !model.getMyNickname().isBlank()) {
            row(GRY + center("Giocatore: " + WHT + BOLD + model.getMyNickname() + RST + GRY, W) + RST);
        } else {
            blank();
        }
        if (state != null && model.getCurrentEra() != null) {
            String sub = "Era " + model.getCurrentEra().name()
                    + "  |  Round " + model.getRoundNumber()
                    + "  |  " + stateLabel(state);
            row(GRY + center(sub, W) + RST);
        } else {
            blank();
        }
        boxLine('╠', '═', '╣');
    }

    private void footer(ClientModel model) {
        boxLine('╚', '═', '╝');
        System.out.print(WHT + BOLD + footerPrompt(model) + RST);
    }

    private void boxLine(char l, char m, char r) {
        System.out.println(CYN + BOLD + l + String.valueOf(m).repeat(W) + r + RST);
    }

    private void row(String content) {
        row(content, CYN);
    }

    private void row(String content, String borderColor) {
        int pad = Math.max(0, W - vis(content));
        System.out.println(borderColor + "║" + RST + content + " ".repeat(pad) + borderColor + "║" + RST);
    }

    private void blank() {
        System.out.println(CYN + "║" + " ".repeat(W) + "║" + RST);
    }

    private void loginScreen() {
        blank();
        row(center(WHT + BOLD + "Benvenuto in MESOS!" + RST, W));
        blank();
        row("  " + GRY + "Inserisci nickname e colore totem per entrare nel gioco." + RST);
        row("  " + GRY + "Se eri gia in partita puoi scegliere la riconnessione." + RST);
        blank();
        row("  Colori disponibili:  "
                + RED + BOLD + "RED" + RST + "   "
                + BLU + BOLD + "BLUE" + RST + "   "
                + MGN + BOLD + "PURPLE" + RST + "   "
                + YLW + BOLD + "YELLOW" + RST + "   "
                + WHT + BOLD + "WHITE" + RST);
        blank();
    }

    private void lobbyScreen(ClientModel model) {
        int cur = model.getLobbyPlayers().size();
        int exp = model.getExpectedPlayers();
        boolean iAmFirst = model.getMyNickname() != null
                && !model.getLobbyPlayers().isEmpty()
                && model.getMyNickname().equals(model.getLobbyPlayers().get(0));

        blank();
        row("  " + WHT + BOLD + "LOBBY" + RST + "  " + GRY + cur + " / " + (exp > 0 ? exp : "?") + " giocatori" + RST);
        blank();
        for (String p : model.getLobbyPlayers()) {
            row("    " + GRN + "●" + RST + "  " + WHT + p + RST);
        }
        blank();

        if (exp == 0) {
            if (iAmFirst) {
                row("  " + YLW + BOLD + "Sei il primo giocatore!" + RST);
                row("  " + GRY + "Scegli il numero di partecipanti:  " + RST + YLW + "players <2-5>" + RST);
            } else {
                row("  " + GRY + "In attesa che il primo giocatore scelga il numero di partecipanti..." + RST);
            }
        } else if (cur < exp) {
            int miss = exp - cur;
            row("  " + GRY + "In attesa di " + miss + (miss == 1 ? " giocatore..." : " giocatori...") + RST);
        } else {
            row("  " + GRY + "Lobby completa — attendo il passaggio al ready check..." + RST);
        }
        blank();
    }

    private void renderReadyCheck(ClientModel model) {
        blank();
        row("  " + WHT + BOLD + "TUTTI CONNESSI — Conferma la tua presenza" + RST);
        blank();

        for (String p : model.getLobbyPlayers()) {
            boolean ready = model.getReadyPlayers().contains(p);
            boolean isMe  = p.equals(model.getMyNickname());
            String status = ready ? GRN + "v  Pronto          " : YLW + "?  In attesa...    ";
            String name   = isMe ? CYN + BOLD + p + "  (tu)" + RST : WHT + p + RST;
            row("    " + status + RST + name);
        }

        blank();
        boolean iAmReady = model.getReadyPlayers().contains(model.getMyNickname());
        if (iAmReady) {
            row("  " + GRN + BOLD + "v HAI CONFERMATO" + RST + GRY + "  —  in attesa degli altri giocatori..." + RST);
        } else {
            row("  " + YLW + BOLD + ">>>  TOCCA A TE!  <<<" + RST);
            row("  " + WHT + "Digita  " + RST + YLW + BOLD + "ready" + RST + WHT + "  per confermare la tua presenza." + RST);
        }
        blank();
    }

    private void suspendedScreen() {
        blank();
        row(center(RED + BOLD + "PARTITA IN PAUSA" + RST, W));
        blank();
        row(center(GRY + "In attesa che almeno un altro giocatore si riconnetta..." + RST, W));
        blank();
    }

    private void endGameScreen(ClientModel model) {
        blank();
        row(center(YLW + BOLD + "FINE PARTITA" + RST, W));
        blank();
        row("  Vincitore:  " + GRN + BOLD + model.getWinnerNickname() + RST);
        blank();

        int rank = 1;
        for (Map.Entry<String, Integer> e : model.getFinalScores().entrySet()
                .stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).toList()) {
            String medal = switch (rank) {
                case 1  -> YLW + "1. " + RST;
                case 2  -> GRY + "2. " + RST;
                default -> DIM + rank + ". " + RST;
            };
            row("  " + medal + WHT + String.format("%-16s", e.getKey()) + RST + "  " + YLW + e.getValue() + " PP" + RST);
            rank++;
        }
        blank();
    }

    private void simpleScreen(String msg, ClientModel model) {
        blank();
        row("  " + YLW + msg + RST);
        blank();
        for (PlayerView p : model.getPlayers().values()) {
            String col = totemColor(p.getTotemColor());
            row("  " + col + "●" + RST + " " + WHT + String.format("%-16s", p.getNickname()) + RST
                    + "  " + YLW + p.getFood() + "c" + RST
                    + "  " + foodBar(p.getFood(), 20)
                    + "  " + GRN + p.getPrestigePoints() + " PP" + RST);
        }
        blank();
    }

    private void gameScreen(ClientModel model, boolean totemPhase) {
        blank();

        List<String> order = model.getTurnOrder();
        if (!order.isEmpty()) {
            StringBuilder sb = new StringBuilder("  " + GRY + "Turno: " + RST);
            for (int i = 0; i < order.size(); i++) {
                if (i > 0) sb.append(GRY + "  ->  " + RST);
                PlayerView pv = model.getPlayers().get(order.get(i));
                String col = pv != null ? totemColor(pv.getTotemColor()) : WHT;
                sb.append(col).append(BOLD).append("● ").append(order.get(i)).append(RST);
            }
            row(sb.toString());
        }

        if (totemPhase) {
            String cur = model.getCurrentTotemPlacementPlayer();
            if (cur != null && cur.equals(model.getMyNickname())) {
                row(center(YLW + BOLD + ">>>  TOCCA A TE — PIAZZA IL TOTEM!  <<<" + RST, W));
            } else if (cur != null) {
                row("  " + GRY + "In attesa del piazzamento di " + YLW + cur + RST + GRY + "..." + RST);
            }
        } else if (model.isMyTurn()) {
            row(center(GRN + BOLD + ">>>  TOCCA A TE!  <<<" + RST, W));
        }

        blank();

        List<Integer> upperAll = new ArrayList<>(model.getUpperRowCharacterIds());
        upperAll.addAll(model.getUpperRowBuildingIds());
        renderCardRow(upperAll);

        blank();
        renderCenterZone(model);
        blank();

        List<Integer> lowerAll = new ArrayList<>(model.getLowerRowCharacterIds());
        lowerAll.addAll(model.getLowerRowBuildingIds());
        renderCardRow(lowerAll);

        blank();

        String currentActor = totemPhase
                ? model.getCurrentTotemPlacementPlayer()
                : (!order.isEmpty() ? order.get(0) : null);
        for (PlayerView p : model.getPlayers().values()) {
            renderPlayer(p, p.getNickname().equals(currentActor), p.getNickname().equals(model.getMyNickname()));
        }

        blank();
        if (totemPhase) {
            row("  " + GRY + "place <lettera>   es. place B" + RST);
        } else {
            row("  " + GRY + "pick <id> upper|lower   --   build <id> upper|lower   --   help" + RST);
        }
        blank();
    }

    private void renderCardRow(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            row("  " + GRY + "(nessuna carta)" + RST);
            return;
        }

        int slotW = CARD_W + 2 + CARD_GAP;
        int maxPer = Math.max(1, (W - 4) / slotW);

        for (int start = 0; start < ids.size(); start += maxPer) {
            List<Integer> slice = ids.subList(start, Math.min(start + maxPer, ids.size()));
            renderCardChunk(slice);
            if (start + maxPer < ids.size()) blank();
        }
    }

    private void renderCardChunk(List<Integer> ids) {
        String[][] cards = new String[ids.size()][];
        for (int i = 0; i < ids.size(); i++) {
            cards[i] = drawCard(ids.get(i));
        }

        for (int line = 0; line < CARD_H; line++) {
            StringBuilder sb = new StringBuilder("  ");
            for (int i = 0; i < cards.length; i++) {
                sb.append(cards[i][line]);
                if (i < cards.length - 1) sb.append(" ".repeat(CARD_GAP));
            }
            row(sb.toString());
        }
    }

    private String[] drawCard(int id) {
        CardCatalog.CardInfo c = CardCatalog.get(id);

        String bc = switch (c.kind) {
            case CHARACTER -> WHT;
            case BUILDING  -> YLW;
            case EVENT     -> MGN;
            default        -> GRY;
        };

        String idStr = "#" + c.id;
        String typeLine = c.typeName;
        String detLine = c.detail;
        String eraLine = "Era " + c.era;
        String extraLine = (c.kind == CardCatalog.Kind.BUILDING)
                ? c.foodCost + "c  +" + c.pointsEnd + "PP"
                : "";

        String[] out = new String[CARD_H];
        out[0] = makeCardTopBorder(idStr, CARD_W, bc);
        out[1] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        out[2] = bc + "|" + RST + BOLD + bc + centerStr(typeLine, CARD_W) + RST + bc + "|" + RST;
        out[3] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;

        if (!extraLine.isEmpty()) {
            out[4] = bc + "|" + RST + GRY + centerStr(extraLine, CARD_W) + RST + bc + "|" + RST;
            out[5] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
            out[6] = bc + "|" + RST + GRY + centerStr(detLine, CARD_W) + RST + bc + "|" + RST;
            out[7] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
            out[8] = bc + "|" + RST + DIM + centerStr(eraLine, CARD_W) + RST + bc + "|" + RST;
        } else {
            out[4] = bc + "|" + RST + GRY + centerStr(detLine, CARD_W) + RST + bc + "|" + RST;
            out[5] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
            out[6] = bc + "|" + RST + DIM + centerStr(eraLine, CARD_W) + RST + bc + "|" + RST;
            out[7] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
            out[8] = bc + "|" + " ".repeat(CARD_W) + "|" + RST;
        }

        out[9] = bc + "+" + "-".repeat(CARD_W) + "+" + RST;
        return out;
    }

    private String makeCardTopBorder(String idStr, int cardW, String bc) {
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

    private void renderCenterZone(ClientModel model) {
        String[] orderBox = drawOrderBox(model);

        int numPlayers = Math.max(2, model.getPlayers().size());
        List<Character> active = TILE_ORDER.stream()
                .filter(c -> TILE_MIN.get(c) <= numPlayers)
                .collect(Collectors.toList());

        Map<Character, String> occ = model.getTotemOnTiles();
        List<String[]> tiles = new ArrayList<>();

        for (char c : active) {
            String occupant = occ.get(c);
            String occCol = WHT;
            if (occupant != null) {
                PlayerView pv = model.getPlayers().get(occupant);
                if (pv != null) occCol = totemColor(pv.getTotemColor());
            }
            tiles.add(drawOfferTile(c, occupant, occCol));
        }

        int orderBoxW = ORDER_W + 2;
        int tileBoxW = OFFER_W + 2;
        int availableForTiles = W - 2 - orderBoxW - 3;
        int tilesPerChunk = Math.max(1, availableForTiles / (tileBoxW + 1));

        for (int start = 0; start < tiles.size(); start += tilesPerChunk) {
            List<String[]> chunk = tiles.subList(start, Math.min(start + tilesPerChunk, tiles.size()));
            boolean firstChunk = start == 0;
            int height = Math.max(firstChunk ? orderBox.length : 0, OFFER_H);

            for (int line = 0; line < height; line++) {
                StringBuilder sb = new StringBuilder("  ");

                if (firstChunk) {
                    sb.append(line < orderBox.length ? orderBox[line] : " ".repeat(orderBoxW));
                    sb.append("   ");
                } else {
                    sb.append(" ".repeat(orderBoxW + 3));
                }

                for (int i = 0; i < chunk.size(); i++) {
                    String[] tile = chunk.get(i);
                    sb.append(line < tile.length ? tile[line] : " ".repeat(tileBoxW));
                    if (i < chunk.size() - 1) sb.append(" ");
                }

                row(sb.toString());
            }

            if (start + tilesPerChunk < tiles.size()) blank();
        }
    }

    private String[] drawOrderBox(ClientModel model) {
        List<String> order = model.getTurnOrder();
        int numPlayers = Math.max(2, Math.max(model.getPlayers().size(), order.size()));
        int visibleSlots = Math.max(numPlayers, order.size());

        String[] box = new String[2 + visibleSlots * 3];
        int line = 0;

        box[line++] = titledTop("ORDINE TURNO", ORDER_W, CYN);

        for (int i = 0; i < visibleSlots; i++) {
            String name = i < order.size() ? order.get(i) : "";
            PlayerView pv = name.isBlank() ? null : model.getPlayers().get(name);
            String col = pv != null ? totemColor(pv.getTotemColor()) : GRY;
            boolean current = i == 0 && !name.isBlank();

            String marker = current ? GRN + BOLD + "▶" + RST : " ";
            String shownName = name.isBlank() ? "vuoto" : ellipsize(name, 12);
            String playerText = marker + " " + (i + 1) + "  " + col + "●" + RST + " "
                    + (current ? BOLD : "") + shownName + RST;

            String effect = turnEffectLabel(numPlayers, i + 1);
            String effectColor = turnEffectColor(effect);

            String playerTop = "•" + "-".repeat(22) + "•";
            String playerMid = "|" + padRight(playerText, 22) + "|";
            String playerBot = "'" + "-".repeat(22) + "'";

            String effectTop = "•" + "-".repeat(9) + "•";
            String effectMid = "|" + center(effectColor + effect + RST, 9) + "|";
            String effectBot = "'" + "-".repeat(9) + "'";

            String border = current ? GRN : CYN;
            box[line++] = border + "|" + RST + " " + playerTop + " " + effectTop + " " + border + "|" + RST;
            box[line++] = border + "|" + RST + " " + playerMid + " " + effectMid + " " + border + "|" + RST;
            box[line++] = border + "|" + RST + " " + playerBot + " " + effectBot + " " + border + "|" + RST;
        }

        box[line] = roundedBottom(ORDER_W, CYN);
        return box;
    }


    private String[] drawOfferTile(char letter, String occupant, String occCol) {
        String[] arrowKeys = TILE_ARROWS.getOrDefault(letter, new String[]{"", ""});
        String[] box = new String[OFFER_H];

        box[0] = titledTop("TILE " + letter, OFFER_W, CYN);

        String shownOccupant = occupant == null ? "vuota" : "● " + ellipsize(occupant, OFFER_W - 4);
        String occupantColor = occupant == null ? GRY : occCol + BOLD;

        box[1] = CYN + "|" + RST + " •" + "-".repeat(OFFER_W - 4) + "• " + CYN + "|" + RST;
        box[2] = CYN + "|" + RST + " |" + center(occupantColor + shownOccupant + RST, OFFER_W - 4) + "| " + CYN + "|" + RST;
        box[3] = CYN + "|" + RST + " '" + "-".repeat(OFFER_W - 4) + "' " + CYN + "|" + RST;

        box[4] = CYN + "|" + " ".repeat(OFFER_W) + "|" + RST;

        String[] arrows = makeColoredArrowLines(arrowKeys[0], arrowKeys[1]);
        box[5] = CYN + "|" + RST + arrows[0] + CYN + "|" + RST;
        box[6] = CYN + "|" + RST + arrows[1] + CYN + "|" + RST;
        box[7] = CYN + "|" + RST + arrows[2] + CYN + "|" + RST;

        box[8] = CYN + "|" + " ".repeat(OFFER_W) + "|" + RST;
        box[9] = roundedBottom(OFFER_W, CYN);

        return box;
    }


    private String[] makeColoredArrowLines(String a1, String a2) {
        boolean has2 = a2 != null && !a2.isEmpty();

        if ("+3c".equals(a1)) {
            return new String[]{
                    " ".repeat(OFFER_W),
                    center(YLW + BOLD + "+3 🍖" + RST, OFFER_W),
                    " ".repeat(OFFER_W)
            };
        }


        if (!has2) {
            String color = arrowSingleColor(a1);
            String[] plain = switch (a1) {
                case "up" -> new String[]{"      /\\      ", "     /  \\     ", "      ||      "};
                case "up2" -> new String[]{"    /\\  /\\    ", "   /  \\/  \\   ", "    ||  ||    "};
                case "down" -> new String[]{"      ||      ", "     \\  /     ", "      \\/      "};
                case "down2" -> new String[]{"    ||  ||    ", "   \\  /\\  /   ", "    \\/  \\/    "};
                default -> new String[]{" ".repeat(OFFER_W), centerStr(a1, OFFER_W), " ".repeat(OFFER_W)};
            };

            return new String[]{
                    color + BOLD + plain[0] + RST,
                    color + BOLD + plain[1] + RST,
                    color + BOLD + plain[2] + RST
            };
        }

        if (a1.equals("up") && a2.equals("down")) {
            return new String[]{
                    GRN + BOLD + "   /\\  " + RST + RED + BOLD + "  ||   " + RST,
                    GRN + BOLD + "  /  \\ " + RST + RED + BOLD + " \\  /  " + RST,
                    GRN + BOLD + "   ||  " + RST + RED + BOLD + "  \\/   " + RST
            };
        }

        if (a1.equals("up2") && a2.equals("down")) {
            return new String[]{
                    GRN + BOLD + " /\\  /\\ " + RST + RED + BOLD + "  ||  " + RST,
                    GRN + BOLD + "/  \\/  \\" + RST + RED + BOLD + " \\  / " + RST,
                    GRN + BOLD + " ||  || " + RST + RED + BOLD + "  \\/  " + RST
            };
        }

        return new String[]{
                " ".repeat(OFFER_W),
                centerStr(a1, OFFER_W),
                " ".repeat(OFFER_W)
        };
    }

    private String arrowSingleColor(String arrow) {
        if (arrow == null) return WHT;
        if (arrow.startsWith("up")) return GRN;
        if (arrow.startsWith("down")) return RED;
        return WHT;
    }

    private void renderPlayer(PlayerView p, boolean isActor, boolean isMe) {
        String col = totemColor(p.getTotemColor());
        String marker = isActor ? GRN + BOLD + "> " + RST : "  ";
        String name = p.getNickname();
        if (name.length() > 14) name = name.substring(0, 13) + ".";
        String nameStr = (isMe ? BOLD : "") + col + String.format("%-14s", name) + RST;
        String foodStr = YLW + String.format("%3d", p.getFood()) + "c" + RST;
        String barStr = foodBar(p.getFood(), 20);
        String ppStr = GRN + String.format("%4d", p.getPrestigePoints()) + " PP" + RST;

        String bc = isActor ? GRN : CYN;
        row(marker + col + "●" + RST + " " + nameStr + "  " + foodStr + "  " + barStr + "  " + ppStr, bc);

        List<Integer> all = new ArrayList<>(p.getCharacterCardIds());
        all.addAll(p.getBuildingCardIds());
        if (!all.isEmpty()) {
            String list = all.stream()
                    .map(id -> {
                        CardCatalog.CardInfo ci = CardCatalog.get(id);
                        return ci.typeName + (ci.detail.isEmpty() ? "" : " " + ci.detail);
                    })
                    .collect(Collectors.joining(" | "));
            if (list.length() > W - 16) list = list.substring(0, W - 18) + "...";
            row("      " + GRY + "Carte: " + RST + WHT + list + RST);
        }
    }

    private String totemColor(TotemColor c) {
        if (c == null) return WHT;
        return switch (c) {
            case RED    -> RED;
            case BLUE   -> BLU;
            case PURPLE -> MGN;
            case YELLOW -> YLW;
            case WHITE  -> WHT;
        };
    }

    private String foodBar(int food, int max) {
        int total = 16;
        int filled = (int) Math.round((double) Math.min(Math.max(food, 0), max) / max * total);
        return GRN + "#".repeat(filled) + GRY + ".".repeat(total - filled) + RST;
    }

    private String stateLabel(String state) {
        return switch (state) {
            case "LobbyState"            -> "LOBBY";
            case "ReadyCheckState"       -> "READY CHECK";
            case "TotemPlacementState"   -> "POSIZIONAMENTO TOTEM";
            case "ActionResolutionState" -> "RISOLUZIONE AZIONI";
            case "EventResolutionState"  -> "RISOLUZIONE EVENTI";
            case "EndRoundState"         -> "FINE ROUND";
            case "SuspendedState"        -> "PAUSA";
            case "EndGameState"          -> "FINE PARTITA";
            default                      -> state;
        };
    }

    private String footerPrompt(ClientModel model) {
        if ("LobbyState".equals(model.getGameState())
                && model.getExpectedPlayers() < 2
                && model.getMyNickname() != null
                && !model.getLobbyPlayers().isEmpty()
                && model.getMyNickname().equals(model.getLobbyPlayers().get(0))) {
            return "  Numero giocatori (2-5) > ";
        }
        return "  > ";
    }

    private int vis(String s) {
        if (s == null) return 0;
        return s.replaceAll("\033\\[[0-9;]*m", "").length();
    }

    private String center(String coloredText, int targetVisW) {
        int textVis = vis(coloredText);
        int pad = Math.max(0, targetVisW - textVis);
        return " ".repeat(pad / 2) + coloredText + " ".repeat(pad - pad / 2);
    }

    private String centerColored(String coloredText, int targetVisW) {
        return center(coloredText, targetVisW);
    }

    private String centerStr(String text, int targetW) {
        if (text == null) text = "";
        if (text.length() > targetW) text = text.substring(0, targetW);
        int pad = targetW - text.length();
        return " ".repeat(pad / 2) + text + " ".repeat(pad - pad / 2);
    }

    private String turnEffectLabel(int numPlayers, int position) {
        return switch (numPlayers) {
            case 2 -> switch (position) {
                case 1 -> "+1 🍖";
                case 2 -> "-1🍖/-2PP";
                default -> "-";
            };
            case 3 -> switch (position) {
                case 1 -> "+2 🍖";
                case 3 -> "-1🍖/-2PP";
                default -> "-";
            };
            case 4 -> switch (position) {
                case 1 -> "+2 🍖";
                case 2 -> "+1 🍖";
                case 4 -> "-1🍖/-2PP";
                default -> "-";
            };
            case 5 -> switch (position) {
                case 1 -> "+3 🍖";
                case 2 -> "+1 🍖";
                case 5 -> "-1🍖/-2PP";
                default -> "-";
            };
            default -> "-";
        };
    }



    private String turnEffectColor(String effect) {
        if (effect.startsWith("+")) return GRN + BOLD;
        if (effect.startsWith("-")) return RED + BOLD;
        return GRY;
    }

    private String titledTop(String title, int width, String color) {
        String label = " " + title + " ";
        int dashTotal = Math.max(0, width - vis(label));
        int left = dashTotal / 2;
        int right = dashTotal - left;
        return color + "•" + "-".repeat(left) + label + "-".repeat(right) + "•" + RST;
    }

    private String roundedBottom(int width, String color) {
        return color + "'" + "-".repeat(width) + "'" + RST;
    }



    private String padRight(String text, int targetVisW) {
        int pad = Math.max(0, targetVisW - vis(text));
        return text + " ".repeat(pad);
    }

    private String ellipsize(String text, int maxVisW) {
        if (text == null) return "";
        if (vis(text) <= maxVisW) return text;
        if (maxVisW <= 1) return ".";
        return text.substring(0, Math.min(text.length(), maxVisW - 1)) + ".";
    }


    private String dashedLine(int width) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) {
            sb.append(i % 2 == 0 ? "─" : " ");
        }
        return sb.toString();
    }

    private String dashedAsciiLine(int width) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) {
            sb.append(i % 2 == 0 ? "-" : " ");
        }
        return sb.toString();
    }

}
