package anthonyswrath.patches.vanilla;

import anthonyswrath.run.DefeatOnDraw;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

/**
 * Vanilla intercept for user story 3 (T022): judge "the curse entered the hand" at the two
 * entry points that cover every measured hand-entry channel (research §R-005, memory fact
 * f013).
 *
 * <ul>
 *   <li>{@code CardGroup.addToHand}: normal draws ({@code AbstractPlayer.draw}),
 *       temporary cards ({@code ShowCardAndAddToHandEffect}) and discard-to-hand retrieval
 *       ({@code MeditateAction});</li>
 *   <li>{@code CardGroup.addToTop} when the target group is the player's hand: draw-pile
 *       retrieval ({@code SkillFromDeckToHandAction}) and Retain
 *       ({@code RestoreRetainedCardsAction}).</li>
 * </ul>
 *
 * <p>Both are postfix handlers: the vanilla body has already inserted the card when the
 * judgment runs, which is what "the card entered the hand" means for FR-008, and the
 * defeat can therefore be settled without changing how the card was moved. The
 * {@code addToTop} judgment is restricted to the player's hand by identity because the
 * same method also moves cards into the draw pile, discard pile and other groups; that
 * filter is also what keeps Retain noise from mattering (only this mod's card ID is
 * judged).</p>
 *
 * <p>One patch class per intercepted vanilla class and one static inner class per
 * interception site (package convention in {@code package-info.java}). The actual decision
 * is in {@code run.DefeatOnDraw}; this class only routes hand entries to it, and it
 * produces no player-visible text.</p>
 */
public final class CardGroupHandEntryPatch {

    private CardGroupHandEntryPatch() {
        // patch holder
    }

    /**
     * {@code CardGroup.addToHand(AbstractCard)}: draws, temporary cards and discard-to-hand
     * retrieval all end here.
     */
    @SpirePatch(clz = CardGroup.class, method = "addToHand", paramtypez = {AbstractCard.class})
    public static class AddToHand {
        @SpirePostfixPatch
        public static void Postfix(CardGroup __instance, AbstractCard card) {
            if (isPlayerHand(__instance)) {
                DefeatOnDraw.onCurseEnteredHand(card);
            }
        }
    }

    /**
     * {@code CardGroup.addToTop(AbstractCard)}: only hand entries are relevant — draw-pile
     * retrieval and Retain come through here.
     */
    @SpirePatch(clz = CardGroup.class, method = "addToTop", paramtypez = {AbstractCard.class})
    public static class AddToTopHand {
        @SpirePostfixPatch
        public static void Postfix(CardGroup __instance, AbstractCard card) {
            if (isPlayerHand(__instance)) {
                DefeatOnDraw.onCurseEnteredHand(card);
            }
        }
    }

    /** Identity check, not a type check: other groups may share {@code CardGroupType}. */
    private static boolean isPlayerHand(CardGroup group) {
        AbstractPlayer player = AbstractDungeon.player;
        return player != null && group == player.hand;
    }
}
