package anthonyswrath.run;

import anthonyswrath.cards.AnthonysWrathCurse;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

/**
 * Adds exactly one copy of the curse to the starting deck when the option is selected
 * (T012; FR-002, FR-003, EC-002).
 *
 * <p>Injection point: {@code PostCreateStartingDeckSubscriber.receivePostCreateStartingDeck}
 * — the point validated by the T004 sentinel experiment (research §R-006): it runs after
 * {@code initializeStarterDeck} and before the first shuffle, and a card added there survives
 * a later "Draft" fill of the (initially empty) starting deck. A loaded save never fires
 * this hook, so the curse cannot be injected twice into a resumed run.</p>
 */
public final class StartDeckInjector {

    /**
     * Injects the curse when — and only when — the run was started with the option
     * selected.
     *
     * @param playerClass the chosen class, as delivered by BaseMod
     * @param startingDeck the starting deck group owned by the game; may be empty for
     *        custom-mode options that rewrite the deck (EC-002)
     */
    public static void injectIfSelected(
            AbstractPlayer.PlayerClass playerClass, CardGroup startingDeck) {
        if (startingDeck == null || !RunState.isOptionSelectedForNewRun()) {
            return;
        }
        if (alreadyContainsCurse(startingDeck)) {
            // Defensive only: the hook fires once per new run, but never risk two copies.
            return;
        }
        startingDeck.addToTop(new AnthonysWrathCurse());
    }

    private static boolean alreadyContainsCurse(CardGroup group) {
        for (AbstractCard card : group.group) {
            if (card != null && AnthonysWrathCurse.CARD_ID.equals(card.cardID)) {
                return true;
            }
        }
        return false;
    }

    private StartDeckInjector() {
        // utility class
    }
}
