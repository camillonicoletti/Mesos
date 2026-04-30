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

    // ── ANSI ─────────────────────────────────────────────────────────────────
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
    private static final String ORG  = "\033[33m";

    private static final int W = 110;          // larghezza interna del box
    private static final int CARD_W = 12;      // larghezza interna delle card (tot 14 con bordi)
    private static final int CARD_GAP = 1;     // spazi tra una card e l'altra
    private static final int OFFER_W = 10;     // larghezza interna offer tile (tot 12 con bordi)
    private static final int ORDER_W = 11;     // larghezza interna tessera ordine

    // tile A-G del JSON offer_tiles.json
    private static final List<Character> TILE_ORDER = List.of('A','B','C','D','E','F','G');

    private static final Map<Character, String[]> TILE_ARROWS = new LinkedHashMap<>() {{
        put('A', new String[]{"+3c", ""});       // food
        put('B', new String[]{"↓",   ""});       // lower 1
        put('C', new String[]{"↑",   ""});       // upper 1
        put('D', new String[]{"↓↓",  ""});       // lower 2
        put('E', new String[]{"↑",   "↓"});      // upper 1 + lower 1
        put('F', new String[]{"↑↑",  ""});       // upper 2
        put('G', new String[]{"↑↑",  "↓"});      // upper 2 + lower 1
    }};

    private static final Map<Character, Integer> TILE_MIN = Map.of(
            'A', 5, 'B', 2, 'C', 2, 'D', 3, 'E', 2, 'F', 2, 'G', 4
    );

    public Renderer(ClientModel model) { }

    // ── entry point ──────────────────────────────────────────────────────────

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

    // ── chrome ───────────────────────────────────────────────────────────────

    private void clear() {
        System.out.print("\033[2J\033[H");
        System.out.flush();
    }

    private void header(String state, ClientModel model) {
        boxLine('╔', '═', '╗');
        row(center(WHT + BOLD + "M E S O S   —   TUI" + RST, W));
        if (model.getMyNickname() != null && !model.getMyNickname().isBlank()) {
            row(GRY + center("Giocatore: " + WHT + BOLD + model.getMyNickname() + RST + GRY, W) + RST);
        } else {
            blank();
        }
        if (state != null && model.getCurrentEra() != null) {
            String sub = "Era " + model.getCurrentEra().name()
                    + "  ·  Round " + model.getRoundNumber()
                    + "  ·  " + stateLabel(state);
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

    // ── login / lobby / ready / suspended / end ──────────────────────────────

    private void loginScreen() {
        blank();
        row(center(WHT + BOLD + "Benvenuto in MESOS!" + RST, W));
        blank();
        row("  " + GRY + "Inserisci nickname e colore totem per entrare nel gioco." + RST);
        row("  " + GRY + "Se eri già in partita puoi scegliere la riconnessione." + RST);
        blank();
        row("  Colori disponibili:  "
                + RED  + BOLD + "RED"    + RST + "   "
                + BLU  + BOLD + "BLUE"   + RST + "   "
                + MGN  + BOLD + "PURPLE" + RST + "   "
                + YLW  + BOLD + "YELLOW" + RST + "   "
                + WHT  + BOLD + "WHITE"  + RST);
        blank();
    }

    private void lobbyScreen(ClientModel model) {
        int currentPlayers = model.getLobbyPlayers().size();
        int expectedPlayers = model.getExpectedPlayers();
        boolean iAmFirstPlayer = model.getMyNickname() != null
                && !model.getLobbyPlayers().isEmpty()
                && model.getMyNickname().equals(model.getLobbyPlayers().get(0));

        blank();
        row("  " + WHT + BOLD + "LOBBY" + RST + "  " + GRY
                + currentPlayers + " / " + (expectedPlayers > 0 ? expectedPlayers : "?")
                + " giocatori" + RST);
        blank();
        for (String p : model.getLobbyPlayers()) {
            row("    " + GRN + "●" + RST + "  " + WHT + p + RST);
        }
        blank();
        if (expectedPlayers == 0) {
            if (iAmFirstPlayer) {
                row("  " + YLW + BOLD + "Sei il primo giocatore!" + RST);
                row("  " + GRY + "Scegli il numero di partecipanti:  " + RST + YLW + "players <2-5>" + RST);
            } else {
                row("  " + GRY + "In attesa che il primo giocatore scelga il numero di partecipanti..." + RST);
            }
        } else if (currentPlayers < expectedPlayers) {
            int missing = expectedPlayers - currentPlayers;
            row("  " + GRY + "In attesa di " + missing
                    + (missing == 1 ? " giocatore..." : " giocatori...") + RST);
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
            String status = ready
                    ? GRN + "✓  Pronto              "
                    : YLW + "⌛  In attesa...        ";
            String name = isMe
                    ? CYN + BOLD + p + "  (tu)" + RST
                    : WHT + p + RST;
            row("    " + status + RST + name);
        }
        blank();
        boolean iAmReady = model.getReadyPlayers().contains(model.getMyNickname());
        if (iAmReady) {
            row("  " + GRN + BOLD + "✓ HAI CONFERMATO" + RST + GRY + "  —  in attesa degli altri giocatori..." + RST);
        } else {
            row("  " + YLW + BOLD + "▶▶▶  TOCCA A TE!  ◀◀◀" + RST);
            row("  " + WHT + "Digita  " + RST + YLW + BOLD + "ready" + RST + WHT + "  per confermare la tua presenza." + RST);
        }
        blank();
    }

    private void suspendedScreen() {
        blank();
        row(center(RED + BOLD + "⚠  PARTITA IN PAUSA" + RST, W));
        blank();
        row(center(GRY + "In attesa che almeno un altro giocatore si riconnetta..." + RST, W));
        blank();
    }

    private void endGameScreen(ClientModel model) {
        blank();
        row(center(YLW + BOLD + "★  FINE PARTITA  ★" + RST, W));
        blank();
        row("  Vincitore:  " + GRN + BOLD + model.getWinnerNickname() + RST);
        blank();
        int rank = 1;
        for (Map.Entry<String, Integer> e : model.getFinalScores().entrySet()
                .stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).toList()) {
            String medal = switch (rank) {
                case 1  -> YLW + "1°" + RST + " ";
                case 2  -> GRY + "2°" + RST + " ";
                default -> DIM + rank + "° " + RST;
            };
            row("  " + medal + WHT + String.format("%-16s", e.getKey()) + RST
                    + "  " + YLW + e.getValue() + " PP" + RST);
            rank++;
        }
        blank();
    }

    private void simpleScreen(String msg, ClientModel model) {
        blank();
        row("  " + YLW + msg + RST);
        blank();
        for (PlayerView p : model.getPlayers().values()) {
            String col = totemColorCode(p.getTotemColor());
            row("  " + col + "●" + RST + " " + WHT + String.format("%-16s", p.getNickname()) + RST
                    + "  " + YLW + p.getFood() + "c" + RST
                    + "  " + foodBar(p.getFood(), 20)
                    + "  " + GRN + p.getPrestigePoints() + " PP" + RST);
        }
        blank();
    }

    // ── GAME SCREEN ──────────────────────────────────────────────────────────

    private void gameScreen(ClientModel model, boolean totemPhase) {
        blank();

        // Turn order con totem colorati
        List<String> order = model.getTurnOrder();
        if (!order.isEmpty()) {
            StringBuilder sb = new StringBuilder("  " + GRY + "Turno: " + RST);
            for (int i = 0; i < order.size(); i++) {
                if (i > 0) sb.append(GRY + "  →  " + RST);
                PlayerView pv = model.getPlayers().get(order.get(i));
                String col = pv != null ? totemColorCode(pv.getTotemColor()) : WHT;
                sb.append(col).append("● ").append(BOLD).append(order.get(i)).append(RST);
            }
            row(sb.toString());
        }

        // Banner "tocca a te"
        if (totemPhase) {
            String current = model.getCurrentTotemPlacementPlayer();
            if (current != null && current.equals(model.getMyNickname())) {
                row(center(YLW + BOLD + "▶▶▶  TOCCA A TE — PIAZZA IL TOTEM!  ◀◀◀" + RST, W));
            } else if (current != null) {
                row("  " + GRY + "In attesa del piazzamento di " + YLW + current + RST + GRY + "..." + RST);
            }
        } else {
            if (model.isMyTurn()) {
                row(center(GRN + BOLD + "▶▶▶  TOCCA A TE!  ◀◀◀" + RST, W));
            }
        }

        blank();

        // ── UPPER ROW (personaggi + edifici + eventi mescolati) ──────────────
        List<Integer> upperAll = new ArrayList<>(model.getUpperRowCharacterIds());
        upperAll.addAll(model.getUpperRowBuildingIds());
        renderCardRow(upperAll);

        blank();

        // ── ZONA CENTRALE: tessera ordine + offer tile ───────────────────────
        renderCenterZone(model);

        blank();

        // ── LOWER ROW (personaggi + edifici + eventi mescolati) ──────────────
        List<Integer> lowerAll = new ArrayList<>(model.getLowerRowCharacterIds());
        lowerAll.addAll(model.getLowerRowBuildingIds());
        renderCardRow(lowerAll);

        blank();

        // ── GIOCATORI ────────────────────────────────────────────────────────
        String currentActor = totemPhase
                ? model.getCurrentTotemPlacementPlayer()
                : (!order.isEmpty() ? order.get(0) : null);
        for (PlayerView p : model.getPlayers().values()) {
            boolean isActor = p.getNickname().equals(currentActor);
            renderPlayer(p, isActor, p.getNickname().equals(model.getMyNickname()));
        }

        blank();
        if (totemPhase) {
            row("  " + GRY + "place <lettera>   ex: place B" + RST);
        } else {
            row("  " + GRY + "pick <id> upper|lower   —   build <id> upper|lower   —   help" + RST);
        }
        blank();
    }

    // ── card row (carte ASCII affiancate) ────────────────────────────────────

    private void renderCardRow(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            row("  " + GRY + "(nessuna carta)" + RST);
            return;
        }

        // Limito quante carte stanno per riga in base alla larghezza
        int slotW = CARD_W + 2 + CARD_GAP; // 12 + bordi + gap
        int maxPerLine = Math.max(1, (W - 4) / slotW);
        for (int start = 0; start < ids.size(); start += maxPerLine) {
            List<Integer> slice = ids.subList(start, Math.min(start + maxPerLine, ids.size()));
            renderCardRowChunk(slice);
            if (start + maxPerLine < ids.size()) blank();
        }
    }

    private void renderCardRowChunk(List<Integer> ids) {
        // Costruisco 7 righe della "card" per ogni id
        String[] lines = new String[7];
        for (int i = 0; i < 7; i++) lines[i] = "  ";
        for (int idx = 0; idx < ids.size(); idx++) {
            String[] cardLines = drawCard(ids.get(idx));
            for (int i = 0; i < 7; i++) {
                lines[i] += cardLines[i];
                if (idx < ids.size() - 1) lines[i] += " ".repeat(CARD_GAP);
            }
        }
        for (String l : lines) row(l);
    }

    private String[] drawCard(int id) {
        CardCatalog.CardInfo c = CardCatalog.get(id);
        String topCol;     // colore bordo / testo per tipo
        String headerLine; // riga "tipo"
        String detailLine; // riga "qualificatore"
        switch (c.kind) {
            case CHARACTER -> {
                topCol = WHT;
                headerLine = c.typeName;
                detailLine = c.detail;
            }
            case BUILDING -> {
                topCol = YLW;
                headerLine = c.typeName;
                detailLine = c.foodCost + "c +" + c.pointsEnd + "PP";
            }
            case EVENT -> {
                topCol = MGN;
                headerLine = "Event";
                detailLine = c.detail;
            }
            default -> {
                topCol = GRY;
                headerLine = "?";
                detailLine = "";
            }
        }

        String[] out = new String[7];
        String border = topCol;
        out[0] = border + "┌" + "─".repeat(CARD_W) + "┐" + RST;
        out[1] = border + "│" + RST + topCol + centerStr("#" + c.id, CARD_W) + RST + border + "│" + RST;
        out[2] = border + "│" + RST + " ".repeat(CARD_W) + border + "│" + RST;
        out[3] = border + "│" + RST + WHT + BOLD + centerStr(headerLine, CARD_W) + RST + border + "│" + RST;
        // riga effetto: gli edifici hanno 2 righe (cost+PP, fxName), altri solo detailLine
        if (c.kind == CardCatalog.Kind.BUILDING) {
            out[4] = border + "│" + RST + GRY + centerStr(detailLine, CARD_W) + RST + border + "│" + RST;
            out[5] = border + "│" + RST + GRY + centerStr(c.detail, CARD_W) + RST + border + "│" + RST;
        } else {
            out[4] = border + "│" + RST + GRY + centerStr(detailLine, CARD_W) + RST + border + "│" + RST;
            out[5] = border + "│" + RST + DIM + centerStr(c.era, CARD_W) + RST + border + "│" + RST;
        }
        // ultima riga: era (per edifici) o niente
        if (c.kind == CardCatalog.Kind.BUILDING) {
            out[6] = border + "└" + RST + DIM + centerStr(c.era, CARD_W).replace(' ', '─').replace(' ', '─') + RST + border + "┘" + RST;
            // semplifico: bordo basso senza era
            out[6] = border + "└" + "─".repeat(CARD_W) + "┘" + RST;
        } else {
            out[6] = border + "└" + "─".repeat(CARD_W) + "┘" + RST;
        }
        return out;
    }

    // ── zona centrale: ordine + offer tile ──────────────────────────────────

    private void renderCenterZone(ClientModel model) {
        // Box ordine: 6 righe
        String[] orderBox = drawOrderBox(model);

        // Offer tiles attive
        int numPlayers = Math.max(2, model.getPlayers().size());
        List<Character> active = TILE_ORDER.stream()
                .filter(c -> TILE_MIN.get(c) <= numPlayers)
                .collect(Collectors.toList());

        Map<Character, String> occ = model.getTotemOnTiles();

        // costruisco offer tile (6 righe ciascuna)
        String[][] tiles = new String[active.size()][];
        for (int i = 0; i < active.size(); i++) {
            char c = active.get(i);
            String occupant = occ.get(c);
            String occCol = WHT;
            if (occupant != null) {
                PlayerView pv = model.getPlayers().get(occupant);
                if (pv != null) occCol = totemColorCode(pv.getTotemColor());
            }
            tiles[i] = drawOfferTile(c, occupant, occCol);
        }

        // unisco riga per riga: orderBox (3 spazi) tiles
        for (int line = 0; line < 6; line++) {
            StringBuilder sb = new StringBuilder("  ");
            sb.append(orderBox[line]);
            sb.append("   ");
            for (int i = 0; i < tiles.length; i++) {
                sb.append(tiles[i][line]);
                if (i < tiles.length - 1) sb.append(" ");
            }
            row(sb.toString());
        }
    }

    private String[] drawOrderBox(ClientModel model) {
        List<String> order = model.getTurnOrder();
        String[] box = new String[6];
        box[0] = CYN + "┌" + "─".repeat(ORDER_W) + "┐" + RST;
        for (int i = 1; i <= 4; i++) {
            String content;
            if (i - 1 < order.size()) {
                String name = order.get(i - 1);
                PlayerView pv = model.getPlayers().get(name);
                String col = pv != null ? totemColorCode(pv.getTotemColor()) : WHT;
                String shortName = name.length() > ORDER_W - 3 ? name.substring(0, ORDER_W - 4) + "…" : name;
                content = " " + i + "." + col + shortName + RST;
                content = content + " ".repeat(Math.max(0, ORDER_W - vis(content)));
            } else {
                content = " ".repeat(ORDER_W);
            }
            box[i] = CYN + "│" + RST + content + CYN + "│" + RST;
        }
        box[5] = CYN + "└" + "─".repeat(ORDER_W) + "┘" + RST;
        return box;
    }

    private String[] drawOfferTile(char letter, String occupant, String occCol) {
        String[] arrows = TILE_ARROWS.getOrDefault(letter, new String[]{"", ""});
        String[] box = new String[6];
        // header con lettera tile (in alto a sinistra DIM)
        box[0] = CYN + "┌" + "─".repeat(OFFER_W) + "┐" + RST;
        // riga 1: occupante (centrato) o vuoto
        String occLine;
        if (occupant != null) {
            String shortOcc = occupant.length() > OFFER_W - 2 ? occupant.substring(0, OFFER_W - 3) + "…" : occupant;
            occLine = occCol + "●" + shortOcc + RST;
            occLine = centerColored(occLine, OFFER_W);
        } else {
            occLine = " ".repeat(OFFER_W);
        }
        box[1] = CYN + "│" + RST + occLine + CYN + "│" + RST;
        // riga 2: lettera (in alto-sinistra)
        String letterLine = " " + DIM + letter + RST + " ".repeat(OFFER_W - 2);
        box[2] = CYN + "│" + RST + letterLine + CYN + "│" + RST;
        // riga 3: freccia 1
        box[3] = CYN + "│" + RST + centerColored(arrowColor(arrows[0]), OFFER_W) + CYN + "│" + RST;
        // riga 4: freccia 2 (o vuoto)
        box[4] = CYN + "│" + RST + centerColored(arrowColor(arrows[1]), OFFER_W) + CYN + "│" + RST;
        box[5] = CYN + "└" + "─".repeat(OFFER_W) + "┘" + RST;
        return box;
    }

    private String arrowColor(String arrow) {
        if (arrow == null || arrow.isEmpty()) return "";
        if (arrow.contains("↑")) return GRN + BOLD + arrow + RST;
        if (arrow.contains("↓")) return BLU + BOLD + arrow + RST;
        return YLW + BOLD + arrow + RST; // food
    }

    // ── giocatori ────────────────────────────────────────────────────────────

    private void renderPlayer(PlayerView p, boolean isActor, boolean isMe) {
        String col = totemColorCode(p.getTotemColor());
        String marker = isActor ? GRN + BOLD + "▶ " + RST : "  ";
        String name = p.getNickname();
        if (name.length() > 14) name = name.substring(0, 13) + "…";
        String nameStr = (isMe ? BOLD : "") + col + String.format("%-14s", name) + RST;

        String foodStr = YLW + String.format("🍖%3d", p.getFood()) + RST;
        String barStr = foodBar(p.getFood(), 20);
        String ppStr  = GRN + String.format("★%3d", p.getPrestigePoints()) + " PP" + RST;

        String borderColor = isActor ? GRN : CYN;
        row(marker + col + "●" + RST + " " + nameStr + "  " + foodStr + "  " + barStr + "  " + ppStr, borderColor);

        // Carte in mano (riassunto compatto)
        List<Integer> all = new ArrayList<>();
        all.addAll(p.getCharacterCardIds());
        all.addAll(p.getBuildingCardIds());
        if (!all.isEmpty()) {
            String list = all.stream()
                    .map(id -> {
                        CardCatalog.CardInfo ci = CardCatalog.get(id);
                        return ci.typeName + (ci.detail.isEmpty() ? "" : " " + ci.detail);
                    })
                    .collect(Collectors.joining(" · "));
            if (list.length() > W - 14) list = list.substring(0, W - 16) + "…";
            row("      " + GRY + "In mano: " + RST + WHT + list + RST);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String totemColorCode(TotemColor c) {
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
        return GRN + "█".repeat(filled) + GRY + "░".repeat(total - filled) + RST;
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
        return s.replaceAll("\033\\[[0-9;]*m", "").length();
    }

    private String center(String coloredText, int targetVisW) {
        int textVis = vis(coloredText);
        int pad = Math.max(0, targetVisW - textVis);
        return " ".repeat(pad / 2) + coloredText + " ".repeat(pad - pad / 2);
    }

    private String centerColored(String coloredText, int targetVisW) {
        int textVis = vis(coloredText);
        int pad = Math.max(0, targetVisW - textVis);
        return " ".repeat(pad / 2) + coloredText + " ".repeat(pad - pad / 2);
    }

    private String centerStr(String text, int targetW) {
        if (text.length() > targetW) text = text.substring(0, targetW);
        int pad = targetW - text.length();
        return " ".repeat(pad / 2) + text + " ".repeat(pad - pad / 2);
    }
}