package it.polimi.ingsw.view.tui;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.GamePhase;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.network.protocol.clientbound.EventResolvedMessage;
import it.polimi.ingsw.view.EndGameHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.polimi.ingsw.view.tui.TuiStyle.*;
import static it.polimi.ingsw.view.tui.TuiText.*;


public class Renderer {

    private static final int LAST_ROUND = 10;

    private static final CardCatalog.CardVisitor<String> KIND_COLOR = new CardCatalog.CardVisitor<>() {
        @Override public String visitCharacter(CardCatalog.CardInfo c) { return WHT; }
        @Override public String visitBuilding(CardCatalog.CardInfo c) { return YLW; }
        @Override public String visitEvent(CardCatalog.CardInfo c) { return MGN; }
        @Override public String visitCard(CardCatalog.CardInfo c) { return GRY; }
    };

    private volatile int visibleTribeNumber;
    private volatile int visibleTribeCountdownSeconds;
    private volatile int visibleBuildingEffectId;
    private List<String> activeSideBox = List.of();
    private int activeSideBoxIndex;
    private int activeSideBoxColumnStart = -1;

    public Renderer(ClientModel model) { }

    public void setVisibleTribeNumber(int tribeNumber, int countdownSeconds) {
        this.visibleTribeNumber = Math.max(0, tribeNumber);
        this.visibleTribeCountdownSeconds = Math.max(0, countdownSeconds);
    }

    public boolean isTribeVisible() {
        return visibleTribeNumber > 0;
    }

    public void setVisibleBuildingEffectId(int buildingId) {
        this.visibleBuildingEffectId = Math.max(0, buildingId);
    }

    public boolean isBuildingEffectVisible() {
        return visibleBuildingEffectId > 0;
    }

    public void render(ClientModel model) {
        clear();
        String state = model.getGameState();

        if (state == null) {
            loginScreen(model);
            return;
        }
        GamePhase phase = GamePhase.fromStateName(state);

        if (phase.isEndGame()) {
            endGameScreen(model);
            System.out.flush();
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

        if (!phase.isEndRound() && !phase.isEventResolution()) {
            blank();
            footer(model);
        }
        System.out.flush();
    }


    private void body(String state, ClientModel model) {
        GamePhase phase = GamePhase.fromStateName(state);
        if (state == null) {
            loginScreen(model);
        } else switch (phase) {
            case GamePhase.Lobby ignored            -> lobbyScreen(model);
            case GamePhase.ReadyCheck ignored       -> renderReadyCheck(model);
            case GamePhase.StartGame ignored        -> gameScreen(model, true);
            case GamePhase.TotemPlacement ignored   -> gameScreen(model, true);
            case GamePhase.ActionResolution ignored -> gameScreen(model, false);
            case GamePhase.EventResolution ignored  -> eventResolutionScreen(model);
            case GamePhase.EndRound ignored         -> endOfRoundScreen(model);
            case GamePhase.Suspended ignored        -> suspendedScreen(model);
            case GamePhase.EndGame ignored          -> endGameScreen(model);
            default                                 -> gameScreen(model, false);
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

        if (isLastRound(model) && !GamePhase.fromStateName(state).isEndGame()) {
            row(center(YLW + BOLD + BLINK + "!!!  LAST ROUND  !!!" + RST, W));
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
        System.out.println(withActiveSideBox(content));
    }

    private void row(String content, String borderColor) {
        System.out.println(content);
    }

    private void blank() {
        if (hasActiveSideBoxLine()) {
            row("");
        } else {
            System.out.println();
        }
    }

    private void beginSideBox(List<String> sideBox) {
        if (sideBox == null || sideBox.isEmpty()) {
            activeSideBox = List.of();
            activeSideBoxIndex = 0;
            activeSideBoxColumnStart = -1;
            return;
        }

        int sideW = sideBox.stream().mapToInt(line -> visualWidth(stripAnsi(line))).max().orElse(0);
        activeSideBox = sideBox;
        activeSideBoxIndex = 0;
        activeSideBoxColumnStart = Math.max(0, TOP_TRIBE_ROW_W - sideW);
    }

    private void endSideBox() {
        activeSideBox = List.of();
        activeSideBoxIndex = 0;
        activeSideBoxColumnStart = -1;
    }

    private boolean hasActiveSideBoxLine() {
        return activeSideBoxIndex < activeSideBox.size();
    }

    private void drainActiveSideBox() {
        while (hasActiveSideBoxLine()) {
            row("");
        }
    }

    private String withActiveSideBox(String content) {
        if (!hasActiveSideBoxLine()) {
            return content;
        }

        String sideLine = activeSideBox.get(activeSideBoxIndex++);
        if (sideLine == null || sideLine.isEmpty()) {
            return content;
        }

        int contentW = visualWidth(stripAnsi(content));
        if (activeSideBoxColumnStart <= 0 || contentW >= activeSideBoxColumnStart) {
            return content + "  " + sideLine;
        }
        return padVisual(content, activeSideBoxColumnStart) + sideLine;
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
        final int panelW = 72;
        // null winner = shared victory: resolve co-winners via the shared tiebreaker utility.
        List<String> tiedList = EndGameHelper.resolveWinners(model);
        String winner = tiedList.isEmpty()
                ? "DRAW"
                : String.join(" & ", tiedList);
        List<Map.Entry<String, Integer>> standings = model.getFinalScores().entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    if (cmp != 0) return cmp;
                    return Integer.compare(foodOf(b.getKey(), model), foodOf(a.getKey(), model));
                })
                .toList();

        blank();
        renderVictoryConfetti(0);
        rowWithScreenConfetti(RED + BOLD + "◆  " + RST
                + RED + BOLD + "███╗   ███╗███████╗███████╗ ██████╗ ███████╗" + RST
                + RED + BOLD + "  ◆" + RST, 10);
        rowWithScreenConfetti(RED + BOLD + "   " + RST
                + RED + BOLD + "████╗ ████║██╔════╝██╔════╝██╔═══██╗██╔════╝" + RST
                + RED + BOLD + "   " + RST, 11);
        rowWithScreenConfetti(RED + BOLD + "◇  " + RST
                + RED + BOLD + "██╔████╔██║█████╗  ███████╗██║   ██║███████╗" + RST
                + RED + BOLD + "  ◇" + RST, 12);
        rowWithScreenConfetti(RED + BOLD + "   " + RST
                + RED + BOLD + "██║╚██╔╝██║██╔══╝  ╚════██║██║   ██║╚════██║" + RST
                + RED + BOLD + "   " + RST, 13);
        rowWithScreenConfetti(RED + BOLD + "   " + RST
                + RED + BOLD + "██║ ╚═╝ ██║███████╗███████║╚██████╔╝███████║" + RST
                + RED + BOLD + "   " + RST, 14);
        rowWithScreenConfetti(RED + BOLD + "   " + RST
                + RED + BOLD + "╚═╝     ╚═╝╚══════╝╚══════╝ ╚═════╝ ╚══════╝" + RST
                + RED + BOLD + "   " + RST, 15);
        rowWithScreenConfetti(RED + "────" + RST + YLW + BOLD + " END GAME " + RST + RED + "────" + RST, 16);

        renderVictoryConfetti(1);
        rowWithScreenConfetti(endBoxOuterBorder(panelW), 17);
        rowWithScreenConfetti(endBoxInnerBorder(panelW, true, 0), 18);
        renderWinnerBanner(panelW);
        renderWinnerNameBanner(panelW, winner, winnerColor(model, winner));
        rowWithScreenConfetti(endBoxSeparator(panelW, 6), 24);
        rowWithScreenConfetti(endGameHeaderLine(panelW), 25);
        rowWithScreenConfetti(endBoxSeparator(panelW, 8), 26);

        int rank = 1;
        for (Map.Entry<String, Integer> e : standings) {
            rowCenteredBox(endGameScoreLine(model, e.getKey(), e.getValue(), rank, panelW), "", 27 + rank);
            rank++;
        }

        rowWithScreenConfetti(endBoxInnerBorder(panelW, false, 10), 34);
        rowWithScreenConfetti(endBoxOuterBorder(panelW), 35);
        renderVictoryConfetti(2);
        renderVictoryConfetti(3);
        renderVictoryCup(winner, winnerColor(model, winner));
        blank();
    }

    private void renderVictoryCup(String winner, String winnerColor) {
        cupRow("      ◆════════════════◆      ", 0);
        cupRow("    ╔═╝                ╚═╗    ", 1);
        cupRow("    ║      VICTORY       ║    ", 2);
        cupRow("    ╚═╗                ╔═╝    ", 3);
        cupRow("      ◆══════╗  ╔══════◆      ", 4);
        cupRow("             ║  ║             ", 5);
        cupRow("             ║  ║             ", 6);
        cupRow("          ╔══╝  ╚══╗          ", 7);
        cupRow("          ╚════════╝          ", 8);
        blank();
        String[] winnerLines = asciiWordLines(winner, W - 16);
        for (int i = 0; i < winnerLines.length; i++) {
            rowWithScreenConfetti(winnerColor + BOLD + winnerLines[i] + RST, 52 + i);
        }
    }

    private void cupRow(String cupLine, int step) {
        rowWithScreenConfetti(YLW + BOLD + cupLine + RST, 40 + step);
    }

    private int[] cupConfettiPositions(int step) {
        return switch (step) {
            case 0 -> new int[]{4, 13, 27, 65, 82, 94};
            case 1 -> new int[]{1, 18, 39, 58, 77, 90};
            case 2 -> new int[]{9, 22, 32, 70, 86};
            case 3 -> new int[]{5, 15, 45, 61, 80, 95};
            case 4 -> new int[]{12, 25, 36, 73, 88};
            case 5 -> new int[]{3, 20, 48, 67, 92};
            case 6 -> new int[]{10, 29, 56, 79, 94};
            case 7 -> new int[]{6, 17, 38, 64, 83};
            default -> new int[]{14, 31, 50, 75, 91};
        };
    }

    private String[] cupConfettiTokens(int step) {
        return switch (step % 4) {
            case 0 -> new String[]{CYN + "▪" + RST, RED + "◆" + RST, YLW + "▪" + RST,
                    CYN + "▪" + RST, RED + "·" + RST, YLW + "▪" + RST};
            case 1 -> new String[]{RED + "·" + RST, CYN + "▪" + RST, MGN + "·" + RST,
                    YLW + "▪" + RST, RED + "▪" + RST, CYN + "·" + RST};
            case 2 -> new String[]{YLW + "▪" + RST, RED + "▪" + RST, CYN + "·" + RST,
                    RED + "◆" + RST, CYN + "▪" + RST, YLW + "·" + RST};
            default -> new String[]{CYN + "·" + RST, RED + "▪" + RST, YLW + "▪" + RST,
                    MGN + "·" + RST, CYN + "▪" + RST, RED + "·" + RST};
        };
    }

    private void renderWinnerBanner(int panelW) {
        String[] lines = {
                "█   █   █   █▄  █   █▄  █   █▀▀   █▀▄",
                "█ █ █   █   █ ▀▄█   █ ▀▄█   █▀    █▀▄",
                "▀▀ ▀▀   ▀   ▀   ▀   ▀   ▀   ▀▀▀   ▀ ▀"
        };
        for (int i = 0; i < lines.length; i++) {
            rowWithScreenConfetti(endBoxLine(center(CYN + BOLD + lines[i] + RST, panelW), panelW, i), 19 + i);
        }
    }

    private void renderWinnerNameBanner(int panelW, String winner, String color) {
        renderAsciiWordInEndBox(panelW, winner, color);
    }

    private void renderAsciiWordInEndBox(int panelW, String text, String color) {
        String[] lines = asciiWordLines(text, panelW);
        for (int i = 0; i < lines.length; i++) {
            rowWithScreenConfetti(endBoxLine(center(color + BOLD + lines[i] + RST, panelW), panelW, i + 3), 22 + i);
        }
    }

    private String winnerColor(ClientModel model, String winner) {
        PlayerView player = model.getPlayers().get(winner);
        return player != null ? totemColor(player.getTotemColor()) : YLW;
    }

    private String endGameHeaderLine(int panelW) {
        String sep = RED + BOLD + "│" + RST;
        String content = "   "
                + RED + BOLD + centerStr("PLAYER", END_PLAYER_W) + RST
                + "  " + sep + "  "
                + RED + BOLD + centerStr("PRESTIGE POINTS", END_PP_W) + RST
                + "  " + sep + "  "
                + RED + BOLD + centerStr("FOOD", END_FOOD_W) + RST
                + "   ";
        return endBoxLine(content, panelW, 6);
    }

    private String endGameScoreLine(ClientModel model, String nickname, int score, int rank, int panelW) {
        PlayerView player = model.getPlayers().get(nickname);
        String playerColor = player != null ? totemColor(player.getTotemColor()) : WHT;
        int food = foodOf(nickname, model);
        String rankColor = rank == 1 ? YLW + BOLD : rank == 2 ? RED + BOLD : GRY;
        String sep = RED + BOLD + "│" + RST;
        String rankText = rankColor + rank + ". " + RST;
        String nameText = playerColor + BOLD
                + ellipsize(nickname.toUpperCase(), END_PLAYER_W - 4) + RST;
        String playerCell = padRight(rankText + nameText, END_PLAYER_W);
        String ppCell = center(playerColor + BOLD + score + " 🌟" + RST, END_PP_W);
        String foodCell = center(playerColor + BOLD + food + " 🍖" + RST, END_FOOD_W);

        String content = "   "
                + playerCell
                + "  " + sep + "  "
                + ppCell
                + "  " + sep + "  "
                + foodCell
                + "   ";
        return endBoxLine(content, panelW, rank + 7);
    }

    private void rowCenteredBox(String boxLine, String suffix, int confettiStep) {
        int pad = Math.max(0, (W - vis(boxLine)) / 2);
        row(confettiLine(W, screenConfettiPositions(confettiStep), screenConfettiTokens(confettiStep), pad, boxLine) + suffix);
    }

    private void rowWithScreenConfetti(String content, int confettiStep) {
        int start = Math.max(0, (W - vis(content)) / 2);
        row(confettiLine(W, screenConfettiPositions(confettiStep), screenConfettiTokens(confettiStep), start, content));
    }

    private String endBoxOuterBorder(int panelW) {
        return RED + BOLD + "◆" + RST + decorativeBorder(panelW + 4) + RED + BOLD + "◆" + RST;
    }

    private String endBoxInnerBorder(int panelW, boolean top, int sideStep) {
        String line = CYN + BOLD
                + (top ? "┌" : "└")
                + "─".repeat(panelW)
                + (top ? "┐" : "┘")
                + RST;
        return decorativeWrap(line, sideStep);
    }

    private String endBoxSeparator(int panelW, int sideStep) {
        String line = CYN + BOLD + "├" + RST
                + RED + "─".repeat(panelW) + RST
                + CYN + BOLD + "┤" + RST;
        return decorativeWrap(line, sideStep);
    }

    private String endBoxLine(String content, int panelW, int sideStep) {
        String line = CYN + BOLD + "│" + RST
                + padRight(content, panelW)
                + CYN + BOLD + "│" + RST;
        return decorativeWrap(line, sideStep);
    }

    private String decorativeWrap(String innerLine, int sideStep) {
        return decorativeSide(sideStep) + " " + innerLine + " " + decorativeSide(sideStep + 3);
    }

    private String decorativeBorder(int width) {
        char[] pattern = {'─', '─', '▪', '·', '◆', '·', '▪', '─', '─'};
        StringBuilder border = new StringBuilder();
        for (int i = 0; i < width; i++) {
            border.append(decorativeToken(pattern[i % pattern.length]));
        }
        return border.toString();
    }

    private String decorativeSide(int step) {
        char[] pattern = {'◆', '▪', '·', '◆', '·', '▪'};
        return decorativeToken(pattern[Math.floorMod(step, pattern.length)]);
    }

    private String decorativeToken(char token) {
        return switch (token) {
            case '◆' -> RED + BOLD + "◆" + RST;
            case '▪' -> CYN + BOLD + "▪" + RST;
            case '·' -> YLW + BOLD + "·" + RST;
            default -> RED + "─" + RST;
        };
    }

    private int foodOf(String nickname, ClientModel model) {
        // Prefer server-authoritative finalFood over the cached PlayerView value.
        java.util.Map<String, Integer> finalFood = model.getFinalFood();
        if (finalFood.containsKey(nickname)) return finalFood.get(nickname);
        PlayerView pv = model.getPlayers().get(nickname);
        return pv != null ? pv.getFood() : 0;
    }

    private void renderVictoryConfetti(int band) {
        final int confettiW = 104;
        switch (band) {
            case 0 -> {
                row(center(confettiLine(confettiW,
                        new int[]{0, 9, 18, 34, 70, 87, 101},
                        new String[]{RED + BOLD + "C" + RST, RED + "▪" + RST, RED + "◆" + RST,
                                CYN + "▪" + RST, RED + "◆" + RST, CYN + "▪" + RST, RED + "▪" + RST}), W));
                row(center(confettiLine(confettiW,
                        new int[]{6, 23, 42, 63, 82, 96},
                        new String[]{CYN + "▪" + RST, YLW + "▪" + RST, RED + "·" + RST,
                                MGN + "·" + RST, YLW + "▪" + RST, RED + "▪" + RST}), W));
                row(center(confettiLine(confettiW,
                        new int[]{2, 14, 30, 50, 74, 91, 103},
                        new String[]{CYN + "▪" + RST, MGN + "·" + RST, YLW + "▪" + RST,
                                RED + "▪" + RST, CYN + "▪" + RST, YLW + "▪" + RST, RED + "·" + RST}), W));
            }
            case 1 -> {
                row(center(confettiLine(confettiW,
                        new int[]{5, 16, 31, 45, 59, 76, 93},
                        new String[]{RED + "▪" + RST, CYN + "·" + RST, YLW + "▪" + RST,
                                RED + "▪" + RST, MGN + "·" + RST, CYN + "▪" + RST, YLW + "▪" + RST}), W));
                row(center(confettiLine(confettiW,
                        new int[]{12, 27, 38, 66, 81, 100},
                        new String[]{YLW + "▪" + RST, RED + "·" + RST, CYN + "▪" + RST,
                                RED + "▪" + RST, CYN + "·" + RST, RED + "▪" + RST}), W));
            }
            case 2 -> {
                row(center(confettiLine(confettiW,
                        new int[]{3, 19, 33, 53, 72, 88, 102},
                        new String[]{CYN + "▪" + RST, RED + "▪" + RST, YLW + "·" + RST,
                                MGN + "·" + RST, YLW + "▪" + RST, RED + "▪" + RST, CYN + "▪" + RST}), W));
                row(center(confettiLine(confettiW,
                        new int[]{9, 25, 48, 61, 79, 97},
                        new String[]{RED + "·" + RST, CYN + "▪" + RST, RED + "▪" + RST,
                                YLW + "▪" + RST, CYN + "·" + RST, RED + "▪" + RST}), W));
            }
            default -> {
                row(center(confettiLine(confettiW,
                        new int[]{1, 21, 36, 50, 68, 84, 103},
                        new String[]{YLW + "▪" + RST, CYN + "·" + RST, RED + "▪" + RST,
                                MGN + "·" + RST, CYN + "▪" + RST, YLW + "▪" + RST, RED + "·" + RST}), W));
            }
        }
    }

    private int[] screenConfettiPositions(int step) {
        return switch (Math.floorMod(step, 8)) {
            case 0 -> new int[]{3, 12, 26, 38, 55, 73, 91, 108, 124, 137};
            case 1 -> new int[]{0, 18, 31, 49, 64, 82, 99, 116, 132};
            case 2 -> new int[]{7, 21, 42, 58, 76, 88, 104, 121, 139};
            case 3 -> new int[]{4, 15, 33, 47, 69, 84, 101, 119, 130};
            case 4 -> new int[]{10, 24, 36, 52, 67, 94, 111, 126, 136};
            case 5 -> new int[]{2, 19, 29, 45, 61, 80, 97, 114, 134};
            case 6 -> new int[]{6, 14, 39, 54, 72, 86, 106, 123, 138};
            default -> new int[]{1, 23, 35, 51, 66, 90, 103, 118, 131};
        };
    }

    private String[] screenConfettiTokens(int step) {
        return switch (Math.floorMod(step, 5)) {
            case 0 -> new String[]{CYN + "▪" + RST, RED + "◆" + RST, YLW + "▪" + RST,
                    RED + "·" + RST, CYN + "▪" + RST, YLW + "▪" + RST,
                    RED + "▪" + RST, MGN + "·" + RST, CYN + "▪" + RST, YLW + "·" + RST};
            case 1 -> new String[]{RED + "·" + RST, CYN + "▪" + RST, RED + "▪" + RST,
                    YLW + "▪" + RST, MGN + "·" + RST, CYN + "·" + RST,
                    RED + "◆" + RST, YLW + "▪" + RST, CYN + "▪" + RST, RED + "·" + RST};
            case 2 -> new String[]{YLW + "▪" + RST, CYN + "·" + RST, RED + "▪" + RST,
                    MGN + "·" + RST, CYN + "▪" + RST, RED + "·" + RST,
                    YLW + "▪" + RST, RED + "▪" + RST, CYN + "·" + RST, YLW + "▪" + RST};
            case 3 -> new String[]{CYN + "·" + RST, RED + "▪" + RST, YLW + "·" + RST,
                    CYN + "▪" + RST, RED + "◆" + RST, MGN + "·" + RST,
                    YLW + "▪" + RST, CYN + "▪" + RST, RED + "·" + RST, YLW + "▪" + RST};
            default -> new String[]{RED + "▪" + RST, YLW + "▪" + RST, CYN + "▪" + RST,
                    RED + "·" + RST, MGN + "·" + RST, YLW + "▪" + RST,
                    CYN + "·" + RST, RED + "▪" + RST, YLW + "·" + RST, CYN + "▪" + RST};
        };
    }

    private String confettiLine(int width, int[] positions, String[] tokens) {
        return confettiLine(width, positions, tokens, -1, null);
    }

    private String confettiLine(int width, int[] positions, String[] tokens, int insertAt, String insert) {
        String[] cells = new String[width];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = " ";
        }
        if (insert != null && insertAt >= 0 && insertAt < width) {
            cells[insertAt] = insert;
            int insertWidth = vis(insert);
            for (int i = 1; i < insertWidth && insertAt + i < width; i++) {
                cells[insertAt + i] = "";
            }
        }
        for (int i = 0; i < positions.length && i < tokens.length; i++) {
            int position = positions[i];
            if (position >= 0 && position < width) {
                if (insert != null && insertAt >= 0) {
                    int insertWidth = vis(insert);
                    if (position >= insertAt && position < insertAt + insertWidth) {
                        continue;
                    }
                }
                cells[position] = tokens[i];
            }
        }
        StringBuilder line = new StringBuilder();
        for (String cell : cells) {
            line.append(cell);
        }
        return line.toString();
    }

    private void eventResolutionScreen(ClientModel model) {
        blank();
        String title = model.getLastResolvedEventTitle();
        String desc  = model.getLastResolvedEventDescription();

        renderEventResolutionTitle();
        blank();
        renderEventDescription(title, desc);

        blank();
        renderEventResultsBox(model);
        blank();
    }

    private void renderEventResolutionTitle() {
        String[] lines = buildAsciiWordLines("RESOLVE EVENTS", 1);
        for (String line : lines) {
            row(center(CYN + BOLD + line + RST, W));
        }
    }

    private void renderEventDescription(String title, String desc) {
        if (title == null) {
            row(center(YLW + BOLD + "Resolving events..." + RST, W));
            return;
        }

        String[] titleLines = buildAsciiWordLines(title.toUpperCase(), 1);
        for (String line : titleLines) {
            row(center(YLW + BOLD + line + RST, W));
        }
        if (desc != null && !desc.isBlank()) {
            row(center(YLW + desc + RST, W));
        }
    }

    private void renderEventResultsBox(ClientModel model) {
        int innerW = 106;
        row(center(titledTop(" EVENT RESULTS ", innerW, CYN), W));
        row(center(eventResultLine(
                RED + BOLD + "PLAYER" + RST,
                RED + BOLD + "FOOD CHANGE" + RST,
                RED + BOLD + "FOOD TOTAL" + RST,
                RED + BOLD + "PRESTIGE CHANGE" + RST,
                RED + BOLD + "PRESTIGE TOTAL" + RST
        ), W));
        row(center(CYN + "|" + RST + RED + "-".repeat(innerW) + CYN + "|" + RST, W));

        List<EventResolvedMessage.PlayerEffect> effects = model.getLastResolvedEventEffects();
        if (effects.isEmpty()) {
            row(center(CYN + "|" + RST
                    + center(GRY + "waiting for event effects..." + RST, innerW)
                    + CYN + "|" + RST, W));
        } else {
            for (EventResolvedMessage.PlayerEffect effect : effects) {
                PlayerView player = model.getPlayers().get(effect.getNickname());
                String col = player != null ? totemColor(player.getTotemColor()) : WHT;
                String name = col + "● " + BOLD + ellipsize(effect.getNickname(), 18) + RST;

                row(center(eventResultLine(
                        name,
                        eventDelta(effect.getFoodDelta(), "🍖"),
                        col + BOLD + effect.getNewFood() + " 🍖" + RST,
                        eventDelta(effect.getPointsDelta(), "🌟"),
                        col + BOLD + effect.getNewPoints() + " 🌟" + RST
                ), W));
            }
        }

        row(center(roundedBottom(innerW, CYN), W));
    }

    private String eventResultLine(String player, String foodDelta, String foodTotal,
                                   String prestigeDelta, String prestigeTotal) {
        return CYN + "|" + RST
                + tableCellLeft(player, 24)
                + CYN + "|" + RST
                + tableCellCenter(foodDelta, 16)
                + CYN + "|" + RST
                + tableCellCenter(foodTotal, 16)
                + CYN + "|" + RST
                + tableCellCenter(prestigeDelta, 18)
                + CYN + "|" + RST
                + tableCellCenter(prestigeTotal, 18)
                + CYN + "|" + RST;
    }

    private String tableCellLeft(String content, int width) {
        return " " + padRight(content, width) + " ";
    }

    private String tableCellCenter(String content, int width) {
        return " " + center(content, width) + " ";
    }

    private String eventDelta(int delta, String icon) {
        String color = delta > 0 ? GRN : delta < 0 ? RED : GRY;
        String sign = delta > 0 ? "+" : "";
        return color + BOLD + sign + delta + " " + icon + RST;
    }

    private void endOfRoundScreen(ClientModel model) {
        blank();
        int finishedRound = model.getLastFinishedRound();
        if (finishedRound < 1) {
            // Fallback: should rarely happen, only if EndRound state arrives
            // before any RoundEnded message, for example after restoring from disk.
            finishedRound = Math.max(1, model.getRoundNumber() - 1);
        }
        renderEndRoundTitle(finishedRound);
        blank();
        renderEndRoundPlayersBox(model);
        blank();
        renderNextRoundBox(finishedRound + 1);
        blank();
    }

    private void renderEndRoundTitle(int finishedRound) {
        String[] endRound = buildAsciiWordLines("END ROUND", 1);

        for (String line : endRound) {
            row(center(CYN + BOLD + line + RST, W));
        }

        row(center(YLW + BOLD + "ROUND " + finishedRound + " ENDED" + RST, W));
    }

    private void renderEndRoundPlayersBox(ClientModel model) {
        int bw = 76;
        int screenW = W;
        row(center(titledTop(" PLAYERS ", bw, CYN), screenW));
        row(center(CYN + "|" + RST + " " + RED + BOLD
                + padRight("PLAYER", 28)
                + CYN + "|" + RST + " " + RED + BOLD
                + center("FOOD", 20)
                + CYN + "|" + RST + " " + RED + BOLD
                + center("PRESTIGE POINTS", 22)
                + RST + " " + CYN + "|" + RST, screenW));
        row(center(CYN + "|" + RST + RED + "-".repeat(bw) + CYN + "|" + RST, screenW));

        List<PlayerView> sortedPlayers = model.getPlayers().values().stream()
                .sorted((a, b) -> Integer.compare(b.getPrestigePoints(), a.getPrestigePoints()))
                .toList();

        for (int i = 0; i < sortedPlayers.size(); i++) {
            PlayerView p = sortedPlayers.get(i);
            String col = totemColor(p.getTotemColor());
            String name = rankLabel(i + 1) + " " + col + BOLD + ellipsize(p.getNickname(), 18) + RST;
            String food = col + BOLD + p.getFood() + " 🍖" + RST;
            String points = col + BOLD + p.getPrestigePoints() + " 🌟" + RST;

            row(center(CYN + "|" + RST + " " + padRight(name, 28)
                    + CYN + "|" + RST + " " + center(food, 20)
                    + CYN + "|" + RST + " " + center(points, 22)
                    + " " + CYN + "|" + RST, screenW));
        }

        row(center(roundedBottom(bw, CYN), screenW));
    }

    private void renderNextRoundBox(int nextRound) {
        int bw = 54;
        row(center(titledTop(" NEXT ROUND ", bw, ORG), W));
        row(center(ORG + "|" + RST + center(WHT + BOLD + "Preparing the next round..." + RST, bw) + ORG + "|" + RST, W));
        row(center(ORG + "|" + RST + center(YLW + BOLD + "NEXT: ROUND " + nextRound + RST, bw) + ORG + "|" + RST, W));
        row(center(roundedBottom(bw, ORG), W));
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
        List<String> tribeBox = buildTribeSidebar(model);
        boolean tribeRenderedAtTop = false;
        try {
            tribeRenderedAtTop = renderCardRowWithSideBox(upperAll, tribeBox);

            blank();
            renderCenterZone(model);
            blank();

            List<Integer> lowerAll = new ArrayList<>(model.getLowerRowCharacterIds());
            lowerAll.addAll(model.getLowerRowBuildingIds());
            renderCardRow(lowerAll);

            blank();

            renderPlayersAndTribe(model, totemPhase, tribeRenderedAtTop);
            drainActiveSideBox();
        } finally {
            endSideBox();
        }
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

    private boolean renderCardRowWithSideBox(List<Integer> ids, List<String> sideBox) {
        if (ids == null || ids.isEmpty()) {
            renderCardRow(ids);
            return false;
        }

        int maxWithSideBox = maxCardsWithSideBox(sideBox);
        if (maxWithSideBox <= 0) {
            renderCardRow(ids);
            return false;
        }

        beginSideBox(sideBox);
        for (int start = 0; start < ids.size(); start += maxWithSideBox) {
            List<Integer> slice = ids.subList(start, Math.min(start + maxWithSideBox, ids.size()));
            renderCardChunk(slice);
        }

        return true;
    }

    private int maxCardsWithSideBox(List<String> sideBox) {
        if (sideBox == null || sideBox.isEmpty()) {
            return 0;
        }
        int sideW = sideBox.stream().mapToInt(line -> visualWidth(stripAnsi(line))).max().orElse(0);
        int max = 0;
        for (int cards = 1; cards <= 10; cards++) {
            int cardsW = cards * (CARD_W + 2) + Math.max(0, cards - 1) * CARD_GAP;
            if (TOP_TRIBE_ROW_W - 2 - cardsW >= sideW + 3) {
                max = cards;
            }
        }
        return max;
    }

    private void renderCardChunk(List<Integer> ids) {
        String[][] cards = new String[ids.size()][];
        for (int i = 0; i < ids.size(); i++) {
            cards[i] = drawCard(ids.get(i));
        }

        for (int line = 0; line < CARD_H; line++) {
            row(cardChunkLine(cards, line));
        }
    }

    private String cardChunkLine(String[][] cards, int line) {
        StringBuilder sb = new StringBuilder("  ");
        for (int i = 0; i < cards.length; i++) {
            sb.append(cards[i][line]);
            if (i < cards.length - 1) sb.append(" ".repeat(CARD_GAP));
        }
        return sb.toString();
    }

    // Renderer owns the selected building-effect id; CardRenderer only
    // receives the resolved rendering flag.
    private String[] drawCard(int id) {
        return CardRenderer.drawCard(id, id == visibleBuildingEffectId);
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
        boolean actionResolution = GamePhase.fromStateName(model.getGameState()).isActionResolution();
        List<String> order = actionResolution ? actionResolutionOrder(model) : model.getTurnOrder();
        java.util.Set<String> placedPlayers = new java.util.HashSet<>(model.getTotemOnTiles().values());

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

            if (placedPlayers.contains(name) || model.isPlayerDisconnected(name)) {
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

    private void renderPlayersAndTribe(ClientModel model, boolean totemPhase, boolean tribeAlreadyRendered) {
        String currentActor = totemPhase
                ? model.getCurrentTotemPlacementPlayer()
                : model.getCurrentActionResolutionPlayer();

        List<String> left = new ArrayList<>();
        left.add("  " + CYN + BOLD + "PLAYERS" + RST);

        List<PlayerView> sorted = model.getPlayers().values().stream()
                .sorted((a, b) -> Integer.compare(b.getPrestigePoints(), a.getPrestigePoints()))
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            PlayerView p = sorted.get(i);
            left.addAll(buildPlayerLines(
                    p, i + 1,
                    p.getNickname().equals(currentActor),
                    p.getNickname().equals(model.getMyNickname()),
                    model.isPlayerDisconnected(p.getNickname())
            ));
        }

        List<String> right = tribeAlreadyRendered ? List.of() : buildTribeSidebar(model);

        int total = Math.max(left.size(), right.size());
        for (int i = 0; i < total; i++) {
            String l = i < left.size()  ? left.get(i)  : "";
            String r = i < right.size() ? right.get(i) : "";
            row(padVisual(l, LEFT_COL_W) + "  " + r);
        }
        blank();
    }

    private List<String> buildPlayerLines(PlayerView p, int rank, boolean isActor, boolean isMe, boolean disconnected) {
        List<String> lines = new ArrayList<>();

        String col       = totemColor(p.getTotemColor());
        String marker    = isActor ? YLW + BOLD + BLINK + "▶" + RST : disconnected ? RED + "x" + RST : " ";
        String meTag     = isMe ? CYN + BOLD + " you" + RST : "";
        String nameColor = disconnected ? GRY : isActor ? YLW : col;
        String nameStr   = nameColor + BOLD + String.format("%-14s", ellipsize(p.getNickname(), 14)) + RST;
        String foodStr   = YLW + BOLD + "🍖 " + String.format("%2d", p.getFood()) + RST;
        String ppColor   = p.getPrestigePoints() < 0 ? RED : GRN;
        String ppStr     = ppColor + BOLD + "🌟 " + String.format("%3d", p.getPrestigePoints()) + RST;

        lines.add("  " + rankLabel(rank) + "  " + marker + " "
                + nameStr
                + "   " + foodStr
                + "   " + foodBar(p.getFood(), 20)
                + "   " + ppStr
                + meTag
                + (isActor      ? YLW + BOLD + "   <-- TURN"     + RST : "")
                + (disconnected ? RED + BOLD + "   disconnected"  + RST : ""));

        return lines;
    }

    private List<String> buildTribeSidebar(ClientModel model) {
        List<String> lines = new ArrayList<>(buildTribeBox(model));
        String myNick = model.getMyNickname();

        int tribeNumber = 1;
        for (PlayerView player : model.getPlayers().values()) {
            if (player.getNickname().equals(myNick)) {
                continue;
            }
            lines.add("");
            lines.addAll(buildOtherTribeBox(player, tribeNumber, visibleTribeNumber == tribeNumber));
            tribeNumber++;
        }

        return lines;
    }

    private List<String> buildTribeBox(ClientModel model) {
        List<String> lines = new ArrayList<>();
        int bw = TRIBE_BOX_W;
        String tribeColor = ORG;

        String     myNick = model.getMyNickname();
        PlayerView me     = (myNick != null) ? model.getPlayers().get(myNick) : null;

        List<Integer> myCards = new ArrayList<>();
        if (me != null) {
            myCards.addAll(me.getCharacterCardIds());
            myCards.addAll(me.getBuildingCardIds());
        }

        lines.add(titledTop("MY TRIBE", bw, tribeColor));

        if (myCards.isEmpty()) {
            lines.add(tribeColor + "|" + RST + center(GRY + "no cards yet" + RST, bw) + tribeColor + "|" + RST);
        } else {
            for (int i = 0; i < myCards.size(); i++) {
                int id = myCards.get(i);
                CardCatalog.CardInfo ci = CardCatalog.get(id);
                String kindColor = ci.accept(KIND_COLOR);
                String label   = CardRenderer.tribeCardLabel(ci);
                String era     = GRY + "  [Era " + ci.era + "]" + RST;
                String content = " " + kindColor + BOLD + label + RST + era;
                lines.add(tribeColor + "|" + RST + padVisual(content, bw) + tribeColor + "|" + RST);
                if (i < myCards.size() - 1) {
                    lines.add(tribeEmptyLine(tribeColor, bw));
                }
            }
        }

        lines.add(roundedBottom(bw, tribeColor));
        return lines;
    }

    private List<String> buildOtherTribeBox(PlayerView player, int tribeNumber, boolean revealCards) {
        List<String> lines = new ArrayList<>();
        int bw = TRIBE_BOX_W;
        String color = totemColor(player.getTotemColor());
        String titlePrefix = tribeNumber + ") ";
        String titleSuffix = " TRIBE";
        String title = titlePrefix
                + ellipsize(player.getNickname().toUpperCase(), Math.max(1, bw - titlePrefix.length() - titleSuffix.length() - 2))
                + titleSuffix;

        lines.add(titledTop(title, bw, color));

        List<Integer> cards = new ArrayList<>();
        cards.addAll(player.getCharacterCardIds());
        cards.addAll(player.getBuildingCardIds());

        if (!revealCards) {
            lines.add(color + "|" + RST + center(WHT + BOLD + "type show tribe " + tribeNumber + RST, bw) + color + "|" + RST);
        } else if (cards.isEmpty()) {
            lines.add(color + "|" + RST + center(GRY + "no cards yet" + RST, bw) + color + "|" + RST);
        } else {
            for (int i = 0; i < cards.size(); i++) {
                int id = cards.get(i);
                CardCatalog.CardInfo ci = CardCatalog.get(id);
                String kindColor = ci.accept(KIND_COLOR);
                String era = GRY + "  [Era " + ci.era + "]" + RST;
                String content = " " + kindColor + BOLD + CardRenderer.tribeCardLabel(ci) + RST + era;
                lines.add(color + "|" + RST + padVisual(content, bw) + color + "|" + RST);
                if (i < cards.size() - 1) {
                    lines.add(tribeEmptyLine(color, bw));
                }
            }
        }

        if (revealCards) {
            lines.add(tribeEmptyLine(color, bw));
            lines.add(tribeEmptyLine(color, bw));
            lines.add(color + "|" + RST + closeHintLine(bw) + color + "|" + RST);
        }

        lines.add(roundedBottom(bw, color));
        return lines;
    }

    private String closeHintLine(int width) {
        String text = "to close press ENTER";
        String countdown = visibleTribeCountdownSeconds + "s";
        int spaces = Math.max(1, width - visualWidth(stripAnsi(text)) - visualWidth(stripAnsi(countdown)));
        return WHT + BOLD + text + RST + " ".repeat(spaces) + YLW + BOLD + countdown + RST;
    }

    private String tribeEmptyLine(String color, int width) {
        return color + "|" + RST + " ".repeat(width) + color + "|" + RST;
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
        return GamePhase.displayLabelFor(state);
    }

    private boolean isLastRound(ClientModel model) {
        return model != null && model.getRoundNumber() == LAST_ROUND;
    }

    private String footerPrompt(ClientModel model) {
        boolean iAmFirst = model.getMyNickname() != null
                && !model.getLobbyPlayers().isEmpty()
                && model.getMyNickname().equals(model.getLobbyPlayers().get(0));

        if (GamePhase.fromStateName(model.getGameState()) == GamePhase.LOBBY
                && model.getExpectedPlayers() < 2
                && iAmFirst) {
            return "  Number of players (2-5) > ";
        }
        return "  > ";
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
        GamePhase phase = GamePhase.fromStateName(state);

        if (state == null) {
            return List.of(
                    new String[]{"nickname", "enter your name when requested"},
                    new String[]{"color", "choose RED, BLUE, PURPLE, YELLOW or WHITE"}
            );
        }

        return switch (phase) {
            case GamePhase.Lobby ignored -> {
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

            case GamePhase.ReadyCheck ignored -> {
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

            case GamePhase.StartGame _, GamePhase.TotemPlacement _ -> {
                List<String[]> cmds = new java.util.ArrayList<>(List.of(
                        new String[]{"place <tile>", "place your totem on a tile, e.g. place B"},
                        new String[]{"see effect <id>", "show a building effect locally"},
                        new String[]{"show tribe <n>", "show one other tribe locally"},
                        new String[]{"help", "show quick help"},
                        new String[]{"quit", "leave the match"}
                ));
                if (Boolean.getBoolean("game.debug")) {
                    cmds.add(new String[]{"skip", "[DEBUG] auto-place all totems + skip round"});
                    cmds.add(new String[]{"endgame", "[DEBUG] force game over now"});
                }
                yield cmds;
            }

            case GamePhase.ActionResolution ignored -> {
                List<String[]> cmds = new java.util.ArrayList<>(List.of(
                        new String[]{"pick <id> upper", "take a character/event card from the upper row"},
                        new String[]{"pick <id> lower", "take a character/event card from the lower row"},
                        new String[]{"build <id> upper", "take a building card from the upper row"},
                        new String[]{"build <id> lower", "take a building card from the lower row"},
                        new String[]{"skipextra", "skip optional extra pick"},
                        new String[]{"see effect <id>", "show a building effect locally"},
                        new String[]{"show tribe <n>", "show one other tribe locally"},
                        new String[]{"help", "show quick help"},
                        new String[]{"quit", "leave the match"}
                ));
                if (Boolean.getBoolean("game.debug")) {
                    cmds.add(new String[]{"skip", "[DEBUG] skip all picks, go to next round"});
                    cmds.add(new String[]{"endgame", "[DEBUG] force game over now"});
                }
                yield cmds;
            }

            case GamePhase.EventResolution ignored -> List.of(
                    new String[]{"wait", "the server is resolving events"},
                    new String[]{"quit", "leave the match"}
            );

            case GamePhase.EndRound ignored -> List.of(
                    new String[]{"wait", "preparing the next round"},
                    new String[]{"show tribe <n>", "show one other tribe locally"},
                    new String[]{"quit", "leave the match"}
            );

            case GamePhase.Suspended ignored -> List.of(
                    new String[]{"wait", "timer active: the only connected player wins when it expires"},
                    new String[]{"see effect <id>", "show a building effect locally"},
                    new String[]{"show tribe <n>", "show one other tribe locally"},
                    new String[]{"quit", "leave the match"}
            );

            case GamePhase.EndGame ignored -> List.of(
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
        for (String nickname : model.getActionResolvedPlayers()) {
            if (!ordered.contains(nickname)) {
                ordered.add(nickname);
            }
        }
        return ordered;
    }

    private void printMesosLogoPlain() {
        System.out.println(centerPlain(RED + BOLD + "███╗   ███╗   ███████╗   ███████╗    ██████╗    ███████╗" + RST, W));
        System.out.println(centerPlain(RED + BOLD + "████╗ ████║   ██╔════╝   ██╔════╝   ██╔═══██╗   ██╔════╝" + RST, W));
        System.out.println(centerPlain(RED + BOLD + "██╔████╔██║   █████╗     ███████╗   ██║   ██║   ███████╗" + RST, W));
        System.out.println(centerPlain(RED + BOLD + "██║╚██╔╝██║   ██╔══╝     ╚════██║   ██║   ██║   ╚════██║" + RST, W));
        System.out.println(centerPlain(RED + BOLD + "██║ ╚═╝ ██║   ███████╗   ███████║   ╚██████╔╝   ███████║" + RST, W));
        System.out.println(centerPlain(RED + BOLD + "╚═╝     ╚═╝   ╚══════╝   ╚══════╝    ╚═════╝    ╚══════╝" + RST, W));
    }

    private String availableColorLabels(ClientModel model) {
        List<String> labels = new ArrayList<>();
        for (TotemColor color : model.getAvailableTotemColors()) {
            labels.add(colorLabel(color));
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
