package anthonyswrath;

import anthonyswrath.cards.AnthonysWrathCurse;
import anthonyswrath.options.AnthonysWrathOption;
import anthonyswrath.run.StartDeckInjector;
import anthonyswrath.util.ModStrings;
import basemod.BaseMod;
import basemod.interfaces.AddCustomModeModsSubscriber;
import basemod.interfaces.EditCardsSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.ISubscriber;
import basemod.interfaces.PostCreateStartingDeckSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.screens.custom.CustomMod;
import java.util.List;

/**
 * Mod entry point.
 *
 * <p>Owns BaseMod initialisation and every subscription used by user story 1:</p>
 * <ul>
 *   <li>{@link EditStringsSubscriber} — bilingual card text and option text (T009 / T013);</li>
 *   <li>{@link EditCardsSubscriber} — registers the curse card (T011);</li>
 *   <li>{@link AddCustomModeModsSubscriber} — registers the custom-mode option (T008);</li>
 *   <li>{@link PostCreateStartingDeckSubscriber} — injects one copy into the starting deck
 *       (T012).</li>
 * </ul>
 *
 * <p>All interfaces are the ones present in the shipped BaseMod 5.56.0 jar (memory fact
 * f009); the subscription is done once through {@code BaseMod.subscribe(this)}. Later user
 * stories add their own subscribers here; vanilla intercepts live in
 * {@code anthonyswrath.patches.vanilla}.</p>
 */
@SpireInitializer
public class AnthonysWrathMod
        implements ISubscriber,
                EditStringsSubscriber,
                EditCardsSubscriber,
                AddCustomModeModsSubscriber,
                PostCreateStartingDeckSubscriber {

    public AnthonysWrathMod() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new AnthonysWrathMod();
    }

    @Override
    public void receiveEditStrings() {
        ModStrings.loadEditStrings();
    }

    @Override
    public void receiveEditCards() {
        BaseMod.addCard(new AnthonysWrathCurse());
    }

    @Override
    public void receiveCustomModeMods(List<CustomMod> mods) {
        mods.add(AnthonysWrathOption.create());
    }

    @Override
    public void receivePostCreateStartingDeck(
            AbstractPlayer.PlayerClass playerClass, CardGroup startingDeck) {
        StartDeckInjector.injectIfSelected(playerClass, startingDeck);
    }
}
