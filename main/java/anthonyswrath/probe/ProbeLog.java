package anthonyswrath.probe;

import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * T004 timing-probe logging helper (development only, removed by T035).
 *
 * <p>Writes every line twice: stdout (MTS console) and {@code <user.home>/awprobe.log}
 * so the user can hand the log back verbatim. Never throws: a probe failure must
 * not affect the run.</p>
 */
public final class ProbeLog {

    /** Card IDs this feature cares about (reference card + future mod card). */
    private static final String[] TRACKED = {
            "CurseOfTheBell", "Necronomicurse", "AscendersBane", "Pride", "AnthonysWrath"
    };

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final SimpleDateFormat TS = new SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT);
    private static PrintWriter writer;
    private static boolean fileTried;

    private ProbeLog() {
    }

    public static void log(String tag, String msg) {
        String line = "[AWP " + TS.format(new Date()) + " #" + SEQ.incrementAndGet() + "] " + tag + " | " + msg;
        System.out.println(line);
        writeFile(line);
    }

    private static synchronized void writeFile(String line) {
        try {
            if (!fileTried) {
                fileTried = true;
                String home = System.getProperty("user.home", ".");
                writer = new PrintWriter(new FileWriter(new File(home, "awprobe.log"), true), true);
            }
            if (writer != null) {
                writer.println(line);
            }
        } catch (Throwable ignored) {
            // probe must never break the game
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
