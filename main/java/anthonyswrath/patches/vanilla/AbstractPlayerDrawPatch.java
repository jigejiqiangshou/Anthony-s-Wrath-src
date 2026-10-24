package anthonyswrath.patches.vanilla;

import anthonyswrath.run.RunState;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

/**
 * Vanilla intercept for user story 3 (T023): after the defeat caused by this mod's curse has
 * been settled, no further draw is processed (EC-004).
 *
 * <p>{@code AbstractPlayer.draw(int)} is the single entry point of the vanilla draw loop:
 * the no-argument overload only plays a sound and delegates to it with {@code 1}, and every
 * draw action ({@code DrawCardAction}, {@code EmptyDeckShuffleAction} follow-ups, ...) calls
 * it. A prefix handler that returns a present {@code SpireReturn} makes ModTheSpire skip the
 * whole vanilla body, which is exactly the "skip at the entry point" required by EC-004:
 * nothing is moved from the draw pile to the hand and no draw-triggered effects run.</p>
 *
 * <p>The flag is only set by the defeat path itself ({@code run.DefeatOnDraw} →
 * {@code RunState.tryMarkDefeated()}), so ordinary draws before the defeat are never
 * affected. The flag is reset by the {@code StartGame} subscriber at the beginning of every
 * run (new or loaded), so a later run is not stuck in the defeated state.</p>
 *
 * <p>Note on the draw call that triggered the defeat: that call is already running when the
 * flag is set, so its remaining iterations fall through this prefix only on their next
 * {@code draw(int)} call — the game's {@code DEATH} screen stops the action manager at the
 * next frame, so no further draw action is executed at all. This is documented in
 * {@code run.DefeatOnDraw} and in the US3 quickstart section.</p>
 */
public final class AbstractPlayerDrawPatch {

    private AbstractPlayerDrawPatch() {
        // patch holder
    }

    /** {@code AbstractPlayer.draw(int)}: skip every later draw once the run is defeated. */
    @SpirePatch(clz = AbstractPlayer.class, method = "draw", paramtypez = {int.class})
    public static class DrawInt {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(AbstractPlayer __instance, int numCards) {
            if (RunState.isDefeated()) {
                return SpireReturn.Return();
            }
            return SpireReturn.Continue();
        }
    }
}
