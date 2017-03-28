package net.moddedminecraft.mmcreboot;

import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;
import net.moddedminecraft.mmcreboot.Tasks.ShutdownTask;
import net.moddedminecraft.mmcreboot.commands.*;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
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
import java.util.ArrayList;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@Plugin(id = "mmcreboot", name = "MMCReboot", version = "1.7.1", authors = {"Leelawd93"})
public class Main {

    @Inject
    public Logger logger;

    @Inject
    private Metrics metrics;

    @Inject
    @ConfigDir(sharedRoot = false)
    private File configDir;

    @Inject
    @DefaultConfig(sharedRoot = false)
    public Path defaultConf;

    @Inject
    @DefaultConfig(sharedRoot = false)
    public File defaultConfFile;

    public int usingReason = 0;
    public int voteCancel = 0;
    public int cdTimer = 0;
    public boolean voteStarted = false;
    public int yesVotes = 0;
    public int noVotes = 0;
    public ArrayList<Player> hasVoted = new ArrayList<Player>();

    public int voteSeconds;
    public String reason;

    public long startTimestamp;
    public boolean justStarted = true;
    public boolean isRestarting = false;
    public boolean TPSRestarting = false;
    public int rebootConfirm = 0;

    // Timers
    private ArrayList<Timer> warningTimers = new ArrayList<Timer>();
    private Timer rebootTimer;
    private Timer justStartedTimer;

    private boolean playSoundNow = false;
    private Vector3d soundLoc = new Vector3d(0, 64, 0);

    private Config config;


    private CommandManager cmdManager = Sponge.getCommandManager();

    private Scoreboard board;

    @Listener
    public void Init(GameInitializationEvent event) throws IOException, ObjectMappingException {
        Sponge.getEventManager().registerListeners(this, new EventListener(this));
        this.config = new Config(this);
        loadCommands();
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) throws IOException {
        if(Config.restartEnabled) {
            scheduleTasks();
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


        Sponge.getScheduler().createTaskBuilder().execute(this::action).delay(250, TimeUnit.MILLISECONDS).interval(500,TimeUnit.MILLISECONDS).name("mmcreboot-s-sendAction").submit(this);

        Sponge.getScheduler().createTaskBuilder().execute(this::reduceVote).interval(1, TimeUnit.SECONDS).name("mmcreboot-s-reduceVoteCount").submit(this);

        Sponge.getScheduler().createTaskBuilder().execute(this::CheckTPSForRestart).delay(Config.tpsCheckDelay, TimeUnit.MINUTES).interval(30, TimeUnit.SECONDS).name("mmcreboot-s-checkTPSForRestart").submit(this);

        logger.info("MMCReboot Loaded");
    }

    public void onServerStop(GameStoppingEvent event) throws IOException {
        cancelTasks();
        logger.info("MMCReboot Disabled");
    }

    @Listener
    public void onPluginReload(GameReloadEvent event) throws IOException, ObjectMappingException {
        cancelTasks();
        removeScoreboard();
        isRestarting = false;

        this.config = new Config(this);

        if(Config.restartEnabled) {
            scheduleTasks();
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
                .permission("mmcreboot.reboot.cancel")
                .executor(new RebootCancel(this))
                .build();

        // /Reboot time
        CommandSpec time = CommandSpec.builder()
                .description(Text.of("Get the time remaining until the next restart"))
                .permission("mmcreboot.reboot.time")
                .executor(new RebootTime(this))
                .build();

        // /Reboot confirm
        CommandSpec confirm = CommandSpec.builder()
                .description(Text.of("Reboot the server immediately"))
                .permission("mmcreboot.reboot.now")
                .executor(new RebootConfirm(this))
                .build();
        // /Reboot now
        CommandSpec now = CommandSpec.builder()
                .description(Text.of("Reboot the server immediately"))
                .permission("mmcreboot.reboot.now")
                .executor(new RebootNow(this))
                .build();

        // /Reboot start h/m/s time reason
        CommandSpec start = CommandSpec.builder()
                .description(Text.of("Reboot base command"))
                .permission("mmcreboot.reboot.start")
                .arguments(GenericArguments.string(Text.of("h/m/s")),
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
        Double TPS = Sponge.getServer().getTicksPerSecond();
        return TPS;
    }

    public boolean getTPSRestarting() {
        return TPSRestarting;
    }

    public void setTPSRestarting(boolean bool) {
        TPSRestarting = bool;
    }

    public void CheckTPSForRestart() {
        if (getTPS() < Config.tpsMinimum && Config.tpsEnabled && !getTPSRestarting()) {
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
                            usingReason = 1;
                            reason = Config.tpsMessage;
                        } else {
                            usingReason = 0;
                        }
                        scheduleTasks();
                    }
                }
            }, 15000);
        }
    }

    public void action() {
        if (isRestarting) {
            displayRestart();
        }
        if (voteStarted && isRestarting && voteCancel == 0) {
            displayVotes();
        }
    }

    public void reduceVote() {
        if (voteStarted && isRestarting && voteCancel == 0) {
            if (voteSeconds > 0) {
                voteSeconds -= 1;
            }
            if (voteSeconds < 0) {
                voteSeconds = 0;
            }
        }
    }

    public void scheduleTasks() {
        boolean wasTPSRestarting = getTPSRestarting();
        cancelTasks();
        if (wasTPSRestarting) {
            setTPSRestarting(true);
        } else {
            setTPSRestarting(false);
        }
        if (Config.timerBroadcast != null) {
            Config.timerBroadcast.stream().filter(aTimerBroadcast -> Config.restartInterval * 60 - aTimerBroadcast > 0).forEach(aTimerBroadcast -> {
                Timer warnTimer = new Timer();
                warningTimers.add(warnTimer);
                warnTimer.schedule(new TimerTask() {
                    public void run() {
                        double timeLeft = (Config.restartInterval * 3600) - ((double) (System.currentTimeMillis() - startTimestamp) / 1000);
                        int hours = (int) (timeLeft / 3600);
                        int minutes = (int) ((timeLeft - hours * 3600) / 60);
                        int seconds = (int) timeLeft % 60;

                        NumberFormat formatter = new DecimalFormat("00");
                        String s = formatter.format(seconds);
                        if (minutes > 1) {
                            broadcastMessage("&f[&6Restart&f] &bThe server will be restarting in &f" + minutes + ":" + s + " &bminutes");
                        } else if (minutes == 1) {
                            broadcastMessage("&f[&6Restart&f] &bThe server will be restarting in &f" + minutes + " &bminute");
                        } else if (minutes < 1) {
                            broadcastMessage("&f[&6Restart&f] &bThe server will be restarting in &f" + s + " &bseconds");
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
                        for(Player p : Sponge.getServer().getOnlinePlayers()) {
                            if (Config.titleEnabled) {
                                p.sendTitle(Title.builder().subtitle(fromLegacy(Config.titleMessage.replace("{hours}",  ""+hours).replace("{minutes}",  ""+minutes).replace("{seconds}",  ""+s)))
                                        .fadeIn(20).fadeOut(20).stay(Config.titleStayTime * 20).build());
                            }
                        }
                        if (reason != null) {
                            broadcastMessage("&f[&6Restart&f] &d" + reason);
                        }
                        isRestarting = true;
                    }
                }, (long) ((Config.restartInterval * 60 - aTimerBroadcast) * 60000.0));
                logger.info("[MMCReboot] warning scheduled for " + (long) ((Config.restartInterval * 60 - aTimerBroadcast) * 60.0) + " seconds from now!");
            });
        }
        rebootTimer = new Timer();
        rebootTimer.schedule(new ShutdownTask(this), (long) (Config.restartInterval * 3600000.0));

        logger.info("[MMCReboot] RebootCMD scheduled for " + (long)(Config.restartInterval  * 3600.0) + " seconds from now!");
        Config.restartEnabled = true;
        startTimestamp = System.currentTimeMillis();
        isRestarting = true;
    }

    public void cancelTasks() {
        for (Timer warningTimer : warningTimers) warningTimer.cancel();
        warningTimers.clear();
        if(rebootTimer != null) {
            rebootTimer.cancel();
        }
        rebootTimer = new Timer();
        Config.restartEnabled = false;
        isRestarting = false;
        TPSRestarting = false;
        usingReason = 0;
        for (World w : Sponge.getServer().getWorlds()) {
            if (playSoundNow) {
                SoundType playSound = Sponge.getGame().getRegistry().getType(SoundType.class, "block.grass.step").get();
                w.playSound(playSound, soundLoc, 0);
                playSoundNow = false;
            }
        }
    }

    public boolean stopServer() {
        logger.info("[MMCReboot] Restarting...");
        isRestarting = false;
        broadcastMessage("&cServer is restarting, we'll be right back!");
        try {
            File file = new File(configDir + File.separator + "restart.txt");
            logger.info("[MMCReboot] Touching restart.txt at: " + file.getAbsolutePath());
            if (file.exists()) {
                file.setLastModified(System.currentTimeMillis());
            } else {
                file.createNewFile();
            }
        } catch (Exception e) {
            logger.info("[MMCReboot] Something went wrong while touching restart.txt!");
            return false;
        }
        try {
            Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "save-all");
            Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "stop");
        } catch (Exception e) {
            logger.info("[MMCReboot] Something went wrong while saving & stopping!");
            return false;
        }
        return true;
    }

    public boolean useCommandOnRestart() {
        logger.info("[MMCReboot] Running Command");
        isRestarting = false;
        Sponge.getCommandManager().process(Sponge.getServer().getConsole(), Config.restartCommand.replace("/", ""));
        return true;
    }

    public int getTimeLeftInSeconds() {
        return voteSeconds;
    }

    public void displayRestart()
    {
        double timeLeft = (Config.restartInterval * 3600) - ((double)(System.currentTimeMillis() - startTimestamp) / 1000);
        int hours = (int)(timeLeft / 3600);
        int minutes = (int)((timeLeft - hours * 3600) / 60);
        int seconds = (int)timeLeft % 60;

        NumberFormat formatter = new DecimalFormat("00");
        String s = formatter.format(seconds);

        board = Scoreboard.builder().build();
        Objective obj = Objective.builder().name("restart").criterion(Criteria.DUMMY).displayName(Text.of("Restart Timer")).build();

        board.addObjective(obj);
        board.updateDisplaySlot(obj, DisplaySlots.SIDEBAR);

        obj.getOrCreateScore(Text.builder(Integer.toString(minutes) +":" + s).color(TextColors.GREEN).build()).setScore(0);

        Sponge.getServer().getOnlinePlayers().stream().filter(player -> minutes < 5 && hours == 0).forEach(player -> {
            player.setScoreboard(board);
        });
    }

    public void displayVotes()
    {
        board = Scoreboard.builder().build();

        Objective obj = Objective.builder().name("vote").criterion(Criteria.DUMMY).displayName(Text.of("Restart Vote")).build();

        board.addObjective(obj);
        board.updateDisplaySlot(obj, DisplaySlots.SIDEBAR);

        obj.getOrCreateScore(Text.builder("Yes:").color(TextColors.GREEN).build()).setScore(yesVotes);
        obj.getOrCreateScore(Text.builder("No:").color(TextColors.AQUA).build()).setScore(noVotes);
        obj.getOrCreateScore(Text.builder("Time Left:").color(TextColors.RED).build()).setScore(getTimeLeftInSeconds());


        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            player.setScoreboard(board);
        }
    }

    public  void removeScoreboard()
    {
        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            player.getScoreboard().clearSlot(DisplaySlots.SIDEBAR);
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
