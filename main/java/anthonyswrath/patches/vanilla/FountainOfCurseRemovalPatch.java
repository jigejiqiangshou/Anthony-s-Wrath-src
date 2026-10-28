package anthonyswrath.patches.vanilla;

import anthonyswrath.run.CurseRemovalGuard;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.events.shrines.FountainOfCurseRemoval;

/**
 * Vanilla intercept for user story 5 (T030): the "Fountain of Cleansing" event must not
 * delete this mod's curse (FR-012, EC-009).
 *
 * <p>{@code FountainOfCurseRemoval.buttonEffect(int)} bulk-removes every curse in the master
 * deck except the three reference curses it hard-codes (and bottled cards). It does not go
 * through {@code CardGroup.getPurgeableCards()}, so the candidate-list filter of
 * {@code CardGroupGetPurgeableCardsPatch} (T029) cannot cover it; the 2026-10-24 T020 probe
 * log even observed the event deleting the card while US5 was unimplemented
 * ({@code patch.fountain} / {@code patch.removeCard} lines, memory log 026).</p>
 *
 * <p>The prefix/postfix pair brackets the vanilla call with
 * {@link CurseRemovalGuard}: the curse is taken out of the master deck before the vanilla
 * loop runs and restored afterwards, so the loop treats it exactly like the hard-coded
 * reference cards (no purge, no purge effect, no metric entry) and the player keeps the
 * card. The bracket is applied to every {@code buttonEffect} call, not only the purge
 * button: the other branches (refuse / leave) do not read the master deck, so a temporary
 * absence is invisible there.</p>
 *
 * <p>Only this mod's card is affected; the vanilla handling of all other curses is
 * unchanged (FR-012).</p>
 */
public final class FountainOfCurseRemovalPatch {

    private FountainOfCurseRemovalPatch() {
        // patch holder
    }

    /** {@code FountainOfCurseRemoval.buttonEffect(int)}: bracket the bulk curse removal. */
    @SpirePatch(clz = FountainOfCurseRemoval.class, method = "buttonEffect",
            paramtypez = {int.class})
    public static class SkipCurseInBulkRemoval {

        @SpirePrefixPatch
        public static void Prefix(FountainOfCurseRemoval __instance, int buttonIdx) {
            CurseRemovalGuard.suspendFromMasterDeck();
        }

        @SpirePostfixPatch
        public static void Postfix(FountainOfCurseRemoval __instance, int buttonIdx) {
            CurseRemovalGuard.restoreToMasterDeck();
        }
    }
}
