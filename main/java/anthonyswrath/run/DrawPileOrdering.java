package anthonyswrath.run;

import anthonyswrath.cards.AnthonysWrathCurse;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.CardGroup.CardGroupType;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import java.util.ArrayList;
import java.util.List;

/**
 * Bottom-of-deck ordering for the「安东尼之怒」curse (T016 / T018, FR-006 / FR-007).
 *
 * <p>Research §R-004: the only place that can see <em>every</em> shuffle is the shuffle
 * primitive itself, so the ordering is applied as a postfix on
 * {@code CardGroup.shuffle()} and {@code CardGroup.shuffle(Random)} (see
 * {@code patches.vanilla.CardGroupShufflePatch}, T019). This class holds the part that does
 * not depend on how the card got shuffled: after the shuffle has fixed the order, move every
 * copy of this mod's curse to the low-index end of the shuffled group.</p>
 *
 * <h2>Why the low-index end is the bottom of the draw pile</h2>
 *
 * <p>The draw pile is a {@link CardGroup} whose {@code getTopCard()} is
 * {@code group.get(size - 1)} (verified by the T004 probe, {@code memory/facts/f010}), so
 * index 0 is the card that is drawn <em>last</em> — the visible bottom of the pile. Both
 * routes by which a shuffled group becomes the draw pile preserve the group's index order
 * (verified against the shipped bytecode, 2026-10-24):</p>
 * <ul>
 *   <li>battle start: {@code CardGroup.initializeDeck} shuffles a temporary group of type
 *       {@code DRAW_PILE} and then walks it from index 0 to the end, calling
 *       {@code drawPile.addToTop(card)} for each card — positions are copied one-to-one;</li>
 *   <li>exhausted draw pile: {@code EmptyDeckShuffleAction} shuffles {@code discardPile},
 *       then its {@code SoulGroup} walk also runs from index 0 to the end and
 *       {@code Soul.shuffle} calls {@code drawPile.addToTop(card)} — positions are copied
 *       one-to-one again.</li>
 * </ul>
 * <p>Moving the curse to index 0 of the shuffled group therefore places it at the bottom of
 * the draw pile on both paths. On a direct {@code drawPile.shuffle()} the group itself is
 * the draw pile, so index 0 is the bottom by definition.</p>
 *
 * <h2>Multi-copy rule (FR-007, T018)</h2>
 *
 * <p>All copies form one contiguous block at indices {@code 0 .. k-1}. Their relative order
 * is the relative order the shuffle produced for them (the block is rebuilt by walking the
 * shuffled list from its end, so the copies keep their ascending post-shuffle order). A group
 * without the card is left untouched and reported as not handled (EC-003: no extra checks
 * when the card is absent).</p>
 *
 * <p>Only draw piles and discard piles are considered: the shuffle primitive is shared with
 * other groups (master deck, card pools, ...) and the feature must not reorder anything
 * outside the two combat piles (FR-006 scope, research §R-004). Battle start is covered by
 * the temporary {@code DRAW_PILE}-typed group built in {@code initializeDeck}.</p>
 *
 * <h2>Unselected runs (T026 / FR-011)</h2>
 *
 * <p>The rule only applies when the option was selected for this run: {@link RunState}
 * captures that once per run at {@code StartGame} and every component consults it before
 * acting, so an unselected run is not touched even if a copy of the card somehow reached a
 * pile. The presence check stays as well — a selected run whose copy was transformed away
 * is left alone (EC-003).</p>
 */
public final class DrawPileOrdering {

    private DrawPileOrdering() {
        // utility class
    }

    /**
     * Moves every copy of {@link AnthonysWrathCurse#CARD_ID} in {@code group} to the
     * low-index end, keeping the copies' relative order (FR-006 / FR-007).
     *
     * @param group the group that has just been shuffled
     * @return {@code true} when at least one copy was moved; {@code false} when the group is
     *         not a combat pile or does not contain the card (EC-003 — caller needs no
     *         follow-up in that case)
     */
    public static boolean sinkCurseToBottom(CardGroup group) {
        if (!RunState.isOptionSelected()) {
            // FR-011 / T026: the feature never intervenes in an unselected run.
            return false;
        }
        if (group == null || group.group == null || group.group.isEmpty()) {
            return false;
        }
        if (!isCombatPile(group)) {
            return false;
        }
        List<AbstractCard> copies = new ArrayList<AbstractCard>();
        for (int i = group.group.size() - 1; i >= 0; i--) {
            AbstractCard card = group.group.get(i);
            if (card != null && AnthonysWrathCurse.CARD_ID.equals(card.cardID)) {
                // Removing from the end and prepending keeps the copies in ascending
                // post-shuffle order inside the block.
                copies.add(0, group.group.remove(i));
            }
        }
        if (copies.isEmpty()) {
            return false;
        }
        group.group.addAll(0, copies);
        return true;
    }

    /**
     * Returns whether {@code group} is one of the piles this feature may reorder: the draw
     * pile or the discard pile. The type check catches the temporary {@code DRAW_PILE} group
     * built by {@code initializeDeck} at battle start (it is not {@code player.drawPile} yet);
     * the identity check is a safety net should a future game version leave the type
     * unspecified on one of the player's piles.
     */
    private static boolean isCombatPile(CardGroup group) {
        if (group.type == CardGroupType.DRAW_PILE || group.type == CardGroupType.DISCARD_PILE) {
            return true;
        }
        AbstractPlayer player = AbstractDungeon.player;
        return player != null && (group == player.drawPile || group == player.discardPile);
    }
}
