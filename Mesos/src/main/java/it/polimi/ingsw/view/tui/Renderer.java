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

    private static final int W = 140;
    private static final int CARD_W   = 16;
    private static final int CARD_H   = 10;
    private static final int CARD_GAP = 2;
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
        System.out.flush();
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

            case "EventResolutionState"  -> eventResolutionScreen(model);
            case "EndRoundState"         -> endOfRoundScreen(model);
            case "SuspendedState"        -> suspendedScreen(model);
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
            row(center(GRN + BOLD + "PLAYER: " + model.getMyNickname().toUpperCase() + RST, W));
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
        System.out.println();
    }

    private void row(String content) {
        System.out.println(content);
    }

    private void row(String content, String borderColor) {
        System.out.println(content);
    }

    private void blank() {
        System.out.println();
    }

    private void loginScreen(ClientModel model) {
        printMesosLogoPlain();

        System.out.println();
        System.out.println(centerPlain(WHT + BOLD + "Welcome to MESOS!" + RST, W));
        System.out.println();
        System.out.println(GRY + "Enter your nickname and totem color to join the game." + RST);
        System.out.println(GRY + "If you were already in a match, you can choose to reconnect." + RST);
        System.out.println();
        System.out.println(GRN + BOLD + "Available colors:" + RST + "  " + availableColorLabels(model));
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
                + " players" + RST);
        blank();

        for (String p : model.getLobbyPlayers()) {
            boolean ready = model.getReadyPlayers().contains(p);
            String readyBadge = ready ? GRN + "✓ ready" + RST : GRY + "⌛ waiting" + RST;

            boolean isLobbyLeader = !model.getLobbyPlayers().isEmpty()
                    && p.equals(model.getLobbyPlayers().get(0));

            String leaderBadge = isLobbyLeader ? YLW + "👑 " + RST : "";

            row("    " + GRN + "●" + RST + "  "
                    + leaderBadge + WHT + p + RST
                    + "  " + readyBadge);
        }

        blank();

        if (!playersCountChosen) {
            if (iAmFirst) {
                row("  " + YLW + BOLD + "You are the first player!" + RST);
                row("  " + GRY + "Type the number of players." + RST);
            }
            else {
                row("  " + GRY + "Waiting for the first player to choose the number of participants..." + RST);
            }
        } else if (cur < exp) {
            int miss = exp - cur;
            row("  " + GRY + "Waiting for " + miss
                    + (miss == 1 ? " player..." : " players...") + RST);

            if (iAmFirst) {
                if (model.isPromotedToLobbyLeader()) {
                    row("  " + YLW + "You are the new lobby leader: you can change the number of players." + RST);
                } else {
                    row("  " + GRY + "You are the lobby leader: you can change the number of players." + RST);
                }
            }
        } else {
            row("  " + GRY + "Lobby complete — waiting for the ready check..." + RST);
        }

        blank();
    }



    private void renderReadyCheck(ClientModel model) {
        blank();
        row("  " + WHT + BOLD + "ALL CONNECTED — Confirm your presence" + RST);
        blank();

        for (String p : model.getLobbyPlayers()) {
            boolean ready = model.getReadyPlayers().contains(p);
            boolean isMe  = p.equals(model.getMyNickname());
            boolean isLobbyLeader = !model.getLobbyPlayers().isEmpty()
                    && p.equals(model.getLobbyPlayers().get(0));
            String status = ready ? GRN + "v  Ready          " : YLW + "?  Waiting...    ";
            String leaderBadge = isLobbyLeader ? YLW + "👑 " + RST : "";
            String name   = isMe ? CYN + BOLD + p + "  (you)" + RST : WHT + p + RST;
            row("    " + status + RST + leaderBadge + name);
        }

        blank();
        boolean iAmReady = model.getReadyPlayers().contains(model.getMyNickname());
        if (iAmReady) {
            row("  " + GRN + BOLD + "v CONFIRMED" + RST + GRY + "  —  waiting for the other players..." + RST);
        } else {
            row("  " + YLW + BOLD + ">>>  YOUR TURN!  <<<" + RST);
            row("  " + WHT + "Type  " + RST + YLW + BOLD + "ready" + RST + WHT + "  to confirm your presence." + RST);
        }
        blank();
    }

    private void suspendedScreen(ClientModel model) {
        blank();
        row(center(RED + BOLD + "MATCH PAUSED" + RST, W));
        blank();
        row(center(GRY + "Waiting for at least one other player to reconnect..." + RST, W));
        blank();

        long remainingMillis = model.getSuspensionRemainingMillis();
        if (remainingMillis >= 0L) {
            row(center(YLW + BOLD + "Automatic win timeout: "
                    + formatDuration(remainingMillis) + RST, W));
        } else {
            row(center(YLW + BOLD + "Automatic win timeout: waiting for the server" + RST, W));
        }
        blank();
    }

    private void endGameScreen(ClientModel model) {
        final int panelW = 88;
        String winner = model.getWinnerNickname() == null ? "UNKNOWN" : model.getWinnerNickname();
        List<Map.Entry<String, Integer>> standings = model.getFinalScores().entrySet()
                .stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).toList();

        blank();
        row(center(GRY + DIM + "▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓" + RST, W));
        row(center(RED + BOLD + " ██████╗  █████╗ ███╗   ███╗███████╗     ██████╗ ██╗   ██╗███████╗██████╗ " + RST, W));
        row(center(RED + BOLD + "██╔════╝ ██╔══██╗████╗ ████║██╔════╝    ██╔═══██╗██║   ██║██╔════╝██╔══██╗" + RST, W));
        row(center(RED + BOLD + "██║  ███╗███████║██╔████╔██║█████╗      ██║   ██║██║   ██║█████╗  ██████╔╝" + RST, W));
        row(center(RED + BOLD + "██║   ██║██╔══██║██║╚██╔╝██║██╔══╝      ██║   ██║╚██╗ ██╔╝██╔══╝  ██╔══██╗" + RST, W));
        row(center(RED + BOLD + "╚██████╔╝██║  ██║██║ ╚═╝ ██║███████╗    ╚██████╔╝ ╚████╔╝ ███████╗██║  ██║" + RST, W));
        row(center(RED + BOLD + " ╚═════╝ ╚═╝  ╚═╝╚═╝     ╚═╝╚══════╝     ╚═════╝   ╚═══╝  ╚══════╝╚═╝  ╚═╝" + RST, W));
        row(center(GRY + DIM + "▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓" + RST, W));

        blank();
        row(center(CYN + BOLD + "╔" + "═".repeat(panelW) + "╗" + RST, W));
        row(center(CYN + BOLD + "║" + RST + center(YLW + BOLD + "◆  WINNER DETECTED  ◆" + RST, panelW) + CYN + BOLD + "║" + RST, W));
        row(center(CYN + BOLD + "║" + RST + center(GRN + BOLD + winner.toUpperCase() + RST, panelW) + CYN + BOLD + "║" + RST, W));
        row(center(CYN + BOLD + "║" + RST + center(GRY + "FINAL STANDINGS" + RST, panelW) + CYN + BOLD + "║" + RST, W));
        row(center(CYN + BOLD + "╠" + "═".repeat(panelW) + "╣" + RST, W));

        row(center(CYN + BOLD + "║" + RST
                + "  " + GRY + "RK" + RST
                + "  " + GRY + padRight("PLAYER", 18) + RST
                + "  " + GRY + padRight("PRESTIGE", 12) + RST
                + "  " + GRY + padRight("FOOD", 8) + RST
                + "  " + GRY + padRight("STATUS", 17) + RST
                + "  " + CYN + BOLD + "║" + RST, W));
        row(center(CYN + BOLD + "╟" + "─".repeat(panelW) + "╢" + RST, W));

        int rank = 1;
        for (Map.Entry<String, Integer> e : standings) {
            row(center(endGameScoreLine(model, e.getKey(), e.getValue(), rank, panelW), W));
            rank++;
        }

        row(center(CYN + BOLD + "╚" + "═".repeat(panelW) + "╝" + RST, W));
        blank();
        row(center(GRY + DIM + "> PRESS ENTER TO CONTINUE" + RST, W));
        blank();
    }

    private String endGameScoreLine(ClientModel model, String nickname, int score, int rank, int panelW) {
        PlayerView player = model.getPlayers().get(nickname);
        String playerColor = player != null ? totemColor(player.getTotemColor()) : WHT;
        int food = player != null ? player.getFood() : 0;
        boolean me = nickname.equals(model.getMyNickname());
        boolean winner = nickname.equals(model.getWinnerNickname());
        String rankColor = rank == 1 ? YLW + BOLD : rank == 2 ? WHT : GRY;
        String scoreColor = score < 0 ? RED : YLW;
        String status = winner ? "★ WINNER" : "";
        if (me) {
            status = status.isBlank() ? "<< YOU" : status + "  << YOU";
        }

        String content = "  "
                + rankColor + String.format("%02d", rank) + RST
                + "  " + playerColor + BOLD + padRight(ellipsize(nickname.toUpperCase(), 18), 18) + RST
                + "  " + scoreColor + BOLD + padRight(score + " PP", 12) + RST
                + "  " + GRN + padRight(food + " food", 8) + RST
                + "  " + CYN + BOLD + padRight(status, 17) + RST
                + "  ";
        return CYN + BOLD + "║" + RST + padRight(content, panelW) + CYN + BOLD + "║" + RST;
    }

    private void simpleScreen(String msg, ClientModel model) {
        blank();
        row("  " + YLW + msg + RST);
        blank();
        for (PlayerView p : model.getPlayers().values()) {
            String col = totemColor(p.getTotemColor());
            row("  " + col + "●" + RST + " " + WHT + String.format("%-16s", p.getNickname()) + RST
                    + "  " + YLW + p.getFood() + " food" + RST
                    + "  " + foodBar(p.getFood(), 20)
                    + "  " + GRN + p.getPrestigePoints() + " PP" + RST);
        }
        blank();
    }

    private void eventResolutionScreen(ClientModel model) {
        blank();
        String title = model.getLastResolvedEventTitle();
        String desc  = model.getLastResolvedEventDescription();

        if (title == null) {
            row("  " + YLW + "Resolving events..." + RST);
            blank();
        } else {
            printBox("EVENT RESOLVED — " + title, desc);
            blank();
            // Per-player breakdown for this event
            for (var eff : model.getLastResolvedEventEffects()) {
                String foodTxt = formatDelta(eff.getFoodDelta(), eff.getNewFood(), " food");
                String ppTxt   = formatDelta(eff.getPointsDelta(), eff.getNewPoints(), " PP");
                PlayerView p = model.getPlayers().get(eff.getNickname());
                String col = p != null ? totemColor(p.getTotemColor()) : WHT;
                row("  " + col + "● " + WHT + String.format("%-14s", eff.getNickname()) + RST
                        + "  food " + foodTxt + "    points " + ppTxt);
            }
        }

        blank();
        for (PlayerView p : model.getPlayers().values()) {
            String col = totemColor(p.getTotemColor());
            row("  " + col + "●" + RST + " " + WHT + String.format("%-16s", p.getNickname()) + RST
                    + "  " + YLW + p.getFood() + " food" + RST
                    + "  " + foodBar(p.getFood(), 20)
                    + "  " + GRN + p.getPrestigePoints() + " PP" + RST);
        }
        blank();
    }

    private String formatDelta(int delta, int total, String unit) {
        String d;
        if (delta > 0)      d = GRN + "+" + delta + RST;
        else if (delta < 0) d = RED + String.valueOf(delta) + RST;
        else                d = GRY + "0" + RST;
        return d + " (" + total + unit + ")";
    }

    private void endOfRoundScreen(ClientModel model) {
        blank();
        int finishedRound = model.getLastFinishedRound();
        if (finishedRound < 1) {
            // Fallback: should rarely happen, only if EndRound state arrives
            // before any RoundEnded message, for example after restoring from disk.
            finishedRound = Math.max(1, model.getRoundNumber() - 1);
        }
        printBox("ROUND " + finishedRound + " ENDED", "Preparing the next round...");
        blank();
        for (PlayerView p : model.getPlayers().values()) {
            String col = totemColor(p.getTotemColor());
            row("  " + col + "●" + RST + " " + WHT + String.format("%-16s", p.getNickname()) + RST
                    + "  " + YLW + p.getFood() + " food" + RST
                    + "  " + foodBar(p.getFood(), 20)
                    + "  " + GRN + p.getPrestigePoints() + " PP" + RST);
        }
        blank();
    }

    private void printBox(String title, String body) {
        int width = Math.max(title.length(), body.length()) + 6;
        String border = "+" + "-".repeat(width - 2) + "+";
        String pad = "|" + " ".repeat(width - 2) + "|";

        row("  " + CYN + border + RST);
        row("  " + CYN + "|" + RST + center(YLW + BOLD + title + RST, width - 2) + CYN + "|" + RST);
        row("  " + CYN + pad + RST);
        row("  " + CYN + "|" + RST + center(WHT + body + RST, width - 2) + CYN + "|" + RST);
        row("  " + CYN + border + RST);
    }

    private void gameScreen(ClientModel model, boolean totemPhase) {
        blank();

        String current = currentTurnPlayer(model, totemPhase);

        if (current != null) {
            PlayerView pv = current != null ? model.getPlayers().get(current) : null;
            String col = pv != null ? totemColor(pv.getTotemColor()) : WHT;

            row(center(WHT + BOLD + "NOW IT IS " + RST
                    + col + BOLD + (current != null ? current.toUpperCase() : "?") + RST
                    + WHT + BOLD + "'S TURN" + RST, W));

            String turnEvent = model.getLastTurnEvent();
            if (turnEvent != null) {
                row(center(CYN + BOLD + ">> " + turnEvent + " <<" + RST, W));
            }
        }

        if (totemPhase) {
            String cur = model.getCurrentTotemPlacementPlayer();
            if (cur != null && cur.equals(model.getMyNickname())) {
                row(center(YLW + BOLD + BLINK + ">>>  YOUR TURN — PLACE YOUR TOTEM!  <<<" + RST, W));

            } else if (cur != null) {
                row("  " + GRY + "Waiting for " + YLW + cur + RST + GRY + " to place the totem..." + RST);
            }
        } else if (model.isMyTurn()) {
            row(center(GRN + BOLD + BLINK + ">>>  YOUR TURN!  <<<" + RST, W));
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
                : model.getCurrentActionResolutionPlayer();

        row("  " + CYN + BOLD + "PLAYERS" + RST);

        List<PlayerView> sortedPlayers = model.getPlayers().values().stream()
                .sorted((a, b) -> Integer.compare(b.getPrestigePoints(), a.getPrestigePoints()))
                .toList();

        for (int i = 0; i < sortedPlayers.size(); i++) {
            PlayerView p = sortedPlayers.get(i);
            renderPlayer(
                    p,
                    i + 1,
                    p.getNickname().equals(currentActor),
                    p.getNickname().equals(model.getMyNickname()),
                    model.isPlayerDisconnected(p.getNickname())
            );
        }
        blank();
    }

    private void renderCardRow(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            row("  " + GRY + "(no cards)" + RST);
            return;
        }

        int maxPer = Math.max(1, (W - 2) / (CARD_W + CARD_GAP));

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

        if (c.kind == CardCatalog.Kind.CHARACTER) {
            return drawCharacterCard(c);
        }

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
                ? c.foodCost + " food  +" + c.pointsEnd + "PP"
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

    private String[] drawCharacterCard(CardCatalog.CardInfo c) {
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


    private String characterName(String typeName) {
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

    private String characterIcon(CardCatalog.CardInfo c) {
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

    private String characterInfoLine(CardCatalog.CardInfo c, boolean bottomLine) {
        return switch (c.typeName) {
            case "Hunter" -> bottomLine && "+".equals(c.detail) ? "+1🍖" : "";

            case "Builder" -> {
                int pp = parseBuilderPoints(c.detail);
                int discount = parseBuilderDiscount(c.detail);
                yield bottomLine ? "+" + pp + "🌟" : "−" + discount + "🍖";
            }

            case "Shaman" -> bottomLine ? c.detail.replace("★", "🔯") : "";

            default -> "";
        };
    }

    private int parseBuilderPoints(String detail) {
        if (detail == null || detail.isBlank()) return 0;

        String[] parts = detail.split(" ");
        for (String part : parts) {
            if (part.startsWith("+")) {
                return Integer.parseInt(part.substring(1));
            }
        }

        return 0;
    }

    private int parseBuilderDiscount(String detail) {
        if (detail == null || detail.isBlank()) return 0;

        String[] parts = detail.split(" ");
        for (String part : parts) {
            if (part.startsWith("-") && part.endsWith("c")) {
                return Integer.parseInt(part.substring(1, part.length() - 1));
            }
        }

        return 0;
    }



    private String characterLine(String text) {
        return WHT + "|" + RST + centerCardText(text) + WHT + "|" + RST;
    }

    private String characterMiniBoxTop() {
        return characterLine("+" + "-".repeat(8) + "+");
    }

    private String characterMiniBoxBottom() {
        return characterLine("+" + "-".repeat(8) + "+");
    }

    private String characterMiniBoxText(String text) {
        return characterLine("|" + centerVisual(text, 8) + "|");
    }

    private String centerCardText(String text) {
        if (text == null || text.isBlank()) {
            return " ".repeat(CARD_W);
        }

        String clean = stripAnsi(text);

        if (isSingleCardEmoji(clean)) {
            return centerSingleEmoji(text);
        }

        return centerVisual(text, CARD_W);
    }

    private boolean isSingleCardEmoji(String text) {
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

    private String centerSingleEmoji(String emoji) {
        return " ".repeat(7) + emoji + " ".repeat(7);
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
        boolean actionResolution = "ActionResolutionState".equals(model.getGameState());
        List<String> order = actionResolution ? actionResolutionOrder(model) : model.getTurnOrder();
        List<String> placedPlayers = new ArrayList<>(model.getTotemOnTiles().values());

        int numPlayers = Math.max(2, Math.max(model.getPlayers().size(), order.size()));
        int visibleSlots = Math.max(numPlayers, order.size());

        String[] box = new String[2 + visibleSlots * 3];
        int line = 0;

        box[line++] = titledTop("TURN ORDER", ORDER_W, CYN);

        String currentPlayer = actionResolution
                ? model.getCurrentActionResolutionPlayer()
                : model.getCurrentTotemPlacementPlayer();

        for (int i = 0; i < visibleSlots; i++) {
            String name = i < order.size() ? order.get(i) : "";

            if (!actionResolution && (placedPlayers.contains(name) || model.isPlayerDisconnected(name))) {
                name = "";
            }

            PlayerView pv = name.isBlank() ? null : model.getPlayers().get(name);
            String col = pv != null ? totemColor(pv.getTotemColor()) : GRY;
            boolean current = name.equals(currentPlayer) && !model.isPlayerDisconnected(name);

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

        String shownOccupant = occupant == null ? "empty" : "● " + ellipsize(occupant, OFFER_W - 4);
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

    private void renderPlayer(PlayerView p, int rank, boolean isActor, boolean isMe, boolean disconnected) {
        String col = totemColor(p.getTotemColor());
        String marker = isActor ? YLW + BOLD + BLINK + "▶" + RST : disconnected ? RED + "x" + RST : " ";
        String meTag = isMe ? CYN + BOLD + " you" + RST : "";

        String rankStr = rankLabel(rank);
        String name = ellipsize(p.getNickname(), 14);
        String nameColor = disconnected ? GRY : isActor ? YLW : col;
        String nameStr = nameColor + BOLD + String.format("%-14s", name) + RST;

        String foodStr = YLW + BOLD + "🍖 " + String.format("%2d", p.getFood()) + RST;
        String barStr = foodBar(p.getFood(), 20);

        String ppColor = p.getPrestigePoints() < 0 ? RED : GRN;
        String ppStr = ppColor + BOLD + "🌟 " + String.format("%3d", p.getPrestigePoints()) + RST;

        row("  " + rankStr + "  " + marker + " "
                + nameStr
                + "   " + foodStr
                + "   " + barStr
                + "   " + ppStr
                + meTag
                + (isActor ? YLW + BOLD + "   <-- TURN" + RST : "")
                + (disconnected ? RED + BOLD + "   disconnected" + RST : ""));

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

            row("      " + GRY + "Cards: " + RST + WHT + list + RST);
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
            case "TotemPlacementState"   -> "TOTEM PLACEMENT";
            case "ActionResolutionState" -> "ACTION RESOLUTION";
            case "EventResolutionState"  -> "EVENT RESOLUTION";
            case "EndRoundState"         -> "END OF ROUND";
            case "SuspendedState"        -> "PAUSED";
            case "EndGameState"          -> "GAME OVER";
            default                      -> state;
        };
    }

    private String footerPrompt(ClientModel model) {
        boolean iAmFirst = model.getMyNickname() != null
                && !model.getLobbyPlayers().isEmpty()
                && model.getMyNickname().equals(model.getLobbyPlayers().get(0));

        if ("LobbyState".equals(model.getGameState()) && model.getExpectedPlayers() < 2 && iAmFirst) {
            return "  Number of players (2-5) > ";
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






    private String rankLabel(int rank) {
        String label = rank + ".";
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
        String title = " COMMANDS ";
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
                    new String[]{"nickname", "enter your name when requested"},
                    new String[]{"color", "choose RED, BLUE, PURPLE, YELLOW or WHITE"}
            );
        }

        return switch (state) {
            case "LobbyState" -> {
                int exp = model.getExpectedPlayers();
                boolean playersCountChosen = exp >= 2;

                if (!playersCountChosen) {
                    boolean iAmFirst = model.getMyNickname() != null
                            && !model.getLobbyPlayers().isEmpty()
                            && model.getMyNickname().equals(model.getLobbyPlayers().get(0));

                    if (iAmFirst) {
                        yield List.of(
                                new String[]{"2 / 3 / 4 / 5", "choose the number of players"},
                                new String[]{"players <2-5>", "explicitly set the number of players"},
                                new String[]{"ready", "mark yourself as ready"},
                                new String[]{"quit", "leave the lobby"}
                        );
                    }

                    yield List.of(
                            new String[]{"ready", "mark yourself as ready"},
                            new String[]{"quit", "leave the lobby"}
                    );
                }

                boolean iAmFirst = model.getMyNickname() != null
                        && !model.getLobbyPlayers().isEmpty()
                        && model.getMyNickname().equals(model.getLobbyPlayers().get(0));

                if (iAmFirst) {
                    yield List.of(
                            new String[]{"ready", "confirm your presence"},
                            new String[]{"unready", "cancel your confirmation"},
                            new String[]{"players <2-5>", "change the number of players"},
                            new String[]{"quit", "leave the lobby"}
                    );
                }

                yield List.of(
                        new String[]{"ready", "confirm your presence"},
                        new String[]{"unready", "cancel your confirmation"},
                        new String[]{"quit", "leave the lobby"}
                );
            }

            case "ReadyCheckState" -> {
                boolean iAmFirst = model.getMyNickname() != null
                        && !model.getLobbyPlayers().isEmpty()
                        && model.getMyNickname().equals(model.getLobbyPlayers().get(0));

                if (iAmFirst) {
                    yield List.of(
                            new String[]{"ready", "confirm your presence"},
                            new String[]{"unready", "cancel your confirmation"},
                            new String[]{"players <2-5>", "change the number of players"},
                            new String[]{"quit", "leave the match"}
                    );
                }

                yield List.of(
                        new String[]{"ready", "confirm your presence"},
                        new String[]{"unready", "cancel your confirmation"},
                        new String[]{"quit", "leave the match"}
                );
            }

            case "StartGameState", "TotemPlacementState" -> List.of(
                    new String[]{"place <tile>", "place your totem on a tile, e.g. place B"},
                    new String[]{"help", "show quick help"},
                    new String[]{"quit", "leave the match"}
            );

            case "ActionResolutionState" -> List.of(
                    new String[]{"pick <id> upper", "take a character/event card from the upper row"},
                    new String[]{"pick <id> lower", "take a character/event card from the lower row"},
                    new String[]{"build <id> upper", "take a building card from the upper row"},
                    new String[]{"build <id> lower", "take a building card from the lower row"},
                    new String[]{"help", "show quick help"},
                    new String[]{"quit", "leave the match"}
            );

            case "EventResolutionState" -> List.of(
                    new String[]{"wait", "the server is resolving events"},
                    new String[]{"quit", "leave the match"}
            );

            case "EndRoundState" -> List.of(
                    new String[]{"wait", "preparing the next round"},
                    new String[]{"quit", "leave the match"}
            );

            case "SuspendedState" -> List.of(
                    new String[]{"wait", "timer active: the only connected player wins when it expires"},
                    new String[]{"quit", "leave the match"}
            );

            case "EndGameState" -> List.of(
                    new String[]{"enter", "close the TUI"},
                    new String[]{"quit", "exit"}
            );

            default -> List.of(
                    new String[]{"help", "show quick help"},
                    new String[]{"quit", "leave the match"}
            );
        };
    }

    private String currentTurnPlayer(ClientModel model, boolean totemPhase) {
        return totemPhase
                ? model.getCurrentTotemPlacementPlayer()
                : model.getCurrentActionResolutionPlayer();
    }

    private List<String> actionResolutionOrder(ClientModel model) {
        List<String> ordered = new ArrayList<>();
        Map<Character, String> totemOnTiles = model.getTotemOnTiles();

        for (Character tile : TILE_ORDER) {
            String nickname = totemOnTiles.get(tile);
            if (nickname != null && !ordered.contains(nickname)) {
                ordered.add(nickname);
            }
        }

        if (ordered.isEmpty()) {
            ordered.addAll(model.getTurnOrder());
        }
        return ordered;
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, (millis + 999L) / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
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

    private String makeAsciiCardTopBorder(String idStr, int cardW, String color) {
        String label = " " + idStr + " ";
        int dashTotal = Math.max(0, cardW - visualWidth(label));
        int left = dashTotal / 2;
        int right = dashTotal - left;

        return color + "+" + "-".repeat(left) + label + "-".repeat(right) + "+" + RST;
    }

    private String centerVisual(String text, int targetW) {
        if (text == null) text = "";

        int textW = visualWidth(text);
        if (textW > targetW) {
            return centerStr(stripAnsi(text), targetW);
        }

        int pad = targetW - textW;
        return " ".repeat(pad / 2) + text + " ".repeat(pad - pad / 2);
    }

    private int visualWidth(String text) {
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

    private boolean isKeycapEmoji(String text, int index) {
        int cp = text.codePointAt(index);
        if (!Character.isDigit(cp)) return false;

        int next = index + Character.charCount(cp);
        if (next < text.length() && text.codePointAt(next) == 0xFE0F) {
            next += Character.charCount(text.codePointAt(next));
        }

        return next < text.length() && text.codePointAt(next) == 0x20E3;
    }

    private boolean isWideEmoji(int cp) {
        return (cp >= 0x1F300 && cp <= 0x1FAFF)
                || (cp >= 0x2600 && cp <= 0x27BF);
    }

    private String stripAnsi(String text) {
        return text.replaceAll("\\u001B\\[[;\\d]*m", "");
    }

    private String builderMiniBoxTop() {
        return characterLine("+" + "-".repeat(10) + "+");
    }

    private String builderMiniBoxBottom() {
        return characterLine("+" + "-".repeat(10) + "+");
    }

    private String builderMiniBoxText(String text) {
        return characterLine("|" + centerVisual(text, 10) + "|");
    }

    private String inventorNumber(String detail) {
        if (detail == null || detail.isBlank()) return "";
        return detail.replace("[", "").replace("]", "");
    }
}
