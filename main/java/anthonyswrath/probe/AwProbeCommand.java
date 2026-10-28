package anthonyswrath.probe;

import anthonyswrath.cards.AnthonysWrathCurse;
import anthonyswrath.run.DrawPileOrdering;
import anthonyswrath.run.RunState;
import basemod.devcommands.ConsoleCommand;
import com.megacrit.cardcrawl.actions.common.ShuffleAction;
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
 * awprobe purgetest        US5/T034 self-check: the curse is in the deck but not purgeable
 * awprobe curseprobe [n]   US4/T028 self-check: sample CardLibrary.getCurse (both overloads,
 *                          the vanilla random-curse source), n default 50
 * awprobe cursepool        dump AbstractDungeon.curseCardPool
 * awprobe find &lt;text&gt;     list card IDs containing the text (any case)
 * awprobe addcard &lt;id&gt;    add a copy of the card to masterDeck ({@code bell} = CurseOfTheBell)
 * awprobe bell             shorthand for {@code addcard bell} (no argument to get wrong)
 * awprobe transform &lt;id&gt;  call AbstractDungeon.transformCard(card, true) and log the result
 * awprobe shuffledeck      call player.drawPile.shuffle() (exercises the shuffle patches)
 * awprobe shuffleaction    queue the vanilla {@code ShuffleAction(drawPile, true)} used by the
 *                          cards Reboot / DeepBreath (card-triggered, non-exhausted shuffle)
 * awprobe drawpile         dump drawPile / discardPile (index 0 = bottom), no shuffle
 * awprobe sinktest         deterministic self-check of DrawPileOrdering (US2 / FR-007)
 * awprobe defeatoff        T032 development-only: keep the curse in the hand (no defeat)
 * awprobe defeaton         T032 development-only: restore the normal defeat behaviour
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
            "info", "purge", "purgetest", "curseprobe", "cursepool", "find", "addcard", "bell",
            "transform", "shuffledeck", "shuffleaction", "drawpile", "sinktest",
            "defeatoff", "defeaton"
    };

    /** Default / maximum sample counts of {@code awprobe curseprobe}. */
    private static final int CURSE_PROBE_DEFAULT = 50;
    private static final int CURSE_PROBE_MAX = 500;

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
            } else if ("purgetest".equals(cmd)) {
                purgeTest();
            } else if ("curseprobe".equals(cmd)) {
                curseProbe(arg);
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
            } else if ("shuffleaction".equals(cmd)) {
                queueShuffleAction();
            } else if ("drawpile".equals(cmd)) {
                drawPile();
            } else if ("sinktest".equals(cmd)) {
                sinkTest();
            } else if ("defeatoff".equals(cmd)) {
                setDefeatSuspended(true);
            } else if ("defeaton".equals(cmd)) {
                setDefeatSuspended(false);
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
                + "\n    drawPile " + ProbeLog.ids(p == null ? null : p.drawPile)
                + " (index 0 = bottom, last index = next drawn)"
                + "\n    discardPile " + ProbeLog.ids(p == null ? null : p.discardPile)
                + "\n    masterDeck " + ProbeLog.ids(p == null ? null : p.masterDeck));
    }

    /** Dumps the two piles US2 cares about without shuffling anything (T020 evidence). */
    private static void drawPile() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            ProbeLog.log("cmd.drawpile", "player=null");
            return;
        }
        ProbeLog.log("cmd.drawpile", "drawPile " + ProbeLog.ids(p.drawPile)
                + " (index 0 = bottom, last index = next drawn)"
                + "\n    discardPile " + ProbeLog.ids(p.discardPile));
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

    /**
     * US5 / T034 self-check: the curse is in the master deck but never in the candidate list
     * built by {@code CardGroup.getPurgeableCards()} (T029 / FR-012).
     *
     * <p>This is the deterministic command-level evidence; QS-11 additionally checks the
     * shop / rest-site / Fountain of Cleansing user interfaces.</p>
     */
    private static void purgeTest() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            ProbeLog.log("cmd.purgetest", "player=null (start a run first)");
            return;
        }
        CardGroup purgeable = p.masterDeck.getPurgeableCards();
        boolean inDeck = findInMasterDeck(p, AnthonysWrathCurse.CARD_ID) != null;
        boolean inPurgeable = groupContains(purgeable, AnthonysWrathCurse.CARD_ID);
        if (!inDeck) {
            ProbeLog.log("cmd.purgetest", "SKIP no " + AnthonysWrathCurse.CARD_ID
                    + " in masterDeck (add it with 'awprobe addcard anthonyswrath:AnthonysWrath')"
                    + "\n    masterDeck " + ProbeLog.ids(p.masterDeck)
                    + "\n    purgeable " + ProbeLog.ids(purgeable));
            return;
        }
        boolean pass = !inPurgeable;
        ProbeLog.log("cmd.purgetest", (pass ? "PASS" : "FAIL")
                + " inDeck=" + inDeck + " inPurgeable=" + inPurgeable
                + "\n    masterDeck " + ProbeLog.ids(p.masterDeck)
                + "\n    purgeable " + ProbeLog.ids(purgeable));
    }

    /**
     * US4 / T028 self-check: the vanilla random-curse source never returns this mod's curse
     * (T027; FR-011, EC-010, SC-004).
     *
     * <p>Both {@code CardLibrary.getCurse()} overloads are sampled — the no-argument one used
     * by {@code AbstractDungeon.returnRandomCurse()} (Cursed Key, cursed chests, events) and
     * the {@code (avoid, rng)} one used by the curse branch of
     * {@code AbstractDungeon.transformCard(...)}. A non-zero hit count means the
     * {@code CardLibraryGetCursePatch} filter is not active and EC-010 is violated.</p>
     */
    private static void curseProbe(String rawArg) {
        int samples = parseSampleCount(rawArg);
        AbstractCard avoid = prototypeFor(BELL_ID);
        if (avoid == null) {
            avoid = new CurseOfTheBell();
        }
        int noArgHits = 0;
        int avoidHits = 0;
        List<String> seen = new ArrayList<String>();
        for (int i = 0; i < samples; i++) {
            AbstractCard plain = CardLibrary.getCurse();
            if (plain != null && AnthonysWrathCurse.CARD_ID.equals(plain.cardID)) {
                noArgHits++;
            }
            AbstractCard transformed = CardLibrary.getCurse(avoid, AbstractDungeon.cardRandomRng);
            if (transformed != null && AnthonysWrathCurse.CARD_ID.equals(transformed.cardID)) {
                avoidHits++;
            }
            if (seen.size() < 6 && plain != null && !seen.contains(plain.cardID)) {
                seen.add(plain.cardID);
            }
        }
        boolean poolClean = !groupContains(AbstractDungeon.curseCardPool, AnthonysWrathCurse.CARD_ID)
                && !groupContains(AbstractDungeon.srcCurseCardPool, AnthonysWrathCurse.CARD_ID);
        boolean pass = noArgHits == 0 && avoidHits == 0 && poolClean;
        ProbeLog.log("cmd.curseprobe", (pass ? "PASS" : "FAIL")
                + " samples=" + samples + " noArgHits=" + noArgHits + " avoidHits=" + avoidHits
                + " poolsClean=" + poolClean
                + "\n    sampleIDs=" + seen
                + "\n    curseCardPool " + ProbeLog.ids(AbstractDungeon.curseCardPool)
                + "\n    srcCurseCardPool " + ProbeLog.ids(AbstractDungeon.srcCurseCardPool));
    }

    /** T032 development-only FR-005 comparison switch (removed together with the probe). */
    private static void setDefeatSuspended(boolean suspended) {
        RunState.setDefeatSuspendedForProbe(suspended);
        ProbeLog.log(suspended ? "cmd.defeatoff" : "cmd.defeaton",
                "defeatSuspended=" + RunState.isDefeatSuspendedForProbe()
                        + " (development only; T035 removes this switch)");
    }

    private static int parseSampleCount(String rawArg) {
        if (rawArg == null || rawArg.trim().isEmpty()) {
            return CURSE_PROBE_DEFAULT;
        }
        try {
            int value = Integer.parseInt(rawArg.trim());
            if (value < 1) {
                return 1;
            }
            return Math.min(value, CURSE_PROBE_MAX);
        } catch (NumberFormatException ignored) {
            return CURSE_PROBE_DEFAULT;
        }
    }

    private static boolean groupContains(CardGroup group, String cardID) {
        if (group == null || group.group == null) {
            return false;
        }
        for (AbstractCard card : group.group) {
            if (card != null && cardID.equals(card.cardID)) {
                return true;
            }
        }
        return false;
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

    /**
     * Queues the vanilla card-triggered shuffle action on the draw pile (T020 / QS-04).
     *
     * <p>{@code ShuffleAction} is the action the cards {@code Reboot} and {@code DeepBreath}
     * add; queueing it from the console exercises the real card-triggered, non-exhausted
     * shuffle path (which calls the no-argument {@code CardGroup.shuffle()} primitive) without
     * depending on having those cards in the deck.</p>
     */
    private static void queueShuffleAction() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            ProbeLog.log("cmd.shuffleaction", "player=null");
            return;
        }
        ProbeLog.log("cmd.shuffleaction", "before drawPile " + ProbeLog.ids(p.drawPile));
        AbstractDungeon.actionManager.addToTop(new ShuffleAction(p.drawPile, true));
        ProbeLog.log("cmd.shuffleaction", "queued ShuffleAction(drawPile, triggerRelics=true)"
                + " (same path as Reboot / DeepBreath)");
    }

    /**
     * Deterministic in-game self-check for {@link DrawPileOrdering} (T018, FR-007).
     *
     * <p>The real shuffle is random, so this exercises the ordering rule on a synthetic
     * {@code DRAW_PILE}-typed group instead: two curse copies interleaved with filler cards
     * must become one contiguous block at indices {@code 0..1} in their previous relative
     * order; the second call must be idempotent; a group without the curse and a
     * non-pile group containing the curse must be reported as not handled and stay
     * unchanged (EC-003 and the "do not reorder other groups" rule).</p>
     */
    private static void sinkTest() {
        AbstractCard curseProto = CardLibrary.getCard(AnthonysWrathCurse.CARD_ID);
        if (curseProto == null) {
            curseProto = new AnthonysWrathCurse();
        }
        AbstractCard fillerProto = CardLibrary.getCard("Strike_R");
        if (fillerProto == null) {
            ProbeLog.log("cmd.sinktest", "SKIP no filler prototype in CardLibrary");
            return;
        }
        int pass = 0;
        int total = 0;

        // Case 1: two copies interleaved -> contiguous block at the low-index end, order kept.
        CardGroup pile = new CardGroup(CardGroup.CardGroupType.DRAW_PILE);
        AbstractCard fillerBefore = fillerProto.makeCopy();
        AbstractCard firstCopy = curseProto.makeCopy();
        AbstractCard fillerMiddle = fillerProto.makeCopy();
        AbstractCard secondCopy = curseProto.makeCopy();
        AbstractCard fillerAfter = fillerProto.makeCopy();
        pile.group.add(fillerBefore);
        pile.group.add(firstCopy);
        pile.group.add(fillerMiddle);
        pile.group.add(secondCopy);
        pile.group.add(fillerAfter);
        total++;
        boolean moved = DrawPileOrdering.sinkCurseToBottom(pile);
        boolean grouped = moved && pile.group.size() == 5
                && pile.group.get(0) == firstCopy
                && pile.group.get(1) == secondCopy
                && pile.group.get(2) == fillerBefore
                && pile.group.get(3) == fillerMiddle
                && pile.group.get(4) == fillerAfter;
        pass += grouped ? 1 : 0;
        ProbeLog.log("cmd.sinktest", "case=grouped " + verdict(grouped)
                + " moved=" + moved + " block=" + uuidPrefix(pile, 0) + "," + uuidPrefix(pile, 1)
                + " expectedBlock=" + shortUuid(firstCopy) + "," + shortUuid(secondCopy)
                + " result=" + ProbeLog.ids(pile));

        // Case 2: calling again changes nothing (the block already is at the bottom).
        total++;
        boolean groupedAgain = DrawPileOrdering.sinkCurseToBottom(pile);
        boolean idempotent = groupedAgain && pile.group.size() == 5
                && pile.group.get(0) == firstCopy
                && pile.group.get(1) == secondCopy
                && pile.group.get(2) == fillerBefore;
        pass += idempotent ? 1 : 0;
        ProbeLog.log("cmd.sinktest", "case=idempotent " + verdict(idempotent)
                + " moved=" + groupedAgain + " result=" + ProbeLog.ids(pile));

        // Case 3: no curse in the group -> not handled, order untouched (EC-003).
        CardGroup plain = new CardGroup(CardGroup.CardGroupType.DRAW_PILE);
        AbstractCard plainOne = fillerProto.makeCopy();
        AbstractCard plainTwo = fillerProto.makeCopy();
        plain.group.add(plainOne);
        plain.group.add(plainTwo);
        total++;
        boolean movedPlain = DrawPileOrdering.sinkCurseToBottom(plain);
        boolean untouched = !movedPlain && plain.group.size() == 2
                && plain.group.get(0) == plainOne && plain.group.get(1) == plainTwo;
        pass += untouched ? 1 : 0;
        ProbeLog.log("cmd.sinktest", "case=absent " + verdict(untouched)
                + " moved=" + movedPlain + " result=" + ProbeLog.ids(plain));

        // Case 4: a non-pile (scratch) group is never reordered, even when it contains the curse.
        CardGroup scratch = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        AbstractCard scratchFill = fillerProto.makeCopy();
        AbstractCard scratchCurse = curseProto.makeCopy();
        scratch.group.add(scratchFill);
        scratch.group.add(scratchCurse);
        total++;
        boolean movedScratch = DrawPileOrdering.sinkCurseToBottom(scratch);
        boolean scratchUntouched = !movedScratch && scratch.group.size() == 2
                && scratch.group.get(0) == scratchFill && scratch.group.get(1) == scratchCurse;
        pass += scratchUntouched ? 1 : 0;
        ProbeLog.log("cmd.sinktest", "case=scratch-group-skipped " + verdict(scratchUntouched)
                + " moved=" + movedScratch + " result=" + ProbeLog.ids(scratch));

        ProbeLog.log("cmd.sinktest", "summary pass=" + pass + "/" + total);
    }

    private static String verdict(boolean ok) {
        return ok ? "PASS" : "FAIL";
    }

    private static String uuidPrefix(CardGroup group, int index) {
        return index < group.group.size() ? shortUuid(group.group.get(index)) : "missing";
    }

    private static String shortUuid(AbstractCard card) {
        String uuid = card.uuid == null ? "null" : card.uuid.toString();
        return uuid.length() <= 8 ? uuid : uuid.substring(0, 8);
    }

    private static void usage() {
        ProbeLog.log("cmd.usage", "awprobe info|purge|purgetest|curseprobe [n]|cursepool|find <text>"
                + "|addcard <cardID>|bell|transform <cardID>|shuffledeck|shuffleaction|drawpile"
                + "|sinktest|defeatoff|defeaton");
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
