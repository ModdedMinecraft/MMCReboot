package net.moddedminecraft.mmcreboot;

import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;
import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Config.Permissions;
import net.moddedminecraft.mmcreboot.Tasks.ShutdownTask;
import net.moddedminecraft.mmcreboot.commands.*;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.bstats.sponge.Metrics2;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.boss.BossBarColors;
import org.spongepowered.api.boss.BossBarOverlays;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.critieria.Criteria;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlots;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.api.world.World;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin(id = "mmcreboot", name = "MMCReboot", version = "2.3.0", authors = {"Leelawd93"})
public class Main {

    @Inject
    public Logger logger;

    @Inject
    private Metrics2 metrics;

    @Inject
    @ConfigDir(sharedRoot = false)
    public Path configDir;

    @Inject
    @DefaultConfig(sharedRoot = false)
    public Path defaultConf;

    @Inject
    @DefaultConfig(sharedRoot = false)
    public File defaultConfFile;

    public boolean voteCancel = false;
    public boolean cdTimer = false;
    public boolean voteStarted = false;
    public int yesVotes = 0;
    public int noVotes = 0;
    public ArrayList<UUID> hasVoted = new ArrayList<UUID>();
    public static ArrayList<Integer> realTimeTimes = new ArrayList<Integer>();

    public int voteSeconds;
    public String reason;

    public long startTimestamp;
    public boolean justStarted = true;
    public boolean isRestarting = false;
    public boolean TPSRestarting = false;
    public boolean rebootConfirm = false;
    public boolean tasksScheduled = false;
    public double nextRealTimeRestart;

    // Timers
    private ArrayList<Timer> warningTimers = new ArrayList<Timer>();
    private Timer rebootTimer;
    private Timer justStartedTimer;

    private boolean playSoundNow = false;
    private Vector3d soundLoc;

    private Config config;
    private Messages messages;

    private CommandManager cmdManager = Sponge.getCommandManager();

    private Scoreboard board;

    private ServerBossBar bar;

    @Listener
    public void Init(GameInitializationEvent event) throws IOException, ObjectMappingException {
        Sponge.getEventManager().registerListeners(this, new EventListener(this));
        this.config = new Config(this);
        this.messages = new Messages(this);
        loadCommands();
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) throws IOException {
        soundLoc = new Vector3d(0, 64, 0);
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
            justStartedTimer = new Timer();
            this.justStartedTimer.schedule(new TimerTask() {
                public void run() {
                    justStarted = false;
                }
            }, (long)(Config.timerStartvote * 60 * 1000));
        }


        Sponge.getScheduler().createTaskBuilder().execute(this::action).delay(250, TimeUnit.MILLISECONDS).interval(500,TimeUnit.MILLISECONDS).async().name("mmcreboot-a-sendAction").submit(this);

        Sponge.getScheduler().createTaskBuilder().execute(this::reduceVote).interval(1, TimeUnit.SECONDS).async().name("mmcreboot-a-reduceVoteCount").submit(this);

        Sponge.getScheduler().createTaskBuilder().execute(this::checkRealTimeRestart).delay(1, TimeUnit.HOURS).interval(15 ,TimeUnit.MINUTES).async().name("mmcreboot-a-checkRealTimeRestart").submit(this);

        Sponge.getScheduler().createTaskBuilder().execute(this::CheckTPSForRestart).delay(Config.tpsCheckDelay, TimeUnit.MINUTES).interval(30, TimeUnit.SECONDS).async().name("mmcreboot-a-checkTPSForRestart").submit(this);

       /* metrics.addCustomChart(new Metrics.SimplePie("restart_type") {
            @Override
            public String getValue() {
                return Config.restartType;
            }
        });*/

        logger.info("MMCReboot Loaded");
    }

    @Listener
    public void onServerStop(GameStoppingEvent event) throws IOException {
        cancelTasks();
        logger.info("MMCReboot Disabled");
    }

    @Listener
    public void onPluginReload(GameReloadEvent event) throws IOException, ObjectMappingException {
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

    private void loadCommands() {
        // /Reboot help
        CommandSpec help = CommandSpec.builder()
                .description(Text.of("List of commands usable to the player"))
                .executor(new RebootHelp(this))
                .build();
        // /Reboot vote
        CommandSpec vote = CommandSpec.builder()
                .description(Text.of("Submit a vote to reboot the server"))
                .executor(new RebootVote(this))
                .arguments(GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("optional"))))
                .build();
        // /Reboot cancel
        CommandSpec cancel = CommandSpec.builder()
                .description(Text.of("Cancel the current timed reboot"))
                .permission(Permissions.COMMAND_CANCEL)
                .executor(new RebootCancel(this))
                .build();

        // /Reboot time
        CommandSpec time = CommandSpec.builder()
                .description(Text.of("Get the time remaining until the next restart"))
                .permission(Permissions.COMMAND_TIME)
                .executor(new RebootTime(this))
                .build();

        // /Reboot confirm
        CommandSpec confirm = CommandSpec.builder()
                .description(Text.of("Reboot the server immediately"))
                .permission(Permissions.COMMAND_NOW)
                .executor(new RebootConfirm(this))
                .build();
        // /Reboot now
        CommandSpec now = CommandSpec.builder()
                .description(Text.of("Reboot the server immediately"))
                .permission(Permissions.COMMAND_NOW)
                .executor(new RebootNow(this))
                .build();

        Map<String, String> choices = new HashMap<String, String>() {
            {
                put("h", "h");
                put("m", "m");
                put("s", "s");
            }
        };

        // /Reboot start h/m/s time reason
        CommandSpec start = CommandSpec.builder()
                .description(Text.of("Reboot base command"))
                .permission(Permissions.COMMAND_START)
                .arguments(GenericArguments.choices(Text.of("h/m/s"), choices),
                        GenericArguments.integer(Text.of("time")),
                        GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("reason"))))
                .executor(new RebootCMD(this))
                .build();

        CommandSpec rbootmain = CommandSpec.builder()
                .description(Text.of("Reboot base command"))
                .child(start, "start")
                .child(now, "now")
                .child(confirm, "confirm")
                .child(time, "time")
                .child(cancel, "cancel")
                .child(vote, "vote")
                .child(help, "help")
                .build();

        cmdManager.register(this, rbootmain, "reboot", "restart");
    }

    public Double getTPS() {
        return Sponge.getServer().getTicksPerSecond();
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

    public void scheduleRealTimeRestart() {
        cancelTasks();
        nextRealTimeRestart = getNextRealTimeFromConfig();
        double rInterval = nextRealTimeRestart;
        if (Config.timerBroadcast != null) {
            warningMessages(rInterval);
        }
        rebootTimer = new Timer();
        rebootTimer.schedule(new ShutdownTask(this), (long) (nextRealTimeRestart * 1000.0));

        logger.info("[MMCReboot] RebootCMD scheduled for " + (long)(nextRealTimeRestart) + " seconds from now!");
        tasksScheduled = true;
        startTimestamp = System.currentTimeMillis();
        isRestarting = true;
    }

    public void scheduleTasks() {
        boolean wasTPSRestarting = getTPSRestarting();
        cancelTasks();
        if (wasTPSRestarting) {
            setTPSRestarting(true);
        } else {
            setTPSRestarting(false);
        }
        double rInterval = Config.restartInterval * 3600;
        if (Config.timerBroadcast != null) {
            warningMessages(rInterval);
        }
        rebootTimer = new Timer();
        rebootTimer.schedule(new ShutdownTask(this), (long) (Config.restartInterval * 3600000.0));

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

                        NumberFormat formatter = new DecimalFormat("00");
                        String s = formatter.format(seconds);
                        if (minutes > 1) {
                            String message = Messages.getRestartNotificationMinutes().replace("%minutes%", "" + minutes).replace("%seconds%", "" + s);
                            broadcastMessage("&f[&6Restart&f] " + message);
                        } else if (minutes == 1) {
                            String message = Messages.getRestartNotificationMinute().replace("%minutes%", "" + minutes).replace("%seconds%", "" + s);
                            broadcastMessage("&f[&6Restart&f] " + message);
                        } else if (minutes < 1) {
                            String message = Messages.getRestartNotificationSeconds().replace("%minutes%", "" + minutes).replace("%seconds%", "" + s);
                            broadcastMessage("&f[&6Restart&f] " + message);
                        } else {
                            logger.info("[MMCReboot] " + "&bThe server will be restarting in &f" + hours + "h" + minutes + "m" + seconds + "s");
                        }
                        if (!playSoundNow && Config.playSoundFirstTime >= aTimerBroadcast) {
                            playSoundNow = true;
                        }
                        for (World w : Sponge.getServer().getWorlds()) {
                            if (Config.playSoundEnabled && playSoundNow) {
                                Optional<SoundType> sound = Sponge.getGame().getRegistry().getType(SoundType.class, Config.playSoundString);
                                SoundType playSound;
                                if (sound.isPresent()) {
                                    playSound = sound.get();
                                } else {
                                    playSound = Sponge.getGame().getRegistry().getType(SoundType.class, "block.note.pling").get();
                                }
                                w.playSound(playSound, soundLoc, 4000);
                            }
                        }
                        for (Player p : Sponge.getServer().getOnlinePlayers()) {
                            if (Config.titleEnabled) {
                                if (reason != null) {
                                    p.sendTitle(Title.builder()
                                            .title(fromLegacy(Config.titleMessage.replace("{hours}", "" + hours).replace("{minutes}", "" + minutes).replace("{seconds}", "" + s)))
                                            .subtitle(fromLegacy(reason))
                                            .fadeIn(10).fadeOut(10).stay(Config.titleStayTime * 20).build());
                                } else {
                                    p.sendTitle(Title.builder()
                                            .subtitle(fromLegacy(Config.titleMessage.replace("{hours}", "" + hours).replace("{minutes}", "" + minutes).replace("{seconds}", "" + s)))
                                            .fadeIn(10).fadeOut(10).stay(Config.titleStayTime * 20).build());
                                }
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
        if(rebootTimer != null) {
            rebootTimer.cancel();
        }
        rebootTimer = new Timer();
        tasksScheduled = false;
        isRestarting = false;
        TPSRestarting = false;
        nextRealTimeRestart = 0;
    }


    public boolean stopServer() {
        logger.info("[MMCReboot] Restarting...");
        isRestarting = false;
        broadcastMessage("&cServer is restarting, we'll be right back!");
        try {
            Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "save-all");
            if (Config.kickmessage.isEmpty()) {
                Sponge.getServer().shutdown();
            } else {
                Sponge.getServer().shutdown(fromLegacy(Config.kickmessage));
            }
        } catch (Exception e) {
            logger.info("[MMCReboot] Something went wrong while saving & stopping!");
            logger.info("Exception: " + e);
            return false;
        }
        return true;
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
            Sponge.getCommandManager().process(Sponge.getServer().getConsole(), cmd.replace("/", ""));
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
        Objective obj = Objective.builder().name("restart").criterion(Criteria.DUMMY).displayName(Text.of(Messages.getSidebarRestartTimerTitle())).build();

        board.addObjective(obj);
        board.updateDisplaySlot(obj, DisplaySlots.SIDEBAR);

        obj.getOrCreateScore(Text.builder(Integer.toString(minutes) +":" + s).color(TextColors.GREEN).build()).setScore(0);

        int mSec = (minutes * 60);
        double val = ((mSec + seconds) * 100 / 300);
        float percent = (float)val / 100.0f;

        Sponge.getServer().getOnlinePlayers().stream().filter(player -> minutes < 5 && hours == 0).forEach(player -> {
            player.setScoreboard(board);
            if (Config.bossbarEnabled) {
                if (bar == null) {
                    bar = ServerBossBar.builder()
                            .name(Text.of(Config.bossbarTitle.replace("{minutes}", Integer.toString(minutes)).replace("{seconds}", s)))
                            .color(BossBarColors.GREEN)
                            .overlay(BossBarOverlays.PROGRESS)
                            .percent(percent)
                            .build();
                } else {
                    bar.setPercent(percent);
                }
                bar.addPlayer(player);
            }
        });
    }

    public void displayVotes() {
        board = Scoreboard.builder().build();

        Objective obj = Objective.builder().name("vote").criterion(Criteria.DUMMY).displayName(Text.of(Messages.getSidebarTitle())).build();

        board.addObjective(obj);
        board.updateDisplaySlot(obj, DisplaySlots.SIDEBAR);

        obj.getOrCreateScore(Text.builder(Messages.getSidebarYes() + ":").color(TextColors.GREEN).build()).setScore(yesVotes);
        obj.getOrCreateScore(Text.builder(Messages.getSidebarNo() + ":").color(TextColors.AQUA).build()).setScore(noVotes);
        obj.getOrCreateScore(Text.builder(Messages.getSidebarTimeleft() + ":").color(TextColors.RED).build()).setScore(getTimeLeftInSeconds());


        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            player.setScoreboard(board);
        }
    }

    public  void removeScoreboard() {
        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            player.getScoreboard().clearSlot(DisplaySlots.SIDEBAR);
        }
    }

    public  void removeBossBar() {
        if (bar != null) {
            bar.removePlayers(bar.getPlayers());
        }
    }

    public void broadcastMessage(String message) {
        Sponge.getServer().getBroadcastChannel().send(fromLegacy(message), ChatTypes.SYSTEM);
    }

    public void sendMessage(CommandSource sender, String message) {
        sender.sendMessage(fromLegacy(message));
    }

    public Text fromLegacy(String legacy) {
        return TextSerializers.FORMATTING_CODE.deserializeUnchecked(legacy);
    }
}
