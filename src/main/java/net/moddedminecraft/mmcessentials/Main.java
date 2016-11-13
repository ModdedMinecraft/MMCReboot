package net.moddedminecraft.mmcessentials;

import com.google.inject.Inject;
import net.moddedminecraft.mmcessentials.commands.RebootCMD;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.scoreboard.Score;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.critieria.Criteria;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlots;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@Plugin(id = "mmcessentials", name = "MMCEssentials", version = "1.0")
public class Main {

    @Inject
    @DefaultConfig(sharedRoot = false)
    private Path defaultConf;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path path;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> loader;

    public Main main = Main.this;
    private Scheduler scheduler = Sponge.getScheduler();
    private Task.Builder taskBuilder = scheduler.createTaskBuilder();

    @Inject
    public Logger logger;

    public boolean restartEnabled = true;
    public double restartInterval = 6;
    private List<Double> timerBroadcast;
    private int timerRevote = 10;
    private int timerStartvote = 60;
    private int timerMinplayers = 10;
    private int timerVotepercent = 60;
    private boolean voteEnabled = true;

    private int usingReason = 0;
    private int voteCancel = 0;
    //private int rebootConfirm = 0;
    //private static int cdTimer = 0;
    private boolean voteStarted = false;
    private int yesVotes = 0;
    private int noVotes = 0;
    //private static ArrayList<Player> hasVoted = new ArrayList<Player>();

    //public static int playersOnline;
    private int voteSeconds;
    public String reason;

    public long startTimestamp;
    private boolean justStarted = true;
    public boolean isRestarting = false;

    // Timers
    private ArrayList<Timer> warningTimers = new ArrayList<Timer>();
    private Timer rebootTimer;
    private Timer justStartedTimer;


    private CommandManager cmdManager = Sponge.getCommandManager();

    private Game game;

    public int getUsingReason() {
        return usingReason;
    }

    @Listener
    public void Init(GameInitializationEvent event) throws IOException {
        setupConfig();
        loadConfig();
        loadCommands();
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) throws IOException {
        loader = HoconConfigurationLoader.builder().setPath(defaultConf).build();
        if(restartEnabled) {
            scheduleTasks();
        } else {
            logger.info("[MMCEssentials] No automatic restarts scheduled!");
        }

        if (voteEnabled) {
            justStartedTimer = new Timer();
            this.justStartedTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    justStarted = false;
                }
            }, (long)(timerStartvote * 60 * 1000));
        }

        taskBuilder.execute(new Runnable() {
            public void run() {
                action();
            }
        }).delay(1, TimeUnit.MILLISECONDS).interval(1,TimeUnit.MILLISECONDS).name("Send action").submit(this);

        taskBuilder.execute(new Runnable() {
            public void run() {
                reduceVote();
            }
        }).delay(1, TimeUnit.MILLISECONDS).interval(1,TimeUnit.MILLISECONDS).name("Reduce vote count").submit(this);

        logger.info("MMCEssentials Loaded");
    }

    public void onServerStop(GameStoppingEvent event) throws IOException {
        //Sboard.unregisterScoreboard(); //TODO SBOARD
        cancelTasks();
        logger.info("MMCEssentials Disabled");
    }

    private void loadCommands() {
        // /RebootCMD h/m/s time
        CommandSpec rboot = CommandSpec.builder()
                .description(Text.of("Reboot base command"))
                .permission("mmce.RebootCMD")
                .arguments(GenericArguments.onlyOne(GenericArguments.string(Text.of("h/m/s"))),
                        GenericArguments.integer(Text.of("time")),
                        GenericArguments.optional(GenericArguments.string(Text.of("reason"))))
                .executor(new RebootCMD(this))
                .build();

        cmdManager.register(this, rboot, "RebootCMD");
    }

    public void setupConfig() throws IOException {
        if (!Files.exists(defaultConf)) {
            loader.load().getNode("autorestart", "enabled").setValue(restartEnabled);
            loader.load().getNode("autorestart", "interval").setValue(restartInterval);
            loader.load().getNode("timer", "broadcast").setValue(timerBroadcast);
            loader.load().getNode("timer", "re-vote").setValue(timerRevote);
            loader.load().getNode("timer", "start-vote").setValue(timerStartvote);
            loader.load().getNode("timer", "vote-percent").setValue(timerVotepercent);
            loader.load().getNode("timer", "min-players").setValue(timerMinplayers);
            loader.load().getNode("voting", "enabled").setValue(voteEnabled);
            loader.save(loader.load());
        }
    }

    public void loadConfig() throws IOException {
        ConfigurationNode breakNode = loader.load().getNode("timer", "broadcast");
        List<Double> breakList;
        breakList = breakNode.getList(Util.doubleTransformer);

        restartEnabled = loader.load().getNode("autorestart", "enabled").getBoolean();
        restartInterval = loader.load().getNode("autorestart", "interval").getDouble();
        timerBroadcast = breakList;
        timerRevote = loader.load().getNode("timer", "re-vote").getInt();
        timerStartvote = loader.load().getNode("timer", "start-vote").getInt();
        timerMinplayers = loader.load().getNode("timer", "vote-percent").getInt();
        timerVotepercent = loader.load().getNode("timer", "min-players").getInt();
        voteEnabled = loader.load().getNode("voting", "enabled").getBoolean();

    }

    public void action() {
        if (isRestarting == true) {
            displayRestart();
        }
        if (voteStarted == true && isRestarting == false && voteCancel == 0) {
            displayVotes();
        }
    }

    public void reduceVote() {
        if (voteStarted == true && isRestarting == false && voteCancel == 0) {
            if (voteSeconds > 0) {
                voteSeconds -= 1;
            }
            if (voteSeconds < 0) {
                voteSeconds = 0;
            }
        }
    }

    public void scheduleTasks() {
        cancelTasks();
        if (timerBroadcast !=null) {
            for (Double aTimerBroadcast : timerBroadcast) {
                if (restartInterval * 60 - aTimerBroadcast > 0) {
                    Timer warnTimer = new Timer();
                    warningTimers.add(warnTimer);
                    warnTimer.schedule(new TimerTask() {
                        public void run() {
                            double timeLeft = (restartInterval * 3600) - ((double) (System.currentTimeMillis() - startTimestamp) / 1000);
                            int hours = (int) (timeLeft / 3600);
                            int minutes = (int) ((timeLeft - hours * 3600) / 60);
                            int seconds = (int) timeLeft % 60;

                            NumberFormat formatter = new DecimalFormat("00");
                            String s = formatter.format(seconds);

                            if (reason != null) {
                                if (minutes > 1) {
                                    Util.broadcastMessage("&f[&6Restart&f] &bThe server will be restarting in &f" + minutes + ":" + s + " &bminutes");
                                    Util.broadcastMessage("&f[&6Restart&f] &d" + reason);
                                    isRestarting = true;
                                } else if (minutes == 1) {
                                    Util.broadcastMessage("&f[&6Restart&f] &bThe server will be restarting in &f" + minutes + " &bminute");
                                    Util.broadcastMessage("&f[&6Restart&f] &d" + reason);
                                    isRestarting = true;
                                } else if (minutes < 1) {
                                    Util.broadcastMessage("&f[&6Restart&f] &bThe server will be restarting in &f" + s + " &bseconds");
                                    Util.broadcastMessage("&f[&6Restart&f] &d" + reason);
                                    isRestarting = true;
                                } else {
                                    logger.info("[MMCEssentials] " + "&bThe server will be restarting in &f" + hours + "h" + minutes + "m" + seconds + "s");
                                    isRestarting = true;
                                }
                            } else {
                                if (minutes > 1) {
                                    Util.broadcastMessage("&f[&6Restart&f] &bThe server will be restarting in &f" + minutes + ":" + s + " &bminutes");
                                    isRestarting = true;
                                } else if (minutes == 1) {
                                    Util.broadcastMessage("&f[&6Restart&f] &bThe server will be restarting in &f" + minutes + " &bminute");
                                    isRestarting = true;
                                } else if (minutes < 1) {
                                    Util.broadcastMessage("&f[&6Restart&f] &bThe server will be restarting in &f" + s + " &bseconds");
                                    isRestarting = true;
                                } else {
                                    logger.info("[MMCEssentials] " + "&bThe server will be restarting in &f" + hours + "h" + minutes + "m" + seconds + "s");
                                    isRestarting = true;
                                }
                            }
                        }
                    }, (long) ((restartInterval * 60 - aTimerBroadcast) * 60000.0));
                    logger.info("[MMCEssentials] warning scheduled for " + (long) ((restartInterval * 60 - aTimerBroadcast) * 60.0) + " seconds from now!");
                }
            }
        }
        rebootTimer = new Timer();
        rebootTimer.schedule(new ShutdownTask(this), (long)(restartInterval * 3600000.0));

        logger.info("[MMCEssentials] RebootCMD scheduled for " + (long)(restartInterval  * 3600.0) + " seconds from now!");
        restartEnabled = true;
        startTimestamp = System.currentTimeMillis();
        isRestarting = true;
    }

    public void cancelTasks() {
        for (Timer warningTimer : warningTimers) warningTimer.cancel();
        warningTimers.clear();
        if(rebootTimer != null) rebootTimer.cancel();
        rebootTimer = new Timer();
        restartEnabled = false;
        isRestarting = false;
        usingReason = 0;
    }

    public boolean stopServer() {
        logger.info("[MMCEssentials] Restarting...");
        isRestarting = false;
        Util.broadcastMessage("&cServer is restarting, we'll be right back!");
        try {
            File file = new File(configDir + File.separator + "restart.txt");
            logger.info("[MMCEssentials] Touching restart.txt at: " + file.getAbsolutePath());
            if (file.exists()) {
                file.setLastModified(System.currentTimeMillis());
            } else {
                file.createNewFile();
            }
        } catch (Exception e) {
            logger.info("[MMCEssentials] Something went wrong while touching restart.txt!");
            return false;
        }
        try {
            game.getCommandManager().process(game.getServer().getConsole(), "save-all");
            game.getCommandManager().process(game.getServer().getConsole(), "stop");
        } catch (Exception e) {
            logger.info("[MMCEssentials] Something went wrong while saving & stoping!");
            return false;
        }
        return true;
    }

    public int getTimeLeftInSeconds() {
        return voteSeconds;
    }

    public void displayRestart()
    {
        double timeLeft = (restartInterval * 3600) - ((double)(System.currentTimeMillis() - startTimestamp) / 1000);
        int hours = (int)(timeLeft / 3600);
        int minutes = (int)((timeLeft - hours * 3600) / 60);
        int seconds = (int)timeLeft % 60;

        NumberFormat formatter = new DecimalFormat("00");
        String s = formatter.format(seconds);

        Scoreboard board = Scoreboard.builder().build();
        Objective obj;

        if (board.getObjective("restart") == null) {
            obj = Objective.builder().name("restart").criterion(Criteria.DUMMY).displayName(Text.of("Restart Timer")).build();
        } else {
            obj = Objective.builder().name("restart").criterion(Criteria.DUMMY).displayName(Text.of("Restart Timer")).build();
        }

        Score score = obj.getOrCreateScore(Text.of(TextColors.GREEN + "" + Integer.toString(minutes) +":" + s));
        score.setScore(0);

        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            if (minutes < 5 && hours == 0) {
                player.setScoreboard(board);
            }
        }
    }

    public  void displayVotes()
    {
        Scoreboard board = Scoreboard.builder().build();

        Objective obj;

        if (board.getObjective("vote") == null) {
            obj = Objective.builder().name("vote").criterion(Criteria.DUMMY).displayName(Text.of("Restart Vote")).build();
        } else {
            obj = Objective.builder().name("vote").displayName(Text.of("Restart Vote")).build();
        }
        Score yes = obj.getOrCreateScore(Text.of(TextColors.GREEN + "Yes:"));
        Score no = obj.getOrCreateScore(Text.of(TextColors.AQUA + "No:"));
        Score time = obj.getOrCreateScore(Text.of(TextColors.RED + "Time Left:"));
        yes.setScore(yesVotes);
        no.setScore(noVotes);
        time.setScore(getTimeLeftInSeconds());

        board.addObjective(obj);
        board.updateDisplaySlot(obj, DisplaySlots.SIDEBAR);

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
}
