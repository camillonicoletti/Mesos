from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas

OUT = "deliverables/final/report/report.pdf"
LOGO = "src/main/resources/assets/ui/mesos_title.png"
VICTORY_BG = "src/main/resources/assets/ui/background_victory.png"
GAME_BG = "src/main/resources/assets/ui/game_background.jpeg"
PAGE_W, PAGE_H = A4
FONT_DIR = (
    "/Users/giorgio/.cache/codex-runtimes/codex-primary-runtime/dependencies/"
    "native/libreoffice-headless/libreoffice/LibreOfficeDev.app/Contents/"
    "Resources/fonts/truetype"
)
REGULAR = "Rubik"
BOLD = "Rubik-Bold"

PLUM = colors.HexColor("#211936")
PLUM_2 = colors.HexColor("#3a2141")
EMBER = colors.HexColor("#c93a24")
ORANGE = colors.HexColor("#f06c22")
GOLD = colors.HexColor("#f6b739")
CREAM = colors.HexColor("#fff1c2")
PAPER = colors.HexColor("#fff8ec")
PAPER_2 = colors.HexColor("#fff3df")
GRID = colors.HexColor("#e5b884")
TEXT = colors.HexColor("#241f2c")
MUTED = colors.HexColor("#6f5660")
TEAL = colors.HexColor("#11879a")


def register_fonts():
    pdfmetrics.registerFont(TTFont(REGULAR, f"{FONT_DIR}/Rubik-Regular.ttf"))
    pdfmetrics.registerFont(TTFont(BOLD, f"{FONT_DIR}/Rubik-Bold.ttf"))


def wrap_text(c, value, width, font=REGULAR, size=9):
    words = value.replace("\n", " \n ").split()
    lines, current = [], ""
    for word in words:
        if word == "\n":
            lines.append(current)
            current = ""
            continue
        candidate = word if not current else f"{current} {word}"
        if c.stringWidth(candidate, font, size) <= width:
            current = candidate
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def draw_wrapped(c, value, x, y, width, font=REGULAR, size=9, leading=11, color=TEXT):
    c.setFont(font, size)
    c.setFillColor(color)
    for line in wrap_text(c, value, width, font, size):
        c.drawString(x, y, line)
        y -= leading
    return y


def draw_logo(c, x, y, w, h):
    c.drawImage(ImageReader(LOGO), x, y, width=w, height=h, preserveAspectRatio=True, mask="auto")


def rounded_box(c, x, y, w, h, fill=PAPER):
    c.setFillColor(fill)
    c.setStrokeColor(GRID)
    c.setLineWidth(0.65)
    c.roundRect(x, y, w, h, 4, fill=1, stroke=1)


def section_title(c, title, x, y, w=72 * mm):
    c.setFillColor(EMBER)
    c.setFont(BOLD, 10.2)
    c.drawString(x, y, title.upper())
    c.setStrokeColor(ORANGE)
    c.setLineWidth(1.05)
    c.line(x, y - 4, x + w, y - 4)


def header(c, page):
    header_h = 30 * mm
    c.drawImage(ImageReader(GAME_BG), 0, PAGE_H - header_h, PAGE_W, header_h, mask="auto")
    c.saveState()
    c.setFillColor(PLUM)
    c.setFillAlpha(0.48)
    c.roundRect(18 * mm, PAGE_H - 25 * mm, PAGE_W - 36 * mm, 18 * mm, 4, fill=1, stroke=0)
    c.restoreState()
    draw_logo(c, 23 * mm, PAGE_H - 23.2 * mm, 47 * mm, 16 * mm)
    c.setFillColor(CREAM)
    c.setFont(BOLD, 8.2)
    c.drawString(78 * mm, PAGE_H - 13.4 * mm, "Benedetto Oliva, Giorgio Ortona, Alessandro Mazzeo, Camillo Nicoletti")
    c.setFont(REGULAR, 7.0)
    c.setFillColor(colors.HexColor("#ffd88a"))
    c.drawString(78 * mm, PAGE_H - 20.2 * mm, "Mini report finale - Mesos")
    c.setFillColor(colors.white)
    c.setFont(BOLD, 8.3)
    c.drawRightString(PAGE_W - 23 * mm, PAGE_H - 16.0 * mm, f"{page}/2")


def footer(c, page):
    c.setStrokeColor(GRID)
    c.setLineWidth(0.5)
    c.line(18 * mm, 14 * mm, PAGE_W - 18 * mm, 14 * mm)
    c.setFillColor(MUTED)
    c.setFont(REGULAR, 7.5)
    c.drawString(18 * mm, 9 * mm, "Ingegneria del Software - Prova finale")
    c.drawRightString(PAGE_W - 18 * mm, 9 * mm, f"{page}/2")


def page_one(c):
    header(c, 1)
    footer(c, 1)

    x0 = 18 * mm
    top = PAGE_H - 39 * mm
    content_w = PAGE_W - 36 * mm
    gap = 8 * mm
    col_w = (content_w - gap) / 2
    col_h = 61 * mm

    rounded_box(c, x0, top - col_h, col_w, col_h, PAPER)
    rounded_box(c, x0 + col_w + gap, top - col_h, col_w, col_h, PAPER)

    section_title(c, "Funzionalita implementate", x0 + 5 * mm, top - 9 * mm)
    feature_text = (
        "Il gioco supporta partite da 2 a 5 giocatori. Il server gestisce login, lobby, scelta del colore e "
        "ready check, poi conduce la partita seguendo ere, round, ordine di turno, offer tile e piazzamento "
        "dei totem. Ogni azione viene validata lato server prima di modificare lo stato. Sono implementate "
        "carte personaggio, edificio ed evento, effetti permanenti o temporanei, costo in cibo degli edifici, "
        "bonus, sustenance, fine round e punteggio finale con gestione dei pareggi. Il client puo partire in "
        "TUI o GUI JavaFX e comunica con lo stesso server tramite Socket o RMI, anche nella stessa lobby. "
        "Le funzionalita aggiuntive coprono disconnessione e riconnessione, sospensione quando resta un solo "
        "giocatore online, timeout, salvataggio su disco e leaderboard JDBC opzionale."
    )
    draw_wrapped(c, feature_text, x0 + 5 * mm, top - 19 * mm, col_w - 10 * mm, size=6.45, leading=7.25)

    rx = x0 + col_w + gap
    section_title(c, "Divisione del lavoro", rx + 5 * mm, top - 9 * mm)
    division = [
        "Benedetto Oliva: model, carte, effetti, regole di dominio e test del model.",
        "Giorgio Ortona: Socket/RMI, client controller, riconnessioni/disconnessioni, GUI e integrazione.",
        "Alessandro Mazzeo: controller, stati di gioco, flussi di partita e regressioni.",
        "Camillo Nicoletti: TUI, comandi utente, persistenza, leaderboard e deliverable.",
    ]
    y = top - 19 * mm
    for item in division:
        c.setFillColor(ORANGE)
        c.circle(rx + 5.5 * mm, y + 2, 1.15, fill=1, stroke=0)
        y = draw_wrapped(c, item, rx + 8 * mm, y, col_w - 13 * mm, size=7.35, leading=8.6)
        y -= 2.4 * mm

    patterns_y = top - col_h - 8 * mm
    section_title(c, "Scelte architetturali e pattern", x0, patterns_y, 78 * mm)
    draw_wrapped(
        c,
        "Il progetto usa MVC con server autoritativo: il client invia intenzioni, mentre il server valida le azioni e conserva lo stato ufficiale. Il client mantiene solo una copia locale aggiornata dai messaggi ricevuti.",
        x0,
        patterns_y - 8 * mm,
        content_w,
        size=7.45,
        leading=8.7,
    )

    table_x = x0
    table_top = patterns_y - 19 * mm
    head_h = 8 * mm
    row_h = 14.05 * mm
    col1, col2, col3 = 35 * mm, 61 * mm, content_w - 96 * mm
    c.setFillColor(PLUM)
    c.rect(table_x, table_top - head_h, content_w, head_h, fill=1, stroke=0)
    c.setFillColor(CREAM)
    c.setFont(BOLD, 7.05)
    c.drawString(table_x + 2.3 * mm, table_top - 5.2 * mm, "Pattern")
    c.drawString(table_x + col1 + 2.3 * mm, table_top - 5.2 * mm, "Dove")
    c.drawString(table_x + col1 + col2 + 2.3 * mm, table_top - 5.2 * mm, "Perche lo usiamo")

    rows = [
        ("State", "LobbyState, TotemPlacementState, ActionResolutionState, SuspendedState, EndGameState", "Incapsula transizioni e invarianti di fase; evita branch centralizzati e riduce stati illegali."),
        ("Command", "GameAction e sottoclassi", "Incapsula l'input utente in oggetti validabili prima dell'applicazione al dominio."),
        ("DTO / Protocol messages", "serverbound, clientbound", "Definisce un boundary stabile di serializzazione; rete e dominio restano disaccoppiati."),
        ("Factory + Strategy", "ClientConnectionFactory, ClientSideConnection", "Applica dependency inversion: il controller dipende da un'interfaccia e il trasporto si seleziona a runtime."),
        ("Adapter", "Socket*Connection, RMI*Connection, VirtualView", "Normalizza API diverse, stream/RMI/callback, in un contratto comune isolando i dettagli infrastrutturali."),
        ("Dispatcher", "MessageDispatcher, mappe handler nel client", "Usa una tabella di handler per tipo messaggio, eliminando catene instanceof e centralizzando il routing."),
        ("Observer", "GameListener, callback setOnMessage, setOnDisconnect", "Realizza un publish/subscribe leggero: eventi verso view e rete senza dipendere dai subscriber concreti."),
        ("Visitor", "carte ed effetti", "Usa double dispatch sugli effetti: comportamento specializzato senza switch sul tipo e con estensione localizzata."),
        ("Snapshot", "GameStartedMessage, GameSyncMessage", "Trasferisce uno stato consistente e serializzabile, utile per bootstrap e recovery idempotente."),
        ("Supervisor / Retry", "ClientConnectionSupervisor", "Isola la politica di reconnect con retry temporizzato, senza bloccare UI o duplicare logica di connessione."),
    ]
    y = table_top - head_h
    for i, (pattern, where, why) in enumerate(rows):
        y -= row_h
        c.setFillColor(PAPER_2 if i % 2 else colors.white)
        c.rect(table_x, y, content_w, row_h, fill=1, stroke=0)
        c.setStrokeColor(GRID)
        c.setLineWidth(0.35)
        c.line(table_x, y, table_x + content_w, y)
        c.setFillColor(TEXT)
        c.setFont(BOLD, 5.9)
        c.drawString(table_x + 2.2 * mm, y + row_h - 4.8 * mm, pattern)
        draw_wrapped(c, where, table_x + col1 + 2.2 * mm, y + row_h - 4.8 * mm, col2 - 4.2 * mm, size=5.45, leading=6.15)
        draw_wrapped(c, why, table_x + col1 + col2 + 2.2 * mm, y + row_h - 4.8 * mm, col3 - 4.2 * mm, size=5.25, leading=5.95)

    table_h = head_h + row_h * len(rows)
    c.setStrokeColor(GRID)
    c.rect(table_x, table_top - table_h, content_w, table_h, fill=0, stroke=1)
    c.line(table_x + col1, table_top, table_x + col1, table_top - table_h)
    c.line(table_x + col1 + col2, table_top, table_x + col1 + col2, table_top - table_h)


def draw_step(c, x, y, w, n, title, body):
    c.setFillColor(ORANGE)
    c.circle(x + 5 * mm, y + 8 * mm, 4.5 * mm, fill=1, stroke=0)
    c.setFillColor(PLUM)
    c.setFont(BOLD, 8)
    c.drawCentredString(x + 5 * mm, y + 6.75 * mm, str(n))
    c.setFillColor(TEXT)
    c.setFont(BOLD, 7.2)
    c.drawString(x + 12 * mm, y + 10 * mm, title)
    draw_wrapped(c, body, x + 12 * mm, y + 5.5 * mm, w - 14 * mm, size=6.3, leading=7.2, color=MUTED)


def page_two(c):
    header(c, 2)
    footer(c, 2)
    x0 = 18 * mm
    content_w = PAGE_W - 36 * mm
    top = PAGE_H - 39 * mm

    rounded_box(c, x0, top - 65 * mm, content_w, 65 * mm, PAPER)
    section_title(c, "Protocollo e persistenza", x0 + 5 * mm, top - 10 * mm)
    left = x0 + 5 * mm
    right = x0 + content_w / 2 + 4 * mm
    w = content_w / 2 - 10 * mm
    draw_wrapped(
        c,
        "Il protocollo usa messaggi DTO serializzabili: sono oggetti semplici che trasportano dati tra client e server senza contenere logica di gioco. I ClientMessage includono LoginMessage, ReadyMessage, GameActionMessage, ReconnectMessage, DisconnectMessage, QuitMessage; i ServerMessage includono LoginAckMessage, GameStartedMessage, GameSyncMessage, StateChangedMessage, PlayerDisconnectedMessage, GameSuspendedMessage, GameResumedMessage.",
        left,
        top - 22 * mm,
        w,
        size=7.05,
        leading=8.25,
    )
    draw_wrapped(
        c,
        "Durante il gioco normale il server invia messaggi delta: solo cio che cambia. Il GameSnapshot completo viene costruito solo quando serve una sincronizzazione completa: inizio partita o riconnessione. La persistenza non salva lo snapshot, ma serializza il GameController in mesos-savestate.bin; al restart il server ricarica il controller e genera un nuovo snapshot per ogni client che si riconnette.",
        right,
        top - 22 * mm,
        w,
        size=7.5,
        leading=9.0,
    )

    msg_y = top - 76 * mm
    section_title(c, "Messaggi principali", x0, msg_y)
    small_y = msg_y - 9 * mm
    c.setFillColor(PLUM)
    c.rect(x0, small_y - 7 * mm, content_w, 7 * mm, fill=1, stroke=0)
    c.setFillColor(CREAM)
    c.setFont(BOLD, 7.4)
    c.drawString(x0 + 3 * mm, small_y - 4.8 * mm, "Direzione")
    c.drawString(x0 + 35 * mm, small_y - 4.8 * mm, "Messaggi")
    msg_rows = [
        ("Client -> Server", "LoginMessage, ReadyMessage, GameActionMessage, ReconnectMessage, DisconnectMessage, QuitMessage"),
        ("Server -> Client", "LoginAckMessage, GameStartedMessage, GameSyncMessage, StateChangedMessage, PlayerDisconnectedMessage, GameSuspendedMessage, GameResumedMessage"),
    ]
    y = small_y - 7 * mm
    for i, (side, msgs) in enumerate(msg_rows):
        c.setFillColor(colors.white if i == 0 else PAPER_2)
        c.rect(x0, y - 9 * mm, content_w, 9 * mm, fill=1, stroke=0)
        c.setFillColor(TEXT)
        c.setFont(BOLD, 6.8)
        c.drawString(x0 + 3 * mm, y - 5.8 * mm, side)
        draw_wrapped(c, msgs, x0 + 35 * mm, y - 3.2 * mm, content_w - 39 * mm, size=6.25, leading=7.0)
        y -= 9 * mm
    c.setStrokeColor(GRID)
    c.rect(x0, small_y - 25 * mm, content_w, 25 * mm, fill=0, stroke=1)
    c.line(x0 + 32 * mm, small_y, x0 + 32 * mm, small_y - 25 * mm)

    flow_y = msg_y - 43 * mm
    section_title(c, "Flusso reconnect", x0, flow_y)
    step_w = (content_w - 8 * mm) / 5
    step_y = flow_y - 30 * mm
    steps = [
        ("Guasto rilevato", "heartbeat, eccezione o onDisconnect"),
        ("Cleanup server", "LobbyManager pulisce la connessione"),
        ("Stato partita", "disconnessione e sospensione"),
        ("Retry client", "nuova richiesta ogni 2 sec"),
        ("Riallineamento", "ack, snapshot e stato corrente"),
    ]
    for i, (title, body) in enumerate(steps):
        sx = x0 + i * (step_w + 2 * mm)
        rounded_box(c, sx, step_y, step_w, 23 * mm, colors.white)
        draw_step(c, sx, step_y + 3 * mm, step_w, i + 1, title, body)
        if i < 4:
            c.setStrokeColor(ORANGE)
            c.setLineWidth(1)
            c.line(sx + step_w + 0.3 * mm, step_y + 11.5 * mm, sx + step_w + 1.8 * mm, step_y + 11.5 * mm)

    tests_y = flow_y - 44 * mm
    section_title(c, "Test e copertura", x0, tests_y)
    draw_wrapped(
        c,
        "La suite contiene 95 classi di test su model, controller, stati, network, protocollo, persistenza e riconnessioni. La copertura e misurata con JaCoCo tramite mvn clean verify. GUI/TUI, launcher e JDBC leaderboard sono validati anche con prove manuali perche dipendono da interazione grafica, processo o database esterno.",
        x0,
        tests_y - 9 * mm,
        content_w,
        size=7.7,
        leading=9.0,
    )

    chart_x = x0
    chart_y = tests_y - 75 * mm
    chart_w = 96 * mm
    chart_h = 52 * mm
    rounded_box(c, chart_x, chart_y, chart_w, chart_h, PAPER)
    c.setFillColor(PLUM)
    c.setFont(BOLD, 8)
    c.drawString(chart_x + 4 * mm, chart_y + chart_h - 7 * mm, "Copertura istruzioni JaCoCo")
    bars = [
        ("Globale", 38.7, EMBER),
        ("Core senza view", 75.2, ORANGE),
        ("Model", 88.4, GOLD),
        ("Controller", 75.9, ORANGE),
        ("Network", 65.5, TEAL),
        ("Protocol DTO", 88.5, GOLD),
    ]
    by = chart_y + chart_h - 16 * mm
    for label, value, color in bars:
        c.setFillColor(TEXT)
        c.setFont(REGULAR, 6.8)
        c.drawString(chart_x + 4 * mm, by, label)
        c.setFillColor(colors.HexColor("#f0d8bd"))
        c.rect(chart_x + 33 * mm, by - 1, 45 * mm, 4, fill=1, stroke=0)
        c.setFillColor(color)
        c.rect(chart_x + 33 * mm, by - 1, 45 * mm * value / 100, 4, fill=1, stroke=0)
        c.setFillColor(TEXT)
        c.setFont(BOLD, 6.6)
        c.drawRightString(chart_x + chart_w - 4 * mm, by, f"{value:.1f}%")
        by -= 6.6 * mm

    table_x = x0 + chart_w + 8 * mm
    table_w = content_w - chart_w - 8 * mm
    rounded_box(c, table_x, chart_y, table_w, chart_h, PAPER)
    c.setFillColor(PLUM)
    c.setFont(BOLD, 8)
    c.drawString(table_x + 4 * mm, chart_y + chart_h - 7 * mm, "Indicatori sintetici")
    metrics = [
        ("Test class", "95"),
        ("Branch globale", "36.0%"),
        ("Linee globali", "41.6%"),
        ("Core linee", "73.5%"),
        ("Comando coverage", "mvn clean verify"),
        ("Validazione manuale", "GUI/TUI, launcher, JDBC"),
    ]
    metric_row_h = 5.75 * mm
    metrics_top = chart_y + chart_h - 16 * mm
    for i, (k, v) in enumerate(metrics):
        row_top = metrics_top - i * metric_row_h
        row_bottom = row_top - metric_row_h
        baseline = row_bottom + 1.85 * mm
        c.setFillColor(colors.white if i % 2 == 0 else PAPER_2)
        c.rect(table_x + 4 * mm, row_bottom, table_w - 8 * mm, metric_row_h, fill=1, stroke=0)
        c.setFillColor(TEXT)
        c.setFont(BOLD, 6.35)
        c.drawString(table_x + 6 * mm, baseline, k)
        c.setFont(REGULAR, 6.35)
        c.drawRightString(table_x + table_w - 6 * mm, baseline, v)


def main():
    register_fonts()
    c = canvas.Canvas(OUT, pagesize=A4)
    c.setTitle("Mesos - Mini report finale")
    c.setAuthor("Benedetto Oliva, Giorgio Ortona, Alessandro Mazzeo, Camillo Nicoletti")
    page_one(c)
    c.showPage()
    page_two(c)
    c.save()


if __name__ == "__main__":
    main()
