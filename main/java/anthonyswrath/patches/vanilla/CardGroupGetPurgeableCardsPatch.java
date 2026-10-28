package anthonyswrath.patches.vanilla;

import anthonyswrath.cards.AnthonysWrathCurse;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;

/**
 * Vanilla intercept for user story 5 (T029): the curse must never appear in a "cards you may
 * remove" candidate list (FR-012, EC-009).
 *
 * <p>{@code CardGroup.getPurgeableCards()} is the single candidate-list builder of the base
 * game: the shop removal service, the rest-site (Peace Pipe) removal, the relic removals
 * (Empty Cage, Astrolabe), Neow's "remove a card" reward, every removal / transform event
 * (Cleric, Golden Wing, Back to Basics, Living Wall, Bonfire, Designer, Note For Yourself,
 * Purification Shrine, Transmogrifier, Gremlin Wheel, We Meet Again trade, ...) call it and
 * open their grid with the returned group. The base game filters the reference card
 * (Bell's Curse) in this very method by hard-coded card id, so filtering this mod's card at
 * the same point is exactly the "same behaviour, no extra restriction" rule of FR-012.</p>
 *
 * <p>The postfix returns the filtered group: ModTheSpire 3.30.3 only replaces a target's
 * return value for patches declared with {@code @SpirePatch2}, whose param mapper recognises
 * the {@code __result} handler parameter and rewrites the target into
 * {@code return ($r)handler(...)}; the legacy {@code @SpirePatch} path would ignore the
 * returned value (the MTS bytecode distinction is recorded in memory fact f024). Every other
 * card stays in its original order; the vanilla method already builds a fresh group, so no
 * caller-owned list is touched.</p>
 */
public final class CardGroupGetPurgeableCardsPatch {

    private CardGroupGetPurgeableCardsPatch() {
        // patch holder
    }

    /** {@code CardGroup.getPurgeableCards()}: remove this mod's curse from the candidates. */
    @SpirePatch2(clz = CardGroup.class, method = "getPurgeableCards", paramtypez = {})
    public static class FilterCurseFromCandidates {

        @SpirePostfixPatch
        public static CardGroup Postfix(CardGroup __result) {
            if (__result == null || __result.group == null || __result.group.isEmpty()) {
                return __result;
            }
            for (int i = __result.group.size() - 1; i >= 0; i--) {
                AbstractCard card = __result.group.get(i);
                if (card != null && AnthonysWrathCurse.CARD_ID.equals(card.cardID)) {
                    __result.group.remove(i);
                }
            }
            return __result;
        }
    }
}
