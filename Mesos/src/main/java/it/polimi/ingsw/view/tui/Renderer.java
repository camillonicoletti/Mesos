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
    private static final String BLINK = "\033[5m";
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

        if (state == null) {
            loginScreen(model);
            return;
        }

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
            loginScreen(model);
        } else switch (state) {
            case "LobbyState"            -> lobbyScreen(model);
            case "ReadyCheckState"       -> renderReadyCheck(model);
            case "StartGameState"        -> gameScreen(model, true);
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

        row(center(RED + BOLD + "███╗   ███╗   ███████╗   ███████╗    ██████╗    ███████╗" + RST, W));
        row(center(RED + BOLD + "████╗ ████║   ██╔════╝   ██╔════╝   ██╔═══██╗   ██╔════╝" + RST, W));
        row(center(RED + BOLD + "██╔████╔██║   █████╗     ███████╗   ██║   ██║   ███████╗" + RST, W));
        row(center(RED + BOLD + "██║╚██╔╝██║   ██╔══╝     ╚════██║   ██║   ██║   ╚════██║" + RST, W));
        row(center(RED + BOLD + "██║ ╚═╝ ██║   ███████╗   ███████║   ╚██████╔╝   ███████║" + RST, W));
        row(center(RED + BOLD + "╚═╝     ╚═╝   ╚══════╝   ╚══════╝    ╚═════╝    ╚══════╝" + RST, W));

        if (model.getMyNickname() != null && !model.getMyNickname().isBlank()) {
            row(center(GRN + BOLD + "GIOCATORE: " + model.getMyNickname().toUpperCase() + RST, W));
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

        blank();
    }



    private void footer(ClientModel model) {
        boxLine('╚', '═', '╝');
        printCommandPanel(model);
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

    private void loginScreen(ClientModel model) {
        printMesosLogoPlain();

        System.out.println();
        System.out.println(centerPlain(WHT + BOLD + "Benvenuto in MESOS!" + RST, W));
        System.out.println();
        System.out.println(GRY + "Inserisci nickname e colore totem per entrare nel gioco." + RST);
        System.out.println(GRY + "Se eri gia in partita puoi scegliere la riconnessione." + RST);
        System.out.println();
        System.out.println(GRN + BOLD + "Colori disponibili:" + RST + "  " + availableColorLabels(model));
        System.out.println();
    }


    private void lobbyScreen(ClientModel model) {
        int cur = model.getLobbyPlayers().size();
        int exp = model.getExpectedPlayers();

        boolean playersCountChosen = exp >= 2;

        boolean iAmFirst = model.getMyNickname() != null
                && !model.getLobbyPlayers().isEmpty()
                && model.getMyNickname().equals(model.getLobbyPlayers().get(0));

        blank();
        row("  " + WHT + BOLD + "LOBBY" + RST + "  " + GRY
                + cur + " / " + (playersCountChosen ? exp : "?")
                + " giocatori" + RST);
        blank();

        for (String p : model.getLobbyPlayers()) {
            boolean ready = model.getReadyPlayers().contains(p);
            String readyBadge = ready ? GRN + "✓ pronto" + RST : GRY + "⌛ attesa" + RST;
            boolean isLobbyLeader = !model.getLobbyPlayers().isEmpty()
                    && p.equals(model.getLobbyPlayers().get(0));
            String leaderBadge = isLobbyLeader ? YLW + "👑 " + RST : "";
            row("    " + GRN + "●" + RST + "  " + leaderBadge + WHT + p + RST + "  " + readyBadge);
        }

        blank();

        if (!playersCountChosen) {
            if (iAmFirst) {
                row("  " + YLW + BOLD + "Sei il primo giocatore!" + RST);
                row("  " + GRY + "Scegli il numero di partecipanti:  " + RST + YLW + "players <2-5>" + RST);
                row("  " + GRY + "In lobby puoi anche usare: " + RST + YLW + "ready" + RST + GRY + " / " + RST + YLW + "unready" + RST + GRY + " / " + RST + YLW + "quit" + RST);
            } else {
                row("  " + GRY + "In attesa che il primo giocatore scelga il numero di partecipanti..." + RST);
                row("  " + GRY + "Puoi gia confermare con: " + RST + YLW + "ready" + RST + GRY + " / " + RST + YLW + "unready" + RST + GRY + " / " + RST + YLW + "quit" + RST);
            }
        } else if (cur < exp) {
            int miss = exp - cur;
            row("  " + GRY + "In attesa di " + miss
                    + (miss == 1 ? " giocatore..." : " giocatori...") + RST);
            row("  " + GRY + "Comandi disponibili: " + RST + YLW + "ready" + RST + GRY + " / " + RST + YLW + "unready" + RST + GRY + " / " + RST + YLW + "quit" + RST);
            if (iAmFirst) {
                row("  " + GRY + "Sei il capo lobby: puoi ancora modificare il numero con " + RST + YLW + "players <2-5>" + RST);
            }
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
        String current = null;

        if (!order.isEmpty()) {
            current = totemPhase ? currentPlacementPlayer(model) : order.get(0);
            PlayerView pv = current != null ? model.getPlayers().get(current) : null;
            String col = pv != null ? totemColor(pv.getTotemColor()) : WHT;

            row(center(WHT + BOLD + "ORA TOCCA A " + RST
                    + col + BOLD + (current != null ? current.toUpperCase() : "?") + RST, W));
        }



        if (totemPhase) {
            String cur = currentPlacementPlayer(model);
            if (cur != null && cur.equals(model.getMyNickname())) {
                row(center(YLW + BOLD + BLINK + ">>>  TOCCA A TE — PIAZZA IL TOTEM!  <<<" + RST, W));

            } else if (cur != null) {
                row("  " + GRY + "In attesa del piazzamento di " + YLW + cur + RST + GRY + "..." + RST);
            }
        } else if (model.isMyTurn()) {
            row(center(GRN + BOLD + BLINK + ">>>  TOCCA A TE!  <<<" + RST, W));

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
                ? currentPlacementPlayer(model)
                : (!order.isEmpty() ? order.get(0) : null);


        row("  " + CYN + BOLD + "GIOCATORI" + RST);

        List<PlayerView> sortedPlayers = model.getPlayers().values().stream()
                .sorted((a, b) -> Integer.compare(b.getPrestigePoints(), a.getPrestigePoints()))
                .toList();

        for (int i = 0; i < sortedPlayers.size(); i++) {
            PlayerView p = sortedPlayers.get(i);
            renderPlayer(
                    p,
                    i + 1,
                    p.getNickname().equals(currentActor),
                    p.getNickname().equals(model.getMyNickname())
            );
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
        List<String> placedPlayers = new ArrayList<>(model.getTotemOnTiles().values());

        int numPlayers = Math.max(2, Math.max(model.getPlayers().size(), order.size()));
        int visibleSlots = Math.max(numPlayers, order.size());

        String[] box = new String[2 + visibleSlots * 3];
        int line = 0;

        box[line++] = titledTop("ORDINE TURNO", ORDER_W, CYN);

        String currentPlacementPlayer = model.getCurrentTotemPlacementPlayer();

        for (int i = 0; i < visibleSlots; i++) {
            String name = i < order.size() ? order.get(i) : "";

            if (placedPlayers.contains(name)) {
                name = "";
            }

            PlayerView pv = name.isBlank() ? null : model.getPlayers().get(name);
            String col = pv != null ? totemColor(pv.getTotemColor()) : GRY;
            boolean current = name.equals(currentPlacementPlayer);

            String marker = current ? GRN + BOLD + "▶" + RST : " ";
            String shownName = name.isBlank() ? "" : ellipsize(name, 12);

            String playerText = name.isBlank()
                    ? " ".repeat(22)
                    : marker + " " + (i + 1) + "  " + col + "●" + RST + " "
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

    private void renderPlayer(PlayerView p, int rank, boolean isActor, boolean isMe) {
        String col = totemColor(p.getTotemColor());
        String marker = isActor ? GRN + BOLD + "▶" + RST : " ";
        String meTag = isMe ? CYN + BOLD + " tu" + RST : "";

        String rankStr = rankLabel(rank);
        String name = ellipsize(p.getNickname(), 14);
        String nameStr = col + BOLD + String.format("%-14s", name) + RST;

        String foodStr = YLW + BOLD + "🍖 " + String.format("%2d", p.getFood()) + RST;
        String barStr = foodBar(p.getFood(), 20);

        String ppColor = p.getPrestigePoints() < 0 ? RED : GRN;
        String ppStr = ppColor + BOLD + "🌟 " + String.format("%3d", p.getPrestigePoints()) + RST;

        row("  " + rankStr + "  " + marker + " "
                + nameStr
                + "   " + foodStr
                + "   " + barStr
                + "   " + ppStr
                + meTag);

        List<Integer> all = new ArrayList<>(p.getCharacterCardIds());
        all.addAll(p.getBuildingCardIds());

        if (!all.isEmpty()) {
            String list = all.stream()
                    .map(id -> {
                        CardCatalog.CardInfo ci = CardCatalog.get(id);
                        return ci.typeName + (ci.detail.isEmpty() ? "" : " " + ci.detail);
                    })
                    .collect(Collectors.joining(" · "));

            if (vis(list) > W - 18) {
                list = ellipsize(list, W - 18);
            }

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
        int total = 18;
        int safeMax = Math.max(1, max);
        int filled = (int) Math.round((double) Math.min(Math.max(food, 0), safeMax) / safeMax * total);

        return GRN + "█".repeat(filled)
                + GRY + "░".repeat(total - filled)
                + RST;
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

    private String rankLabel(int rank) {
        String label = rank + "°";
        return switch (rank) {
            case 1 -> YLW + BOLD + label + RST;
            case 2 -> WHT + BOLD + label + RST;
            case 3 -> GRY + BOLD + label + RST;
            default -> GRY + label + RST;
        };
    }

    private void printCommandPanel(ClientModel model) {
        List<String[]> commands = commandsForState(model);

        if (commands.isEmpty()) {
            System.out.println();
            return;
        }

        int panelW = 72;
        String title = " COMANDI ";
        int dashTotal = panelW - title.length();
        int left = dashTotal / 2;
        int right = dashTotal - left;

        System.out.println();
        System.out.println(CYN + "•" + "-".repeat(left) + title + "-".repeat(right) + "•" + RST);

        for (String[] cmd : commands) {
            String leftCol = cmd[0];
            String rightCol = cmd[1];

            String line = " " + String.format("%-20s", leftCol) + " " + rightCol;
            int pad = Math.max(0, panelW - vis(line));

            System.out.println(CYN + "|" + RST
                    + WHT + BOLD + line.substring(0, Math.min(line.length(), Math.min(21, line.length()))) + RST
                    + (line.length() > 21 ? GRY + line.substring(21) + RST : "")
                    + " ".repeat(pad)
                    + CYN + "|" + RST);
        }

        System.out.println(CYN + "'" + "-".repeat(panelW) + "'" + RST);
        System.out.println();
    }

    private List<String[]> commandsForState(ClientModel model) {
        String state = model.getGameState();

        if (state == null) {
            return List.of(
                    new String[]{"nickname", "inserisci il tuo nome quando richiesto"},
                    new String[]{"colore", "scegli RED, BLUE, PURPLE, YELLOW o WHITE"}
            );
        }

        return switch (state) {
            case "LobbyState" -> {
                boolean canSetPlayers = model.getMyNickname() != null
                        && !model.getLobbyPlayers().isEmpty()
                        && model.getMyNickname().equals(model.getLobbyPlayers().get(0));

                if (canSetPlayers) {
                    yield List.of(
                            new String[]{"ready / unready", "conferma o annulla la tua presenza"},
                            new String[]{"players <2-5>", "scegli o modifica il numero di giocatori"},
                            new String[]{"quit", "esci dalla lobby"}
                    );
                }

                yield List.of(
                        new String[]{"ready / unready", "conferma o annulla la tua presenza"},
                        new String[]{"attendi", "il capo lobby sceglie il numero"},
                        new String[]{"quit", "esci dalla lobby"}
                );
            }

            case "ReadyCheckState" -> List.of(
                    new String[]{"ready", "conferma la tua presenza"},
                    new String[]{"unready", "annulla la conferma"},
                    new String[]{"quit", "esci dalla partita"}
            );

            case "StartGameState", "TotemPlacementState" -> List.of(
                    new String[]{"place <tile>", "piazza il totem su una tile, es. place B"},
                    new String[]{"help", "mostra aiuto rapido"},
                    new String[]{"quit", "esci dalla partita"}
            );

            case "ActionResolutionState" -> List.of(
                    new String[]{"pick <id> upper", "prendi personaggio/evento dalla riga sopra"},
                    new String[]{"pick <id> lower", "prendi personaggio/evento dalla riga sotto"},
                    new String[]{"build <id> upper", "costruisci edificio dalla riga sopra"},
                    new String[]{"build <id> lower", "costruisci edificio dalla riga sotto"},
                    new String[]{"help", "mostra aiuto rapido"},
                    new String[]{"quit", "esci dalla partita"}
            );


            case "EventResolutionState" -> List.of(
                    new String[]{"attendi", "il server sta risolvendo gli eventi"},
                    new String[]{"quit", "esci dalla partita"}
            );

            case "EndRoundState" -> List.of(
                    new String[]{"attendi", "preparazione del prossimo round"},
                    new String[]{"quit", "esci dalla partita"}
            );

            case "SuspendedState" -> List.of(
                    new String[]{"attendi", "la partita riprende quando i giocatori tornano"},
                    new String[]{"quit", "esci dalla partita"}
            );

            case "EndGameState" -> List.of(
                    new String[]{"invio", "chiudi la TUI"},
                    new String[]{"quit", "esci"}
            );

            default -> List.of(
                    new String[]{"help", "mostra aiuto rapido"},
                    new String[]{"quit", "esci dalla partita"}
            );
        };
    }

    private String currentPlacementPlayer(ClientModel model) {
        List<String> placedPlayers = new ArrayList<>(model.getTotemOnTiles().values());

        for (String nickname : model.getTurnOrder()) {
            if (!placedPlayers.contains(nickname)) {
                return nickname;
            }
        }

        return null;
    }

    private void printMesosLogoPlain() {
        System.out.println(centerPlain(RED + BOLD + "███╗   ███╗   ███████╗   ███████╗    ██████╗    ███████╗" + RST, W));
        System.out.println(centerPlain(RED + BOLD + "████╗ ████║   ██╔════╝   ██╔════╝   ██╔═══██╗   ██╔════╝" + RST, W));
        System.out.println(centerPlain(RED + BOLD + "██╔████╔██║   █████╗     ███████╗   ██║   ██║   ███████╗" + RST, W));
        System.out.println(centerPlain(RED + BOLD + "██║╚██╔╝██║   ██╔══╝     ╚════██║   ██║   ██║   ╚════██║" + RST, W));
        System.out.println(centerPlain(RED + BOLD + "██║ ╚═╝ ██║   ███████╗   ███████║   ╚██████╔╝   ███████║" + RST, W));
        System.out.println(centerPlain(RED + BOLD + "╚═╝     ╚═╝   ╚══════╝   ╚══════╝    ╚═════╝    ╚══════╝" + RST, W));
    }

    private String centerPlain(String text, int width) {
        int pad = Math.max(0, width - vis(text));
        return " ".repeat(pad / 2) + text;
    }

    private String availableColorLabels(ClientModel model) {
        List<TotemColor> taken = new ArrayList<>(model.getLobbyPlayerColors().values());

        List<String> labels = new ArrayList<>();
        for (TotemColor color : TotemColor.values()) {
            if (!taken.contains(color)) {
                labels.add(colorLabel(color));
            }
        }

        return String.join("   ", labels);
    }

    private String colorLabel(TotemColor color) {
        return switch (color) {
            case RED    -> RED + BOLD + "RED" + RST;
            case BLUE   -> BLU + BOLD + "BLUE" + RST;
            case PURPLE -> MGN + BOLD + "PURPLE" + RST;
            case YELLOW -> YLW + BOLD + "YELLOW" + RST;
            case WHITE  -> WHT + BOLD + "WHITE" + RST;
        };
    }





}
