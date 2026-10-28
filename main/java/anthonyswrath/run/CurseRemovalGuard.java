package anthonyswrath.run;

import anthonyswrath.cards.AnthonysWrathCurse;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps this mod's curse in the master deck across the vanilla bulk-removal loop of the
 * "Fountain of Cleansing" event (T030; FR-012, EC-009).
 *
 * <h2>Why a bracket instead of a loop patch</h2>
 *
 * <p>{@code FountainOfCurseRemoval.buttonEffect(int)} does not use
 * {@code getPurgeableCards()}: it walks {@code player.masterDeck.group} itself, skips the
 * three hard-coded reference curses (and bottled cards), and for every other curse it queues
 * a {@code PurgeCardEffect}, records a metric entry and calls {@code masterDeck.removeCard}.
 * {@code CardGroupGetPurgeableCardsPatch} therefore cannot cover this event. Patching the
 * loop itself would require re-implementing the method, so instead the curse is temporarily
 * taken out of the master deck for the duration of the call ({@link #suspendFromMasterDeck()}
 * → vanilla loop → {@link #restoreToMasterDeck()}). The loop then sees exactly the same deck
 * the reference card sees — the curse is not purged, not shown as purged and not recorded in
 * the event metric — and the card is put back at its original position afterwards.</p>
 *
 * <p>The bracket is safe because the event method does not read the master deck before the
 * loop (the intro screen is a separate method) and because the card object is never
 * destroyed: only the deck list is touched, then restored. Several copies are handled in
 * original-index order so their positions stay stable.</p>
 *
 * <p>Only this mod's card id is inspected; every other curse keeps the vanilla behaviour
 * (FR-012).</p>
 */
public final class CurseRemovalGuard {

    /** Cards taken out of the master deck, ascending original index (parallel to indices). */
    private static final List<AbstractCard> SUSPENDED_CARDS = new ArrayList<AbstractCard>();

    /** Original master-deck indices of {@link #SUSPENDED_CARDS}, ascending. */
    private static final List<Integer> SUSPENDED_INDICES = new ArrayList<Integer>();

    private static boolean suspended;

    private CurseRemovalGuard() {
        // utility class
    }

    /**
     * Removes every copy of this mod's curse from the player's master deck and remembers the
     * original positions. Does nothing when no player / deck exists or when a suspension is
     * already active (defensive: the bracket must be balanced).
     */
    public static void suspendFromMasterDeck() {
        if (suspended) {
            return;
        }
        List<AbstractCard> masterDeck = masterDeckGroup();
        if (masterDeck == null) {
            return;
        }
        SUSPENDED_CARDS.clear();
        SUSPENDED_INDICES.clear();
        for (int i = masterDeck.size() - 1; i >= 0; i--) {
            AbstractCard card = masterDeck.get(i);
            if (card != null && AnthonysWrathCurse.CARD_ID.equals(card.cardID)) {
                // Walk from the end, prepend: both lists stay in ascending index order.
                SUSPENDED_CARDS.add(0, masterDeck.remove(i));
                SUSPENDED_INDICES.add(0, Integer.valueOf(i));
            }
        }
        suspended = !SUSPENDED_CARDS.isEmpty();
    }

    /**
     * Puts the suspended copies back into the master deck at their original positions. Does
     * nothing when no suspension is active. The bookkeeping is always cleared, so a
     * restoration can never reuse stale data.
     */
    public static void restoreToMasterDeck() {
        if (!suspended) {
            return;
        }
        suspended = false;
        try {
            List<AbstractCard> masterDeck = masterDeckGroup();
            if (masterDeck == null) {
                return;
            }
            for (int i = 0; i < SUSPENDED_CARDS.size(); i++) {
                int originalIndex = SUSPENDED_INDICES.get(i).intValue();
                int insertAt = Math.min(originalIndex, masterDeck.size());
                masterDeck.add(insertAt, SUSPENDED_CARDS.get(i));
            }
        } finally {
            SUSPENDED_CARDS.clear();
            SUSPENDED_INDICES.clear();
        }
    }

    /** The mutable card list of the player's master deck, or {@code null} when absent. */
    private static List<AbstractCard> masterDeckGroup() {
        AbstractPlayer player = AbstractDungeon.player;
        if (player == null || player.masterDeck == null || player.masterDeck.group == null) {
            return null;
        }
        return player.masterDeck.group;
    }
}
