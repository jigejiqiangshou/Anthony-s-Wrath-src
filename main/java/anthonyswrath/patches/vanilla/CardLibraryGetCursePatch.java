package anthonyswrath.patches.vanilla;

import anthonyswrath.cards.AnthonysWrathCurse;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.random.Random;

/**
 * Vanilla intercept for user story 4 (T027): no random curse source may produce this mod's
 * curse (FR-011, EC-010, SC-004).
 *
 * <h2>Why the card pools are not the place to intercept</h2>
 *
 * <p>The T027 task originally planned to remove the card from
 * {@code AbstractDungeon.curseCardPool} in the {@code StartGame} hook (research §R-007). The
 * V2.3.4 bytecode shows that this would not be enough: every vanilla "give the player a
 * random curse" path — {@code AbstractDungeon.returnRandomCurse()},
 * {@code returnTrulyRandomCardFromAvailable(...)}, and the curse branch of
 * {@code AbstractDungeon.transformCard(...)} — obtains the card through
 * {@code CardLibrary.getCurse()}, which iterates the card library's <em>private</em>
 * {@code curses} map and hard-codes four excluded reference cards. {@code curseCardPool} is
 * only a copy of that set and is read by {@code getCard(CardRarity.CURSE)} /
 * {@code getCardFromPool(...)}; it is not involved in {@code returnRandomCurse()}. Both
 * layers are therefore covered: the class carries BaseMod's {@code @NoPools} annotation
 * (pools), and this patch filters the two {@code getCurse} overloads (the actual source).</p>
 *
 * <h2>Why {@code @SpirePatch2} and not {@code @SpirePatch}</h2>
 *
 * <p>The filter has to <em>replace</em> the return value. In ModTheSpire 3.30.3 that is only
 * possible for patches declared with {@code @SpirePatch2}: its param mapper
 * ({@code ParamInfo2} / {@code PostfixParamInfo2}) recognises the special parameter name
 * {@code __result} and, when the handler returns a value, rewrites the target method to
 * return the handler's result ({@code return ($r)handler(...)}). The legacy {@code @SpirePatch}
 * path maps parameters by position and silently ignores a non-void postfix return value.
 * A consequence of the {@code @SpirePatch2} mapper is that handler parameters are matched by
 * <b>name</b> against the target method's local-variable names, so the handler below uses the
 * target's parameter names ({@code prohibitedCard}, {@code rng}) instead of descriptive ones.</p>
 *
 * <h2>How the filter works</h2>
 *
 * <p>When the vanilla draw happens to pick this mod's card, the postfix draws again from the
 * same vanilla method, which keeps the vanilla RNG call and exclusion handling untouched for
 * every other curse. A re-entrancy flag makes the inner call return its raw result, and the
 * loop is bounded, so the patch can never recurse or hang: with 10 eligible vanilla curses an
 * inner result is already acceptable in the overwhelming majority of cases.</p>
 *
 * <p>Both overloads must be patched: {@code getCurse()} is the source used by
 * {@code returnRandomCurse()} (Cursed Key, cursed chests, events), while
 * {@code getCurse(AbstractCard prohibitedCard, Random rng)} is the source used by
 * {@code AbstractDungeon.transformCard(...)} for cursing the transformed card, which also
 * must never turn anything into this curse (EC-003: the transformed result of the reference
 * card is always a <em>different</em> curse). The one-argument
 * {@code getCurse(AbstractCard)} overload delegates to the two-argument one, so it is covered
 * transitively.</p>
 *
 * <p>Only this mod's card id is inspected; the distribution and the behaviour of every
 * other curse are unchanged.</p>
 */
public final class CardLibraryGetCursePatch {

    /** Upper bound for the re-draw loop; unreachable in practice (10 vanilla curses). */
    private static final int MAX_REDRAWS = 64;

    private CardLibraryGetCursePatch() {
        // patch holder
    }

    /**
     * {@code CardLibrary.getCurse()}: the source of {@code AbstractDungeon.returnRandomCurse()}
     * and of every "gain a random curse" reward / relic / event effect.
     */
    @SpirePatch2(clz = CardLibrary.class, method = "getCurse", paramtypez = {})
    public static class GetCurseNoArg {

        /** Re-entrancy guard: an inner call returns its raw result to the outer loop. */
        private static boolean redrawing;

        @SpirePostfixPatch
        public static AbstractCard Postfix(AbstractCard __result) {
            if (redrawing || !isThisModsCurse(__result)) {
                return __result;
            }
            redrawing = true;
            try {
                AbstractCard result = __result;
                for (int i = 0; i < MAX_REDRAWS && isThisModsCurse(result); i++) {
                    result = CardLibrary.getCurse();
                }
                return result;
            } finally {
                redrawing = false;
            }
        }
    }

    /**
     * {@code CardLibrary.getCurse(AbstractCard prohibitedCard, Random rng)}: the source of the
     * curse branch of {@code AbstractDungeon.transformCard(...)}; the result must never be
     * this mod's curse either (EC-003, same as the reference card).
     */
    @SpirePatch2(clz = CardLibrary.class, method = "getCurse",
            paramtypez = {AbstractCard.class, Random.class})
    public static class GetCurseAvoidAndRng {

        /** Re-entrancy guard: an inner call returns its raw result to the outer loop. */
        private static boolean redrawing;

        @SpirePostfixPatch
        public static AbstractCard Postfix(AbstractCard __result, AbstractCard prohibitedCard,
                                           Random rng) {
            if (redrawing || !isThisModsCurse(__result)) {
                return __result;
            }
            redrawing = true;
            try {
                AbstractCard result = __result;
                for (int i = 0; i < MAX_REDRAWS && isThisModsCurse(result); i++) {
                    result = CardLibrary.getCurse(prohibitedCard, rng);
                }
                return result;
            } finally {
                redrawing = false;
            }
        }
    }

    private static boolean isThisModsCurse(AbstractCard card) {
        return card != null && AnthonysWrathCurse.CARD_ID.equals(card.cardID);
    }
}
