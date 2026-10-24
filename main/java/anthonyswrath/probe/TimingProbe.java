package anthonyswrath.probe;

import basemod.BaseMod;
import basemod.DevConsole;
import basemod.devcommands.ConsoleCommand;
import basemod.interfaces.ISubscriber;
import basemod.interfaces.OnPlayerTurnStartSubscriber;
import basemod.interfaces.OnStartBattleSubscriber;
import basemod.interfaces.PostBattleSubscriber;
import basemod.interfaces.PostCreateStartingDeckSubscriber;
import basemod.interfaces.PostDungeonInitializeSubscriber;
import basemod.interfaces.PostDrawSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.PreStartGameSubscriber;
import basemod.interfaces.StartActSubscriber;
import basemod.interfaces.StartGameSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

/**
 * T004 in-game timing probe (development only, removed by T035).
 *
 * <p>Answers the six questions in {@code specs/001-anthonys-wrath/research.md}
 * R-004..R-008 / {@code quickstart.md} §3 by logging hook order and pile contents.
 * The {@code ProbePatches} classes log the vanilla-side call sites (shuffle,
 * hand-add, purgeable, transform, curse pool, removeCard).</p>
 *
 * <p>Self-registers through its own {@code @SpireInitializer}; the main entry class
 * stays untouched, so deleting this package at T035 is enough.</p>
 */
@SpireInitializer
public class TimingProbe implements ISubscriber,
        PostInitializeSubscriber,
        PreStartGameSubscriber,
        StartGameSubscriber,
        StartActSubscriber,
        PostCreateStartingDeckSubscriber,
        PostDungeonInitializeSubscriber,
        OnStartBattleSubscriber,
        PostBattleSubscriber,
        OnPlayerTurnStartSubscriber,
        PostDrawSubscriber {

    private static boolean commandRegistered;
    private static boolean consoleEnabled;

    public TimingProbe() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new TimingProbe();
    }

    private static void log(String tag, String msg) {
        try {
            ProbeLog.log(tag, msg);
        } catch (Throwable ignored) {
            // never break the run because of the probe
        }
    }

    private static void ensureConsoleAndCommand() {
        try {
            if (!consoleEnabled) {
                DevConsole.enabled = true;
                consoleEnabled = true;
                log("probe.console", "DevConsole.enabled=true (toggle with the grave/backquote key)");
            }
            if (!commandRegistered) {
                ConsoleCommand.addCommand("awprobe", AwProbeCommand.class);
                commandRegistered = true;
                log("probe.console", "registered command: awprobe info|purge|cursepool|transform <cardID>|addcard <cardID>|shuffledeck");
            }
        } catch (Throwable t) {
            log("probe.console", "console/command setup deferred: " + t);
        }
    }

    @Override
    public void receivePostInitialize() {
        log("hook.PostInitialize", ProbeLog.state() + " | " + ProbeLog.mods());
        ensureConsoleAndCommand();
    }

    @Override
    public void receivePreStartGame() {
        log("hook.PreStartGame", "playerClassSelect starts | " + ProbeLog.mods());
        ensureConsoleAndCommand();
    }

    @Override
    public void receiveStartGame() {
        log("hook.StartGame", ProbeLog.state() + " | " + ProbeLog.mods());
        ensureConsoleAndCommand();
    }

    @Override
    public void receiveStartAct() {
        log("hook.StartAct", ProbeLog.state());
    }

    @Override
    public void receivePostCreateStartingDeck(AbstractPlayer.PlayerClass chosenClass, CardGroup deck) {
        log("hook.PostCreateStartingDeck", "class=" + chosenClass + " deck=" + ProbeLog.ids(deck)
                + " | " + ProbeLog.mods());
    }

    @Override
    public void receivePostDungeonInitialize() {
        log("hook.PostDungeonInitialize", ProbeLog.state()
                + "\n    curseCardPool " + ProbeLog.ids(AbstractDungeon.curseCardPool));
    }

    @Override
    public void receiveOnBattleStart(AbstractRoom room) {
        log("hook.StartBattle", "room=" + (room == null ? "null" : room.getClass().getSimpleName())
                + " " + ProbeLog.pileSizes() + "\n    [after first shuffle, before initial draw]" + ProbeLog.pileDump());
    }

    @Override
    public void receivePostBattle(AbstractRoom room) {
        log("hook.PostBattle", "room=" + (room == null ? "null" : room.getClass().getSimpleName())
                + " " + ProbeLog.pileSizes());
    }

    @Override
    public void receiveOnPlayerTurnStart() {
        log("hook.PlayerTurnStart", ProbeLog.pileSizes());
    }

    @Override
    public void receivePostDraw(AbstractCard card) {
        log("hook.PostDraw", "card=" + (card == null ? "null" : card.cardID) + " " + ProbeLog.pileSizes());
    }
}
