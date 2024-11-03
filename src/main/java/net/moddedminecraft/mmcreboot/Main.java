package net.moddedminecraft.mmcreboot;

import com.google.inject.Inject;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Config.Permissions;
import net.moddedminecraft.mmcreboot.commands.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.*;
import org.spongepowered.api.scheduler.ScheduledTaskFuture;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.criteria.Criteria;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlots;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;
import sawfowl.localeapi.api.TextUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin("mmcreboot")
public class Main {

    public static final Logger logger = LogManager.getLogger("MCReboot");

    @Inject
    @DefaultConfig(sharedRoot = false)
    public Path defaultConf;

    @Inject
    @ConfigDir(sharedRoot = false)
    public Path configDir;

    public boolean voteCancel = false;
    public boolean cdTimer = false;
    public boolean voteStarted = false;
    public int yesVotes = 0;
    public int noVotes = 0;
    public ArrayList<String> hasVoted = new ArrayList<>();
    public static ArrayList<Integer> realTimeTimes = new ArrayList<>();

    public int voteSeconds;
    public String reason;

    public long startTimestamp;
    public boolean justStarted = true;
    public boolean isRestarting = false;
    public boolean TPSRestarting = false;
    public boolean rebootConfirm = false;
    public boolean tasksScheduled = false;
    public double nextRealTimeRestart;

    // Timers & Tasks
    private final ArrayList<Timer> warningTimers = new ArrayList<Timer>();
    ScheduledTaskFuture<?> justStartedTimer;
    ScheduledTaskFuture<?> scoreboardRefreshTask;
    ScheduledTaskFuture<?> reduceVoteTask;
    ScheduledTaskFuture<?> checkRealTimeRestartTask;
    ScheduledTaskFuture<?> checkTPSForRestartTask;

    private boolean playSoundNow = false;

    private Config config;
    private Messages messages;

    private Scoreboard board;
    private BossBar bar;

    public final PluginContainer container;

    @Inject
    public Main(final PluginContainer container) {
        this.container = container;
    }

    @Listener
    public void onServerAboutStart(ConstructPluginEvent event) throws IOException {
        Sponge.eventManager().registerListeners(container, new EventListener(this));
        this.config = new Config(this);
        this.messages = new Messages(this);
    }

    @Listener
    public void onServerStart(StartedEngineEvent<Server> event) {
        if(Config.restartType.equalsIgnoreCase("fixed")) {
            logger.info("[MMCReboot] Using fixed restart scheduler");
            scheduleTasks();
            if (!Config.defaultRestartReason.isEmpty()) {
                reason = Config.defaultRestartReason;
            }
        } else if(Config.restartType.equalsIgnoreCase("realtime")) {
            logger.info("[MMCReboot] Using realtime restart scheduler");
            scheduleRealTimeRestart();
            if (!Config.defaultRestartReason.isEmpty()) {
                reason = Config.defaultRestartReason;
            }
            Config.restartInterval = 0;
        } else {
            logger.info("[MMCReboot] No automatic restarts scheduled!");
        }

        if (Config.voteEnabled) {
            justStartedTimer = Sponge.asyncScheduler().executor(container).schedule(() -> {
                justStarted = false;
            }, (long) Config.timerStartvote * 60 * 1000, TimeUnit.SECONDS);
        }

        scoreboardRefreshTask = Sponge.asyncScheduler().executor(container).scheduleWithFixedDelay(this::action,250, 500, TimeUnit.MILLISECONDS);
        reduceVoteTask = Sponge.asyncScheduler().executor(container).scheduleWithFixedDelay(this::reduceVote,1, 1, TimeUnit.SECONDS);
        checkRealTimeRestartTask = Sponge.asyncScheduler().executor(container).scheduleWithFixedDelay(this::checkRealTimeRestart,15, 15, TimeUnit.MINUTES);
        checkTPSForRestartTask = Sponge.asyncScheduler().executor(container).scheduleWithFixedDelay(this::CheckTPSForRestart,Config.tpsCheckDelay, 1, TimeUnit.MINUTES);

        logger.info("MMCReboot Loaded");
    }
    @Listener
    public void onGameStop(StoppedGameEvent event) {
        removeScoreboard();
        removeBossBar();
        cancelTasks();
        scoreboardRefreshTask.task().cancel();
        reduceVoteTask.task().cancel();
        checkRealTimeRestartTask.task().cancel();
        checkTPSForRestartTask.task().cancel();
    }

    @Listener
    public void onServerStop(StoppingEngineEvent<Server> event) {
        logger.info("MMCReboot Stopped");
    }

    @Listener
    public void onPluginReload(RefreshGameEvent event) throws IOException {
        cancelTasks();
        removeScoreboard();
        removeBossBar();
        isRestarting = false;

        this.config = new Config(this);
        this.messages = new Messages(this);

        if(Config.restartType.equalsIgnoreCase("fixed")) {
            scheduleTasks();
            if (!Config.defaultRestartReason.isEmpty()) {
                reason = Config.defaultRestartReason;
            }
        } else if(Config.restartType.equalsIgnoreCase("realtime")) {
            scheduleRealTimeRestart();
            if (!Config.defaultRestartReason.isEmpty()) {
                reason = Config.defaultRestartReason;
            }
            Config.restartInterval = 0;
        } else {
            logger.info("[MMCReboot] No automatic restarts scheduled!");
        }
    }

    @Listener
    public void onRegisterSpongeCommand(final RegisterCommandEvent<Command.Parameterized> event) {

        // /Reboot help
        Command.Parameterized rebootHelp = Command.builder()
                .shortDescription(Component.text("List of commands usable to the player"))
                .executor(new RebootHelp(this))
                .build();

        // /Reboot vote
        Command.Parameterized rebootVote = Command.builder()
                .shortDescription(Component.text("Submit a vote to reboot the server"))
                .executor(new RebootVote(this))
                .addParameters(Parameter.remainingJoinedStrings().key("answer").optional().build())
                .build();

        // /Reboot cancel
        Command.Parameterized rebootCancel = Command.builder()
                .shortDescription(Component.text("Cancel the current timed reboot"))
                .permission(Permissions.COMMAND_CANCEL)
                .executor(new RebootCancel(this))
                .build();

        // /Reboot time
        Command.Parameterized rebootTime = Command.builder()
                .shortDescription(Component.text("Get the time remaining until the next restart"))
                .permission(Permissions.COMMAND_TIME)
                .executor(new RebootTime(this))
                .build();

        // /Reboot confirm
        Command.Parameterized rebootConfirm = Command.builder()
                .shortDescription(Component.text("Reboot the server immediately"))
                .permission(Permissions.COMMAND_NOW)
                .executor(new RebootConfirm(this))
                .build();
        // /Reboot now
        Command.Parameterized rebootNow = Command.builder()
                .shortDescription(Component.text("Reboot the server immediately"))
                .permission(Permissions.COMMAND_NOW)
                .executor(new RebootNow(this))
                .build();

        // /Reboot start h/m/s time reason
        Command.Parameterized rebootStart = Command.builder()
                .shortDescription(Component.text("Reboot base command"))
                .permission(Permissions.COMMAND_START)
                .addParameters(Parameter.choices("h", "m", "s").key("h/m/s").build(),
                        Parameter.integerNumber().key("time").build(),
                        Parameter.remainingJoinedStrings().key("reason").optional().build())
                .executor(new RebootCMD(this))
                .build();

        event.register(this.container,Command.builder()
                .shortDescription(Component.text("Reboot base command"))
                .addChild(rebootStart, "start")
                .addChild(rebootNow, "now")
                .addChild(rebootConfirm, "confirm")
                .addChild(rebootTime, "time")
                .addChild(rebootCancel, "cancel")
                .addChild(rebootVote, "vote")
                .addChild(rebootHelp, "help")
                .build(), "reboot", "mmcreboot", "restart"
        );

    }

    public Double getTPS() {
        return Sponge.server().ticksPerSecond();
    }

    public boolean getTPSRestarting() {
        return TPSRestarting;
    }

    public void setTPSRestarting(boolean bool) {
        TPSRestarting = bool;
    }

    public void CheckTPSForRestart() {
        if (getTPS() < Config.tpsMinimum && Config.tpsEnabled && !getTPSRestarting()) {
            double timeLeft = Config.restartInterval * 3600 - ((double) (System.currentTimeMillis() - startTimestamp) / 1000);
            int hours = (int) (timeLeft / 3600);
            int minutes = (int) ((timeLeft - hours * 3600) / 60);
            if (hours == 0 && minutes > 20 || hours > 0) {
                setTPSRestarting(true);
                Timer warnTimer = new Timer();
                warningTimers.add(warnTimer);
                warnTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (getTPS() < Config.tpsMinimum) {
                            isRestarting = true;
                            Config.restartInterval = (Config.tpsTimer + 1) / 3600.0;
                            logger.info("[MMCReboot] scheduling restart tasks...");
                            if (Config.tpsUseReason) {
                                reason = Config.tpsMessage;
                            }
                            scheduleTasks();
                        } else {
                            setTPSRestarting(false);
                        }
                    }
                }, 15000);
            }
        }
    }

    public void action() {
        if (isRestarting && Config.timerUseScoreboard) {
            if (Config.restartInterval > 0) {
                displayRestart(Config.restartInterval * 3600);
            } else if (nextRealTimeRestart > 0){
                displayRestart(nextRealTimeRestart);
            }
        }
        if (voteStarted && voteCancel && Config.timerUseVoteScoreboard) {
            displayVotes();
        }
    }

    public void reduceVote() {
        if (voteStarted && !voteCancel) {
            if (voteSeconds > 0) {
                voteSeconds -= 1;
            }
            if (voteSeconds < 0) {
                voteSeconds = 0;
            }
        }
    }

    public void checkRealTimeRestart() {
        if(Config.restartType.equalsIgnoreCase("realtime")) {
            if (nextRealTimeRestart == 0 && !isRestarting && !voteCancel) {
                scheduleRealTimeRestart();
            }
        }
    }

    Runnable shutdownCmdTasks = () -> {
        cancelTasks();
        removeScoreboard();
        removeBossBar();
        useCommandOnRestart();
    };

    Runnable shutdownRunnable = () -> {
        if (getTPSRestarting()) {
            if (getTPS() >= Config.tpsMinimum && Config.tpsRestartCancel) {
                cancelTasks();
                removeScoreboard();
                removeBossBar();
                isRestarting = false;
                setTPSRestarting(false);
                if (!Config.tpsRestartCancelMsg.isEmpty()) {
                    broadcastMessage("&f[&6Restart&f] " + Config.tpsRestartCancelMsg);
                }
            } else if (getTPS() < Config.tpsMinimum) {
                if (Config.restartUseCommand) {
                    shutdownCmdTasks.run();
                } else {
                    stopServer();
                }
            }
        } else {
            if (Config.restartUseCommand) {
                shutdownCmdTasks.run();
            } else {
                stopServer();
            }
        }
    };

    public void scheduleRealTimeRestart() {
        cancelTasks();
        nextRealTimeRestart = getNextRealTimeFromConfig();
        double rInterval = nextRealTimeRestart;
        if (Config.timerBroadcast != null) {
            warningMessages(rInterval);
        }
        int roundedInterval = Math.toIntExact(Math.round(rInterval));

        Sponge.asyncScheduler().executor(container).schedule(shutdownRunnable, roundedInterval, TimeUnit.SECONDS);

        logger.info("[MMCReboot] RebootCMD scheduled for " + (long)(nextRealTimeRestart) + " seconds from now!");
        tasksScheduled = true;
        startTimestamp = System.currentTimeMillis();
        isRestarting = true;
    }

    public void scheduleTasks() {
        boolean wasTPSRestarting = getTPSRestarting();
        cancelTasks();
        setTPSRestarting(wasTPSRestarting);
        double rInterval = Config.restartInterval * 3600;
        if (Config.timerBroadcast != null) {
            warningMessages(rInterval);
        }

        int roundedInterval = Math.toIntExact(Math.round(rInterval));

        Sponge.asyncScheduler().executor(container).schedule(shutdownRunnable, roundedInterval, TimeUnit.SECONDS);

        logger.info("[MMCReboot] RebootCMD scheduled for " + (long)(Config.restartInterval  * 3600.0) + " seconds from now!");
        tasksScheduled = true;
        startTimestamp = System.currentTimeMillis();
        isRestarting = true;
    }

    private void warningMessages(double rInterval) {
        Config.timerBroadcast.stream().filter(aTimerBroadcast -> rInterval * 60 - aTimerBroadcast > 0).forEach(aTimerBroadcast -> {
            Timer warnTimer = new Timer();
            warningTimers.add(warnTimer);
            if (aTimerBroadcast <= rInterval) {
                warnTimer.schedule(new TimerTask() {
                    public void run() {
                        double timeLeft = rInterval - ((double) (System.currentTimeMillis() - startTimestamp) / 1000);
                        int hours = (int) (timeLeft / 3600);
                        int minutes = (int) ((timeLeft - hours * 3600) / 60);
                        int seconds = (int) timeLeft % 60;

                        logger.info("Debug: Restart Time = " + hours + " : " + minutes + " : " + seconds);

                        NumberFormat formatter = new DecimalFormat("00");
                        String s = formatter.format(seconds);
                        if (Config.timerUseChat) {
                            if (minutes > 1) {
                                String message = Messages.getRestartNotificationMinutes().replace("%minutes%", "" + minutes).replace("%seconds%", s);
                                broadcastMessage("&f[&6Restart&f] " + message);
                            } else if (minutes == 1) {
                                String message = Messages.getRestartNotificationMinute().replace("%minutes%", "" + minutes).replace("%seconds%", s);
                                broadcastMessage("&f[&6Restart&f] " + message);
                            } else {
                                String message = Messages.getRestartNotificationSeconds().replace("%minutes%", "" + minutes).replace("%seconds%", s);
                                broadcastMessage("&f[&6Restart&f] " + message);
                            }
                        }
                        logger.info("[MMCReboot] " + "&bThe server will be restarting in &f" + hours + "h" + minutes + "m" + seconds + "s");
                        if (!playSoundNow && Config.playSoundFirstTime >= aTimerBroadcast) {
                            playSoundNow = true;
                        }

                        Component titleString = fromLegacy(Config.titleMessage.replace("{hours}", "" + hours).replace("{minutes}", "" + minutes).replace("{seconds}", s));

                        for (Player p : Sponge.server().onlinePlayers()) {
                            if (Config.playSoundEnabled && playSoundNow) {
                                p.playSound(Sound.sound(Key.key("block.note_block.pling"), Sound.Source.MUSIC, 1f, 1f));
                            }
                            if (Config.titleEnabled) {
                                Title title;
                                if (reason != null) {
                                    title = Title.title(titleString,
                                            fromLegacy(reason),
                                            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(Config.titleStayTime), Duration.ofSeconds(1)));
                                } else {
                                    title = Title.title(titleString,
                                            Component.empty(),
                                            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(Config.titleStayTime), Duration.ofSeconds(1)));
                                }
                                p.showTitle(title);
                            }
                        }
                        if (reason != null) {
                            broadcastMessage("&f[&6Restart&f] &d" + reason);
                        }
                        isRestarting = true;
                    }
                }, (long) ((rInterval - aTimerBroadcast) * 1000.0));
                logger.info("[MMCReboot] warning scheduled for " + (long) (rInterval - aTimerBroadcast) + " seconds from now!");
            }
        });
    }

    public void cancelTasks() {
        for (Timer warningTimer : warningTimers) warningTimer.cancel();
        warningTimers.clear();
        tasksScheduled = false;
        isRestarting = false;
        TPSRestarting = false;
        nextRealTimeRestart = 0;
    }


    public void stopServer() {
        logger.info("[MMCReboot] Restarting...");
        isRestarting = false;
        broadcastMessage("&cServer is restarting, we'll be right back!");
        try {
            if (Config.kickmessage.isEmpty()) {
                Sponge.server().onlinePlayers().forEach(ServerPlayer::kick);
            } else {
                Sponge.server().onlinePlayers().forEach(ServerPlayer -> ServerPlayer.kick(fromLegacy(Config.kickmessage)));
            }
            Sponge.asyncScheduler().executor(container).schedule(() -> {
                try {
                    shutdown();
                } catch (CommandException e) {
                    throw new RuntimeException(e);
                }
            }, 1, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.info("[MMCReboot] Something went wrong while saving & stopping!");
            logger.info("Exception: " + e);
            broadcastMessage("&cServer has encountered an error while restarting.");
        }
    }

    public void shutdown() throws CommandException {
        Sponge.server().commandManager().process(Sponge.systemSubject(), "save-all");
        Sponge.server().shutdown();
    }

    public int getNextRealTimeFromConfig() {
        realTimeTimes = new ArrayList<Integer>();
        for (String realTime : Config.realTimeInterval) {
            int time = getTimeUntil(realTime);
            realTimeTimes.add(time);
        }
        return Collections.min(realTimeTimes);
    }

    private int getTimeUntil(String time) {
        Calendar cal = Calendar.getInstance();
        int nowHour = cal.get(Calendar.HOUR_OF_DAY);
        int nowMin  = cal.get(Calendar.MINUTE);
        //int nowSec = cal.get(Calendar.SECOND);
        return getTimeTill(nowHour, nowMin, time);
    }

    private int getTimeTill(int nowHour, int nowMin, String endTime) {
        Matcher m = Pattern.compile("(\\d{2}):(\\d{2})").matcher(endTime);
        if (! m.matches()) {
            throw new IllegalArgumentException("Invalid time format: " + endTime);
        }
        int endHour = Integer.parseInt(m.group(1));
        int endMin  = Integer.parseInt(m.group(2));
        if (endHour >= 24 || endMin >= 60) {
            throw new IllegalArgumentException("Invalid time format: " + endTime);
        }
        int timeTill = (endHour * 60 + endMin - (nowHour * 60 + nowMin)) * 60;
        if (timeTill < 0) {
            timeTill += 24 * 60 * 60;
        }
        return timeTill;
    }

    public void useCommandOnRestart() {
        logger.info("[MMCReboot] Running Command");
        isRestarting = false;
        List<String> cmds = Config.restartCommands;
        for (String cmd : cmds) {
            try {
                Sponge.server().commandManager().process(Sponge.systemSubject(), cmd.replace("/", ""));
            } catch (CommandException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public int getTimeLeftInSeconds() {
        return voteSeconds;
    }

    public void displayRestart(double rInterval) {
        double timeLeft = rInterval - ((double)(System.currentTimeMillis() - startTimestamp) / 1000);
        int hours = (int)(timeLeft / 3600);
        int minutes = (int)((timeLeft - hours * 3600) / 60);
        int seconds = (int)timeLeft % 60;

        NumberFormat formatter = new DecimalFormat("00");
        String s = formatter.format(seconds);

        board = Scoreboard.builder().build();
        Objective obj = Objective.builder().name("restart").criterion(Criteria.DUMMY).displayName(fromLegacy(Messages.getSidebarRestartTimerTitle())).build();

        board.addObjective(obj);
        board.updateDisplaySlot(obj, DisplaySlots.SIDEBAR);

        Component score = Component.text(minutes +":" + s).color(TextColor.color(0,255, 0));
        obj.findOrCreateScore(score).setScore(0);

        int mSec = (minutes * 60);
        double val = ((mSec + seconds) * 100 / 300);
        float percent = (float)val / 100.0f;

        Sponge.server().onlinePlayers().stream().filter(player -> minutes < 5 && hours == 0).forEach(player -> {
            player.setScoreboard(board);
            if (Config.bossbarEnabled) {
                if (bar == null) {
                    bar = BossBar.bossBar(
                            Component.text(Config.bossbarTitle.replace("{minutes}", Integer.toString(minutes)).replace("{seconds}", s)),
                            percent,
                            BossBar.Color.GREEN,
                            BossBar.Overlay.PROGRESS);
                } else {
                    bar.progress(percent);
                }
                player.showBossBar(bar);
            }
        });
    }

    public void displayVotes() {
        board = Scoreboard.builder().build();

        Objective obj = Objective.builder().name("vote").criterion(Criteria.DUMMY).displayName(fromLegacy(Messages.getSidebarTitle())).build();

        board.addObjective(obj);
        board.updateDisplaySlot(obj, DisplaySlots.SIDEBAR);

        obj.findOrCreateScore(fromLegacy(Messages.getSidebarYes() + ":").color(TextColor.color(0, 255, 0))).setScore(yesVotes);
        obj.findOrCreateScore(fromLegacy(Messages.getSidebarNo() + ":").color(TextColor.color(51, 255, 255))).setScore(noVotes);
        obj.findOrCreateScore(fromLegacy(Messages.getSidebarTimeleft() + ":").color(TextColor.color(255, 0, 0))).setScore(getTimeLeftInSeconds());


        for (ServerPlayer player : Sponge.server().onlinePlayers()) {
            player.setScoreboard(board);
        }
    }

    public  void removeScoreboard() {
        for (ServerPlayer player : Sponge.server().onlinePlayers()) {
            player.scoreboard().clearSlot(DisplaySlots.SIDEBAR);
        }
    }

    public  void removeBossBar() {
        if (bar != null) {
            for (ServerPlayer player : Sponge.server().onlinePlayers()) {
                player.hideBossBar(bar);
            }
        }
    }

    public void broadcastMessage(String message) {
        Sponge.server().broadcastAudience().sendMessage(fromLegacy(message));
    }

    public void sendMessage(Audience audience, String message) {
        audience.sendMessage(fromLegacy(message));
    }

    public Component fromLegacy(String legacy) {
        return TextUtils.deserializeLegacy(legacy);
    }
}
