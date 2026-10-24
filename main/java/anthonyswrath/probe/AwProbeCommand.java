package anthonyswrath.probe;

import basemod.devcommands.ConsoleCommand;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.curses.CurseOfTheBell;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * T004 dev-console command {@code awprobe} (development only, removed by T035).
 *
 * <p>Usage inside the game console (BaseMod Dev Console, grave/backquote key):</p>
 * <pre>
 * awprobe info             dump floor/room/piles/custom mods
 * awprobe purge            call masterDeck.getPurgeableCards() and log the result
 * awprobe cursepool        dump AbstractDungeon.curseCardPool
 * awprobe find &lt;text&gt;     list card IDs containing the text (any case)
 * awprobe addcard &lt;id&gt;    add a copy of the card to masterDeck ({@code bell} = CurseOfTheBell)
 * awprobe bell             shorthand for {@code addcard bell} (no argument to get wrong)
 * awprobe transform &lt;id&gt;  call AbstractDungeon.transformCard(card, true) and log the result
 * awprobe shuffledeck      call player.drawPile.shuffle() (exercises the shuffle patches)
 * </pre>
 *
 * <p>Card IDs are resolved case-insensitively and {@code bell} is an alias for
 * {@code CurseOfTheBell}; when a direct {@link CardLibrary} lookup is unavailable the
 * {@link CurseOfTheBell} class is instantiated directly. The command logs the raw
 * token array so a later console-side surprise is diagnosable from {@code awprobe.log}.
 * Tab completion only offers card IDs at the argument position, not sub-command names
 * (the old build offered sub-command names there, which let BaseMod's auto-complete
 * replace a partly typed card ID with {@code cursepool}).</p>
 */
public class AwProbeCommand extends ConsoleCommand {

    private static final String BELL_ID = "CurseOfTheBell";

    private static final String[] SUBCOMMANDS = {
            "info", "purge", "cursepool", "find", "addcard", "bell", "transform", "shuffledeck"
    };

    public AwProbeCommand() {
        minExtraTokens = 0;
        maxExtraTokens = 2;
        simpleCheck = false;
        requiresPlayer = false;
    }

    @Override
    protected void execute(String[] tokens, int depth) {
        String sub = tokenAt(tokens, 1);
        String arg = tokenAt(tokens, 2);
        String cmd = sub == null ? "info" : sub.toLowerCase(Locale.ROOT);
        ProbeLog.log("cmd.raw", "depth=" + depth + " tokens=" + Arrays.toString(tokens)
                + " -> cmd=" + cmd + " arg=" + arg);
        try {
            if ("info".equals(cmd)) {
                info();
            } else if ("purge".equals(cmd)) {
                purge();
            } else if ("cursepool".equals(cmd)) {
                cursepool();
            } else if ("find".equals(cmd)) {
                find(arg);
            } else if ("addcard".equals(cmd)) {
                addCard(arg);
            } else if ("bell".equals(cmd) || "addbell".equals(cmd)) {
                addCard(BELL_ID);
            } else if ("transform".equals(cmd)) {
                transform(arg);
            } else if ("transformbell".equals(cmd)) {
                transform(BELL_ID);
            } else if ("shuffledeck".equals(cmd)) {
                shuffleDrawPile();
            } else {
                usage();
            }
        } catch (Throwable t) {
            ProbeLog.log("cmd.error", cmd + " -> " + t);
        }
    }

    private static void info() {
        AbstractPlayer p = AbstractDungeon.player;
        ProbeLog.log("cmd.info", ProbeLog.state() + " | " + ProbeLog.mods()
                + "\n    curseCardPool " + ProbeLog.ids(AbstractDungeon.curseCardPool)
                + "\n    masterDeck " + ProbeLog.ids(p == null ? null : p.masterDeck));
    }

    private static void purge() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            ProbeLog.log("cmd.purge", "player=null");
            return;
        }
        CardGroup purgeable = p.masterDeck.getPurgeableCards();
        ProbeLog.log("cmd.purge", "masterDeck " + ProbeLog.ids(p.masterDeck)
                + "\n    purgeable " + ProbeLog.ids(purgeable));
    }

    private static void cursepool() {
        ProbeLog.log("cmd.cursepool", ProbeLog.ids(AbstractDungeon.curseCardPool)
                + " srcCurseCardPool " + ProbeLog.ids(AbstractDungeon.srcCurseCardPool));
    }

    /** Lists the card IDs known to the game that contain the given text (any case). */
    private static void find(String text) {
        if (text == null || text.trim().isEmpty()) {
            ProbeLog.log("cmd.find", "usage: awprobe find <text>");
            return;
        }
        String needle = text.trim().toLowerCase(Locale.ROOT);
        List<String> hits = new ArrayList<String>();
        for (String id : CardLibrary.cards.keySet()) {
            if (id != null && id.toLowerCase(Locale.ROOT).contains(needle)) {
                hits.add(id);
            }
        }
        Collections.sort(hits);
        ProbeLog.log("cmd.find", "needle=" + text.trim() + " hits=" + hits.size() + " " + hits);
    }

    private static void addCard(String rawID) {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            ProbeLog.log("cmd.addcard", "player=null (start a run first)");
            return;
        }
        if (rawID == null) {
            ProbeLog.log("cmd.addcard", "usage: awprobe addcard <cardID>  (or 'awprobe bell')");
            return;
        }
        AbstractCard proto = prototypeFor(rawID.trim());
        if (proto == null) {
            ProbeLog.log("cmd.addcard", "unknown cardID: " + rawID
                    + " (try 'awprobe find <text>' or 'awprobe bell')"
                    + "\n    matching card IDs " + matchingIDs(rawID.trim())
                    + "\n    curseCardPool " + ProbeLog.ids(AbstractDungeon.curseCardPool));
            return;
        }
        AbstractCard copy = proto.makeCopy();
        p.masterDeck.addToTop(copy);
        ProbeLog.log("cmd.addcard", "added=" + copy.cardID + " masterDeck " + ProbeLog.ids(p.masterDeck));
    }

    private static void transform(String rawID) {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            ProbeLog.log("cmd.transform", "player=null (start a run first)");
            return;
        }
        if (rawID == null) {
            ProbeLog.log("cmd.transform", "usage: awprobe transform <cardID>  (bell = CurseOfTheBell)");
            return;
        }
        String resolved = resolveCardID(rawID.trim());
        if (resolved == null) {
            ProbeLog.log("cmd.transform", "unknown cardID: " + rawID
                    + " (try 'awprobe find <text>')"
                    + "\n    matching card IDs " + matchingIDs(rawID.trim()));
            return;
        }
        AbstractCard target = findInMasterDeck(p, resolved);
        if (target == null) {
            ProbeLog.log("cmd.transform", "card not found in masterDeck: " + resolved
                    + "\n    masterDeck " + ProbeLog.ids(p.masterDeck)
                    + "\n    add it first: awprobe addcard " + (BELL_ID.equals(resolved) ? "bell" : resolved));
            return;
        }
        ProbeLog.log("cmd.transform", "target=" + target.cardID + " type=" + target.type
                + " color=" + target.color + " rarity=" + target.rarity
                + " class=" + target.getClass().getName());
        AbstractDungeon.transformCard(target, true);
        AbstractCard result = AbstractDungeon.transformedCard;
        ProbeLog.log("cmd.transform", "transformedCard=" + (result == null ? "null" : result.cardID));
    }

    private static void shuffleDrawPile() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            ProbeLog.log("cmd.shuffledeck", "player=null");
            return;
        }
        ProbeLog.log("cmd.shuffledeck", "before " + ProbeLog.ids(p.drawPile));
        p.drawPile.shuffle();
        ProbeLog.log("cmd.shuffledeck", "after " + ProbeLog.ids(p.drawPile));
    }

    private static void usage() {
        ProbeLog.log("cmd.usage", "awprobe info|purge|cursepool|find <text>|addcard <cardID>|bell"
                + "|transform <cardID>|shuffledeck");
    }

    /**
     * Resolves a user-supplied card ID to an exact {@link CardLibrary} key.
     * Accepts the {@code bell} alias and any casing; returns null when nothing matches.
     */
    private static String resolveCardID(String rawID) {
        if (rawID == null) {
            return null;
        }
        String id = rawID.trim();
        if (id.isEmpty()) {
            return null;
        }
        if ("bell".equalsIgnoreCase(id) || BELL_ID.equalsIgnoreCase(id)) {
            return BELL_ID;
        }
        if (CardLibrary.getCard(id) != null) {
            return id;
        }
        for (String key : CardLibrary.cards.keySet()) {
            if (key != null && key.equalsIgnoreCase(id)) {
                return key;
            }
        }
        return null;
    }

    /** Library prototype for an ID; falls back to a direct {@link CurseOfTheBell} instance. */
    private static AbstractCard prototypeFor(String rawID) {
        String resolved = resolveCardID(rawID);
        if (resolved == null) {
            return null;
        }
        AbstractCard fromLibrary = CardLibrary.getCard(resolved);
        if (fromLibrary != null) {
            return fromLibrary;
        }
        if (BELL_ID.equals(resolved)) {
            return new CurseOfTheBell();
        }
        return null;
    }

    /** All library card IDs containing the text (case-insensitive), sorted, capped at 12 in the message. */
    private static String matchingIDs(String rawID) {
        if (rawID == null || rawID.trim().isEmpty()) {
            return "[]";
        }
        String needle = rawID.trim().toLowerCase(Locale.ROOT);
        List<String> hits = new ArrayList<String>();
        for (String id : CardLibrary.cards.keySet()) {
            if (id != null && id.toLowerCase(Locale.ROOT).contains(needle)) {
                hits.add(id);
            }
        }
        Collections.sort(hits);
        if (hits.isEmpty()) {
            return "[] (use 'awprobe find <text>' for a looser search)";
        }
        List<String> capped = hits.size() > 12 ? hits.subList(0, 12) : hits;
        return capped + (hits.size() > 12 ? " ... (" + hits.size() + " total)" : "");
    }

    private static AbstractCard findInMasterDeck(AbstractPlayer p, String cardID) {
        for (AbstractCard card : p.masterDeck.group) {
            if (cardID.equals(card.cardID)) {
                return card;
            }
        }
        return null;
    }

    /**
     * Returns token {@code offset} after the {@code awprobe} token, without changing case
     * (card IDs are case-sensitive). Returns null when absent.
     */
    private static String tokenAt(String[] tokens, int offset) {
        int base = indexOfCommand(tokens);
        if (base < 0) {
            return null;
        }
        int idx = base + offset;
        return (idx >= 0 && idx < tokens.length) ? tokens[idx] : null;
    }

    private static int indexOfCommand(String[] tokens) {
        if (tokens == null) {
            return -1;
        }
        for (int i = 0; i < tokens.length; i++) {
            if ("awprobe".equalsIgnoreCase(tokens[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * BaseMod auto-complete hook. Sub-command names are only offered while the
     * sub-command itself is being typed; the argument position offers card IDs for
     * {@code addcard} / {@code transform} instead (never sub-command names, which is
     * what made a partially typed {@code CurseOfTheBell} collapse into {@code cursepool}).
     */
    @Override
    protected ArrayList<String> extraOptions(String[] tokens, int depth) {
        ArrayList<String> out = new ArrayList<String>();
        int base = indexOfCommand(tokens);
        if (base < 0) {
            return out;
        }
        int consumed = tokens.length - base - 1;
        if (consumed <= 1) {
            out.addAll(Arrays.asList(SUBCOMMANDS));
            return out;
        }
        String sub = tokens[base + 1] == null ? "" : tokens[base + 1].toLowerCase(Locale.ROOT);
        String current = tokens[tokens.length - 1] == null ? "" : tokens[tokens.length - 1];
        if (current.isEmpty()) {
            // Do not auto-fill an empty argument: let the user type the intended ID.
            return out;
        }
        if ("addcard".equals(sub)) {
            out.addAll(ConsoleCommand.getCardOptions());
            out.add("bell");
        } else if ("transform".equals(sub)) {
            AbstractPlayer p = AbstractDungeon.player;
            if (p != null && p.masterDeck != null) {
                out.addAll(ConsoleCommand.getCardOptionsFromCardGroup(p.masterDeck));
                out.add("bell");
            }
        }
        return out;
    }
}
