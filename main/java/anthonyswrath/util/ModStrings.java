package anthonyswrath.util;

import basemod.BaseMod;
import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.RunModStrings;
import java.util.Locale;

/**
 * Loads the bilingual resource files registered by {@code EditStringsSubscriber} (T005
 * skeleton, filled by T009 / T013).
 *
 * <h2>Naming convention (T005)</h2>
 *
 * <pre>
 * src/main/resources/strings/anthonyswrath/
 * ├── eng/cards.json       CardStrings, key = full card id
 * ├── eng/run_mods.json    RunModStrings, key = option id
 * ├── zhs/cards.json       same keys as eng/cards.json
 * └── zhs/run_mods.json    same keys as eng/run_mods.json
 * </pre>
 *
 * <p>The directory name is the lower-cased {@code Settings.GameLanguage} constant
 * ({@code Settings.GameLanguage.ZHS} → {@code zhs}), which is also how BaseMod 5.56 names
 * its own localisation directories. Chinese and English files must always carry exactly the
 * same key set (constitution: player-visible text is externalised and the two languages
 * match one-to-one).</p>
 *
 * <h2>Loading rules verified against the shipped jars (BaseMod 5.56.0 / game 12-18-2022)</h2>
 *
 * <ul>
 *   <li>{@code BaseMod.loadCustomStringsFile(Class, path)} reads
 *       {@code Gdx.files.internal(path)} as UTF-8 and hands the JSON to
 *       {@code loadJsonStrings}; the class selects the {@code LocalizedStrings} map
 *       ({@code CardStrings} → {@code cards}, {@code RunModStrings} → {@code mod}).</li>
 *   <li>BaseMod 5.56's {@code findCallingModName()} returns {@code null}, so keys are
 *       <em>not</em> prefixed with the mod id: a card key must be the full card id
 *       ({@code anthonyswrath:AnthonysWrath}) and a run-mod key the full option id.</li>
 *   <li>{@code CustomMod} resolves its name/description through
 *       {@code LocalizedStrings.getRunModString(optionId)}, so the run_mods key is the
 *       option id — this is how the option gets localised text at all.</li>
 *   <li>Files live on the mod jar's classpath; a missing file makes
 *       {@code FileHandle.readString} throw, hence the existence check below.</li>
 * </ul>
 */
public final class ModStrings {

    /** Classpath root of the bilingual resources. */
    private static final String RESOURCE_ROOT = "strings/anthonyswrath/";

    /** Language used when the current game language has no resources of its own. */
    private static final String FALLBACK_LANGUAGE = "eng";

    private static final String CARD_FILE = "cards.json";
    private static final String RUN_MOD_FILE = "run_mods.json";

    /**
     * Loads cards and run-mod strings for the current game language, falling back to English
     * for languages this mod does not translate yet.
     */
    public static void loadEditStrings() {
        String language = selectLanguage();
        BaseMod.loadCustomStringsFile(
                CardStrings.class, RESOURCE_ROOT + language + "/" + CARD_FILE);
        BaseMod.loadCustomStringsFile(
                RunModStrings.class, RESOURCE_ROOT + language + "/" + RUN_MOD_FILE);
    }

    private static String selectLanguage() {
        String language = Settings.language.name().toLowerCase(Locale.ROOT);
        if (hasResources(language)) {
            return language;
        }
        return FALLBACK_LANGUAGE;
    }

    private static boolean hasResources(String language) {
        String languageRoot = RESOURCE_ROOT + language + "/";
        return Gdx.files.internal(languageRoot + CARD_FILE).exists()
                && Gdx.files.internal(languageRoot + RUN_MOD_FILE).exists();
    }

    private ModStrings() {
        // utility class
    }
}
