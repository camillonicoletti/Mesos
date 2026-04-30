package it.polimi.ingsw.view.tui;

import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.PlayerView;

import java.util.List;
import java.util.Map;

public class Renderer {

    private static final String RST  = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String DIM  = "\033[2m";
    private static final String CYN  = "\033[96m";   // bordi
    private static final String YLW  = "\033[93m";   // valori
    private static final String GRN  = "\033[92m";   // positivo / barre
    private static final String RED  = "\033[91m";   // errori
    private static final String MGN  = "\033[95m";   // magenta
    private static final String BLU  = "\033[94m";   // blu
    private static final String WHT  = "\033[97m";   // testo principale
    private static final String GRY  = "\033[90m";   // testo secondario

    private static final int W = 78;

    public Renderer(ClientModel model) { }

    // ── entry point ──────────────────────────────────────────────────────────

    public void render(ClientModel model) {
        clear();
        String state = model.getGameState();
        header(state, model);
        body(state, model);
        if (model.getLastError() != null) {
            blank();
            row("  " + RED + "⚠  " + model.getLastError() + RST);
        }
        blank();
        footer();
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
        row(center(WHT + BOLD + "M E S O S   —   TUI MODE" + RST, W));
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

    private void footer() {
        boxLine('╚', '═', '╝');
        System.out.print(WHT + BOLD + "  > " + RST);
    }

    private void boxLine(char l, char m, char r) {
        System.out.println(CYN + BOLD + l + String.valueOf(m).repeat(W) + r + RST);
    }

    private void row(String content) {
        int pad = Math.max(0, W - vis(content));
        System.out.println(CYN + "║" + RST + content + " ".repeat(pad) + CYN + "║" + RST);
    }

    private void blank() {
        System.out.println(CYN + "║" + " ".repeat(W) + "║" + RST);
    }

    private void section(String title) {
        int dashes = W - title.length() - 4;
        int left = dashes / 2;
        int right = dashes - left;
        row(GRY + "─".repeat(left) + RST + "  " + WHT + BOLD + title + RST + "  " + GRY + "─".repeat(right) + RST);
    }

    // ── schermate ────────────────────────────────────────────────────────────

    private void loginScreen() {
        blank();
        row(center(WHT + BOLD + "Benvenuto in MESOS!" + RST, W));
        blank();
        row("  " + GRY + "Inserisci nickname e colore totem per entrare nel gioco." + RST);
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

        blank();
        row("  " + WHT + BOLD + "LOBBY" + RST + "  " + GRY
                + currentPlayers + " / " + expectedPlayers
                + " giocatori" + RST);
        blank();
        for (String p : model.getLobbyPlayers()) {
            row("    " + GRN + "●" + RST + "  " + WHT + p + RST);
        }
        blank();
        if (expectedPlayers == 0) {
            row("  " + GRY + "Sei il primo — digita:  " + RST + YLW + "players <2-5>" + RST);
        } else if (currentPlayers < expectedPlayers) {
            int missingPlayers = expectedPlayers - currentPlayers;
            row("  " + GRY + "In attesa di " + missingPlayers
                    + (missingPlayers == 1 ? " giocatore..." : " giocatori...") + RST);
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

    private void gameScreen(ClientModel model, boolean totemPhase) {
        blank();

        // ordine di turno
        List<String> order = model.getTurnOrder();
        if (!order.isEmpty()) {
            StringBuilder sb = new StringBuilder("  " + GRY + "Turno: " + RST);
            for (int i = 0; i < order.size(); i++) {
                if (i > 0) sb.append(GRY + " → " + RST);
                sb.append(YLW).append(order.get(i)).append(RST);
            }
            row(sb.toString());
            blank();
        }

        section("TABELLONE");
        blank();
        Map<Character, String> tiles = model.getTotemOnTiles();
        row(tilesRow(tiles, 'A', 'D'));
        row(tilesRow(tiles, 'E', 'H'));

        if (!totemPhase) {
            blank();
            section("CARTE");
            blank();
            row("  " + GRY + "Personaggi  Upper: " + RST + WHT
                    + formatIds(model.getUpperRowCharacterIds()) + RST
                    + "    " + GRY + "Lower: " + RST + WHT
                    + formatIds(model.getLowerRowCharacterIds()) + RST);
            row("  " + GRY + "Edifici     Upper: " + RST + YLW
                    + formatIds(model.getUpperRowBuildingIds()) + RST
                    + "    " + GRY + "Lower: " + RST + YLW
                    + formatIds(model.getLowerRowBuildingIds()) + RST);
        }

        blank();
        section("GIOCATORI");
        blank();
        row("  " + DIM + String.format("  %-14s  %5s  %-18s  %5s",
                "Nickname", "Cibo", "", "PP") + RST);
        for (PlayerView p : model.getPlayers().values()) {
            row("  " + WHT + String.format("  %-14s", p.getNickname()) + RST
                    + "  " + YLW + String.format("%5d", p.getFood()) + RST
                    + "  " + foodBar(p.getFood(), 20)
                    + "  " + YLW + String.format("%5d", p.getPrestigePoints()) + RST + " PP");
        }

        blank();
        if (totemPhase) {
            row(GRY + "  place <lettera>   es. place A" + RST);
        } else {
            row(GRY + "  pick <id> upper|lower   —   build <id> upper|lower" + RST);
        }
        blank();
    }

    private void simpleScreen(String msg, ClientModel model) {
        blank();
        row("  " + YLW + msg + RST);
        blank();
        for (PlayerView p : model.getPlayers().values()) {
            row("  " + WHT + String.format("%-16s", p.getNickname()) + RST
                    + "  " + YLW + p.getFood() + RST + " cibo"
                    + "  " + GRN + p.getPrestigePoints() + RST + " PP");
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
        section("CLASSIFICA FINALE");
        blank();
        int rank = 1;
        for (Map.Entry<String, Integer> e : model.getFinalScores().entrySet()
                .stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).toList()) {
            String medal = switch (rank) {
                case 1  -> YLW + "①" + RST + " ";
                case 2  -> GRY + "②" + RST + " ";
                default -> DIM + "③" + RST + " ";
            };
            row("  " + medal + WHT + String.format("%-16s", e.getKey()) + RST
                    + "  " + YLW + e.getValue() + " PP" + RST);
            rank++;
        }
        blank();
    }

    // ── utility ──────────────────────────────────────────────────────────────

    private String tilesRow(Map<Character, String> tiles, char from, char to) {
        StringBuilder sb = new StringBuilder("  ");
        for (char c = from; c <= to; c++) {
            String occ = tiles.get(c);
            sb.append(CYN + BOLD + "[" + c + "]" + RST + " ");
            if (occ != null) {
                sb.append(GRN + String.format("%-12s", occ) + RST);
            } else {
                sb.append(GRY + String.format("%-12s", "─────") + RST);
            }
        }
        return sb.toString();
    }

    private String foodBar(int food, int max) {
        int total = 16;
        int filled = (int) Math.round((double) Math.min(food, max) / max * total);
        return GRN + "█".repeat(filled) + GRY + "░".repeat(total - filled) + RST;
    }

    private String formatIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return "[ ]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i));
            if (i < ids.size() - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }

    private String stateLabel(String state) {
        return switch (state) {
            case "LobbyState"            -> "LOBBY";
            case "ReadyCheckState"       -> "READY CHECK";
            case "TotemPlacementState"   -> "POSIZIONAMENTO TOTEM";
            case "ActionResolutionState" -> "RISOLUZIONE AZIONI";
            case "EventResolutionState"  -> "RISOLUZIONE EVENTI";
            case "EndRoundState"         -> "FINE ROUND";
            case "SuspendedState"        -> "⚠ PAUSA";
            case "EndGameState"          -> "FINE PARTITA";
            default                      -> state;
        };
    }

    /** Lunghezza visibile: ignora i codici ANSI escape. */
    private int vis(String s) {
        return s.replaceAll("\033\\[[0-9;]*m", "").length();
    }

    /** Centra `coloredText` in una zona larga `targetVisW` caratteri visibili. */
    private String center(String coloredText, int targetVisW) {
        int textVis = vis(coloredText);
        int pad = Math.max(0, targetVisW - textVis);
        return " ".repeat(pad / 2) + coloredText + " ".repeat(pad - pad / 2);
    }
}
