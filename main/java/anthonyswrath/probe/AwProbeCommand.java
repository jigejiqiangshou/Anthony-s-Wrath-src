package anthonyswrath.probe;

import basemod.devcommands.ConsoleCommand;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;

/**
 * T004 dev-console command {@code awprobe} (development only, removed by T035).
 *
 * <p>Usage inside the game console:</p>
 * <pre>
 * awprobe info             dump floor/room/piles/custom mods
 * awprobe purge            call masterDeck.getPurgeableCards() and log the result
 * awprobe cursepool        dump AbstractDungeon.curseCardPool
 * awprobe transform &lt;id&gt;  call AbstractDungeon.transformCard(card, true) and log the result
 * awprobe addcard &lt;id&gt;    add a copy of the card to masterDeck (e.g. CurseOfTheBell)
 * awprobe shuffledeck      call player.drawPile.shuffle() (exercises the shuffle patches)
 * </pre>
 */
public class AwProbeCommand extends ConsoleCommand {

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
        String cmd = sub == null ? "info" : sub.toLowerCase();
        try {
            if ("info".equals(cmd)) {
                ProbeLog.log("cmd.info", ProbeLog.state() + " | " + ProbeLog.mods()
                        + "\n    curseCardPool " + ProbeLog.ids(AbstractDungeon.curseCardPool));
            } else if ("purge".equals(cmd)) {
                purge();
            } else if ("cursepool".equals(cmd)) {
                ProbeLog.log("cmd.cursepool", ProbeLog.ids(AbstractDungeon.curseCardPool)
                        + " srcCurseCardPool " + ProbeLog.ids(AbstractDungeon.srcCurseCardPool));
            } else if ("transform".equals(cmd)) {
                transform(arg);
            } else if ("addcard".equals(cmd)) {
                addCard(arg);
            } else if ("shuffledeck".equals(cmd)) {
                shuffleDrawPile();
            } else {
                ProbeLog.log("cmd.usage", "awprobe info|purge|cursepool|transform <cardID>|addcard <cardID>|shuffledeck");
            }
        } catch (Throwable t) {
            ProbeLog.log("cmd.error", cmd + " -> " + t);
        }
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

    private static void transform(String cardID) {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            ProbeLog.log("cmd.transform", "player=null");
            return;
        }
        if (cardID == null) {
            ProbeLog.log("cmd.usage", "awprobe transform <cardID>");
            return;
        }
        AbstractCard target = findInMasterDeck(p, cardID);
        if (target == null) {
            ProbeLog.log("cmd.transform", "card not found in masterDeck: " + cardID);
            return;
        }
        ProbeLog.log("cmd.transform", "target=" + target.cardID + " type=" + target.type
                + " color=" + target.color + " class=" + target.getClass().getName());
        AbstractDungeon.transformCard(target, true);
        AbstractCard result = AbstractDungeon.transformedCard;
        ProbeLog.log("cmd.transform", "transformedCard=" + (result == null ? "null" : result.cardID));
    }

    private static void addCard(String cardID) {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            ProbeLog.log("cmd.addcard", "player=null (start a run first)");
            return;
        }
        if (cardID == null) {
            ProbeLog.log("cmd.usage", "awprobe addcard <cardID>");
            return;
        }
        AbstractCard proto = CardLibrary.getCard(cardID);
        if (proto == null) {
            ProbeLog.log("cmd.addcard", "unknown cardID: " + cardID);
            return;
        }
        AbstractCard copy = proto.makeCopy();
        p.masterDeck.addToTop(copy);
        ProbeLog.log("cmd.addcard", "added=" + copy.cardID + " masterDeck " + ProbeLog.ids(p.masterDeck));
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
        if (tokens == null) {
            return null;
        }
        for (int i = 0; i < tokens.length; i++) {
            if ("awprobe".equalsIgnoreCase(tokens[i])) {
                int idx = i + offset;
                return (idx >= 0 && idx < tokens.length) ? tokens[idx] : null;
            }
        }
        return null;
    }

    @Override
    protected java.util.ArrayList<String> extraOptions(String[] tokens, int depth) {
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        out.add("info");
        out.add("purge");
        out.add("cursepool");
        out.add("transform");
        out.add("addcard");
        out.add("shuffledeck");
        return out;
    }
}
