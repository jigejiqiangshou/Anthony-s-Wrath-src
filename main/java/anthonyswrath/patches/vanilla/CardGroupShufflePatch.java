package anthonyswrath.patches.vanilla;

import anthonyswrath.run.DrawPileOrdering;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.random.Random;

/**
 * Vanilla intercept for user story 2 (T019): puts this mod's curse at the bottom of the pile
 * that has just been shuffled (research §R-004, FR-006 / FR-007).
 *
 * <p>{@code CardGroup} has two independent shuffle primitives — {@code shuffle()} and
 * {@code shuffle(Random)} — and neither delegates to the other (verified against the shipped
 * bytecode, {@code memory/facts/f010}), so both need a postfix. Postfix is the right side:
 * the vanilla body has already fixed the order at that point, and moving the curse then does
 * not depend on how the pile is consumed afterwards. Both battle-start first shuffle
 * ({@code initializeDeck} on a temporary {@code DRAW_PILE} group) and the exhausted-pile
 * reshuffle ({@code EmptyDeckShuffleAction} on {@code discardPile}) go through these two
 * methods, as do card / relic triggered shuffles (for example {@code ShuffleAction}), so the
 * primitives are the common convergence point for every shuffle path.</p>
 *
 * <p>One patch class per intercepted vanilla class, one static inner class per interception
 * site (package convention in {@code package-info.java}). Only this mod's card ID is inspected
 * ({@link DrawPileOrdering}), no other card's behaviour changes, and no player-visible text is
 * produced here.</p>
 */
public final class CardGroupShufflePatch {

    private CardGroupShufflePatch() {
        // patch holder
    }

    /** {@code CardGroup.shuffle()}: no-argument primitive (e.g. {@code ShuffleAction}). */
    @SpirePatch(clz = CardGroup.class, method = "shuffle", paramtypez = {})
    public static class ShuffleNoArg {
        @SpirePostfixPatch
        public static void Postfix(CardGroup __instance) {
            DrawPileOrdering.sinkCurseToBottom(__instance);
        }
    }

    /**
     * {@code CardGroup.shuffle(Random)}: primitive used by the battle-start first shuffle and
     * by the exhausted-draw-pile reshuffle (both pass {@code AbstractDungeon.shuffleRng}).
     */
    @SpirePatch(clz = CardGroup.class, method = "shuffle", paramtypez = {Random.class})
    public static class ShuffleWithRng {
        @SpirePostfixPatch
        public static void Postfix(CardGroup __instance, Random rng) {
            DrawPileOrdering.sinkCurseToBottom(__instance);
        }
    }
}
