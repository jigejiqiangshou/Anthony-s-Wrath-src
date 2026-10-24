package anthonyswrath.options;

import com.megacrit.cardcrawl.screens.custom.CustomMod;

/**
 * Custom-mode option「安东尼之怒」(T007).
 *
 * <p>The option is a run-long negative modifier, so it is registered the same way as the
 * vanilla run-long negative modifiers ("Binary", "Terminal", ...): red colour marker and
 * {@code isDailyMod = true}. The third flag is the vanilla way of routing a selected
 * modifier through {@code CustomTrial.dailyModIDs()}, which the game stores in
 * {@code AbstractPlayer.customMods} and persists in {@code SaveFile.custom_mods}; that is
 * the single source of truth for "is this option selected this run" (see
 * {@code anthonyswrath.run.RunState}).</p>
 *
 * <p>The display name and the effect description are not built here: {@link CustomMod}'s
 * constructor resolves them from {@code LocalizedStrings.mod} (run_mods JSON) using
 * {@link #OPTION_ID} as the key, so the texts live in
 * {@code strings/anthonyswrath/<language>/run_mods.json} (constitution: all player-visible
 * text is externalised, Chinese and English keys match one-to-one).</p>
 */
public final class AnthonysWrathOption {

    /**
     * Stable option identifier (FR-001, EC-007).
     *
     * <p>Namespaced with the mod id so it can never collide with a vanilla modifier or with
     * a same-named option from another mod; every decision is made on this id, never on a
     * display name. It is also the {@code RunModStrings} key of the option's texts.</p>
     */
    public static final String OPTION_ID = "anthonyswrath:AnthonysWrathOption";

    /** Red marker, same as the vanilla negative modifiers ("Insanity", "Binary", ...). */
    private static final String NEGATIVE_COLOR = "r";

    /** Run-long modifier: routed through CustomTrial so it survives save / load (T012). */
    private static final boolean DAILY_MOD = true;

    /**
     * Creates the {@link CustomMod} instance handed to BaseMod's
     * {@code AddCustomModeModsSubscriber.receiveCustomModeMods(List<CustomMod>)} (T008).
     *
     * @return a fresh option instance; the custom-mode screen owns its selected state
     */
    public static CustomMod create() {
        return new CustomMod(OPTION_ID, NEGATIVE_COLOR, DAILY_MOD);
    }

    private AnthonysWrathOption() {
        // utility class
    }
}
