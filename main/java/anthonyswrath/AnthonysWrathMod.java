package anthonyswrath;

import basemod.BaseMod;
import basemod.interfaces.ISubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;

/**
 * Mod entry point (T003).
 *
 * <p>Owns BaseMod initialization and subscriber registration skeleton only. The
 * custom-mode option, curse card, strings and run hooks are added by later tasks
 * (T008 / T011 / T012 / T017 / T024 / T027).</p>
 */
@SpireInitializer
public class AnthonysWrathMod implements ISubscriber {

    public AnthonysWrathMod() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new AnthonysWrathMod();
    }
}
