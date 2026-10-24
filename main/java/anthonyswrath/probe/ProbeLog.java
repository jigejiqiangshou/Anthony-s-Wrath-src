package anthonyswrath.probe;

import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * T004 timing-probe logging helper (development only, removed by T035).
 *
 * <p>Writes every line twice: stdout (MTS console) and one log file. The file is
 * resolved once, in this order:</p>
 * <ol>
 *   <li>environment variable {@code AWPROBE_LOG} (absolute path);</li>
 *   <li>first non-empty, non-{@code #} line of {@code <user.home>/awprobe.logpath};</li>
 *   <li>default {@code <user.home>/awprobe.log}.</li>
 * </ol>
 *
 * <p>The first line written to the file records the resolved absolute path, so the
 * reader never has to guess. Setting the target inside the project workspace makes the
 * log readable in place (the repo's {@code *.log} ignore rule keeps it out of git).
 * Never throws: a probe failure must not affect the run.</p>
 */
public final class ProbeLog {

    /** Card IDs this feature cares about (reference cards + this mod's card, full id). */
    private static final String[] TRACKED = {
            "CurseOfTheBell", "Necronomicurse", "AscendersBane", "Pride",
            "anthonyswrath:AnthonysWrath"
    };

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final SimpleDateFormat TS = new SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT);
    private static PrintWriter writer;
    private static boolean fileTried;
    private static boolean resolved;
    private static File logFile;
    private static String logSource = "default:user.home";

    private ProbeLog() {
    }

    public static void log(String tag, String msg) {
        String line = "[AWP " + TS.format(new Date()) + " #" + SEQ.incrementAndGet() + "] " + tag + " | " + msg;
        System.out.println(line);
        writeFile(line);
    }

    /** Absolute path of the resolved log target plus where the setting came from. */
    public static synchronized String logPath() {
        resolve();
        return logFile.getAbsolutePath() + " (source=" + logSource + ")";
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        String override = null;
        String source = null;
        try {
            String fromEnv = System.getenv("AWPROBE_LOG");
            if (fromEnv != null && !fromEnv.trim().isEmpty()) {
                override = fromEnv.trim();
                source = "env:AWPROBE_LOG";
            }
        } catch (Throwable ignored) {
        }
        if (override == null) {
            try {
                File cfg = new File(System.getProperty("user.home", "."), "awprobe.logpath");
                if (cfg.isFile()) {
                    String fromFile = firstMeaningfulLine(cfg);
                    if (fromFile != null) {
                        override = fromFile;
                        source = "file:" + cfg.getAbsolutePath();
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        if (override != null) {
            logFile = new File(override);
            logSource = source;
        } else {
            logFile = new File(System.getProperty("user.home", "."), "awprobe.log");
            logSource = "default:user.home";
        }
    }

    /**
     * Reads the first non-empty, non-{@code #} line of the pointer file. Accepts UTF-8
     * (with or without BOM) and, when the UTF-8 decode is not clean, the platform default
     * charset, so a pointer file written by Notepad or PowerShell either way still works.
     */
    private static String firstMeaningfulLine(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (text.indexOf('\uFFFD') >= 0) {
                text = new String(bytes);
            }
            if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
                text = text.substring(1);
            }
            for (String rawLine : text.split("\\r?\\n")) {
                String line = rawLine.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    return line;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static synchronized void writeFile(String line) {
        try {
            if (!fileTried) {
                fileTried = true;
                resolve();
                writer = open(logFile);
                if (writer == null && !logFile.equals(new File(System.getProperty("user.home", "."), "awprobe.log"))) {
                    File fallback = new File(System.getProperty("user.home", "."), "awprobe.log");
                    writer = open(fallback);
                }
            }
            if (writer != null) {
                writer.println(line);
            }
        } catch (Throwable ignored) {
            // probe must never break the game
        }
    }

    private static PrintWriter open(File file) {
        try {
            File parent = file.getAbsoluteFile().getParentFile();
            if (parent != null && !parent.isDirectory()) {
                parent.mkdirs();
            }
            PrintWriter out = new PrintWriter(new FileWriter(file, true), true);
            out.println("===== awprobe log: " + file.getAbsolutePath() + " (source=" + logSource + ")");
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * {@code size=N [0:id, 1:id, ...]} with index 0 = <b>bottom</b> of the group and the
     * <b>last</b> index = top (the next card drawn / played). Verified by the T004 probe
     * log: {@code getTopCard()} returns {@code group.get(size-1)} and
     * {@code Collections.shuffle} keeps that meaning; see {@code memory/log/018}.
     */
    public static String ids(CardGroup g) {
        if (g == null) {
            return "null-group";
        }
        if (g.group == null) {
            return "null-list";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("size=").append(g.group.size()).append(" [");
        for (int i = 0; i < g.group.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(i).append(':').append(g.group.get(i).cardID);
        }
        sb.append(']');
        return sb.toString();
    }

    /** Positions of all tracked card IDs inside the given group (empty if none). */
    public static String trackedPositions(CardGroup g) {
        if (g == null || g.group == null) {
            return "tracked=?";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("tracked={");
        boolean first = true;
        for (String id : TRACKED) {
            for (int i = 0; i < g.group.size(); i++) {
                if (id.equals(g.group.get(i).cardID)) {
                    if (!first) {
                        sb.append(", ");
                    }
                    first = false;
                    sb.append(id).append('@').append(i);
                }
            }
        }
        sb.append(first ? "none}" : "}");
        return sb.toString();
    }

    /** Human-readable identity of a CardGroup: drawPile / discardPile / hand / masterDeck / other. */
    public static String groupTag(CardGroup g) {
        if (g == null) {
            return "null";
        }
        AbstractPlayer p = AbstractDungeon.player;
        if (p != null) {
            if (g == p.drawPile) {
                return "player.drawPile";
            }
            if (g == p.discardPile) {
                return "player.discardPile";
            }
            if (g == p.hand) {
                return "player.hand";
            }
            if (g == p.masterDeck) {
                return "player.masterDeck";
            }
            if (g == p.exhaustPile) {
                return "player.exhaustPile";
            }
        }
        return "other(type=" + g.type + ")";
    }

    /** Sizes of the player's four piles. */
    public static String pileSizes() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            return "player=null";
        }
        return "draw=" + sizeOf(p.drawPile) + " discard=" + sizeOf(p.discardPile)
                + " hand=" + sizeOf(p.hand) + " exhaust=" + sizeOf(p.exhaustPile)
                + " masterDeck=" + sizeOf(p.masterDeck);
    }

    /** Size of a CardGroup, -1 when absent. */
    public static int sizeOf(CardGroup g) {
        return (g == null || g.group == null) ? -1 : g.group.size();
    }

    /** Full dump of the player's piles, handy right after a shuffle. */
    public static String pileDump() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            return "player=null";
        }
        return "\n    drawPile    " + ids(p.drawPile) + " " + trackedPositions(p.drawPile)
                + "\n    discardPile " + ids(p.discardPile)
                + "\n    hand        " + ids(p.hand)
                + "\n    masterDeck  " + ids(p.masterDeck);
    }

    /** Selected custom-mode flags (set by the custom-mode screen) and enabled daily-mod IDs. */
    public static String mods() {
        StringBuilder sb = new StringBuilder();
        sb.append("customMods=");
        try {
            sb.append(AbstractPlayer.customMods == null ? "null" : AbstractPlayer.customMods.toString());
        } catch (Throwable t) {
            sb.append("err");
        }
        sb.append(" enabledMods=");
        try {
            ArrayList<String> ids = ModHelper.getEnabledModIDs();
            sb.append(ids == null ? "null" : ids.toString());
        } catch (Throwable t) {
            sb.append("err");
        }
        return sb.toString();
    }

    /** Room / floor / pile summary + tracked positions. */
    public static String state() {
        StringBuilder sb = new StringBuilder();
        sb.append("floor=").append(AbstractDungeon.floorNum);
        AbstractRoom room = null;
        try {
            room = AbstractDungeon.getCurrRoom();
        } catch (Throwable ignored) {
        }
        sb.append(" room=").append(room == null ? "null" : room.getClass().getSimpleName());
        if (room != null) {
            sb.append(" phase=").append(room.phase);
        }
        sb.append(" ").append(pileSizes());
        AbstractPlayer p = AbstractDungeon.player;
        if (p != null && p.drawPile != null) {
            sb.append("\n    drawPile ").append(trackedPositions(p.drawPile));
        }
        return sb.toString();
    }

    /** Enclosing game/mod frames of the current call, closest first. */
    public static String caller() {
        StringBuilder sb = new StringBuilder("caller=");
        try {
            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            int n = 0;
            for (StackTraceElement e : st) {
                String cn = e.getClassName();
                if (cn.startsWith("anthonyswrath.probe.") || cn.equals(ProbeLog.class.getName())) {
                    continue;
                }
                if (cn.startsWith("com.megacrit") || cn.startsWith("basemod")) {
                    if (n > 0) {
                        sb.append(" <- ");
                    }
                    sb.append(simple(cn)).append('.').append(e.getMethodName()).append(':').append(e.getLineNumber());
                    n++;
                    if (n >= 6) {
                        break;
                    }
                }
            }
            if (n == 0) {
                sb.append("none");
            }
        } catch (Throwable t) {
            sb.append("err");
        }
        return sb.toString();
    }

    private static String simple(String className) {
        int i = className.lastIndexOf('.');
        return i < 0 ? className : className.substring(i + 1);
    }
}
