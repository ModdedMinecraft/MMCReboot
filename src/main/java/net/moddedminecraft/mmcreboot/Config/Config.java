package net.moddedminecraft.mmcreboot.Config;

import com.google.common.reflect.TypeToken;
import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Config {

    private final Main plugin;

    private static ConfigurationLoader<CommentedConfigurationNode> loader;
    public static CommentedConfigurationNode config;

    public Config(Main main) throws IOException {
        plugin = main;
        loader = HoconConfigurationLoader.builder().path(plugin.defaultConf).build();
        config = loader.load();
        configCheck();
    }

    private Integer[] timerBroadcastList = {
            600,
            300,
            240,
            180,
            120,
            60,
            30
    };

    private String[] RealTimeList = {
            "00:00",
            "06:00",
            "12:00",
            "18:00"
    };

    private String[] restartCmdList = {
            "/changeme",
            "/me too"
    };

    public static String restartType;
    public static double restartInterval;

    //public static boolean realTimeEnabled;
    public static List<String> realTimeInterval;

    public static boolean bossbarEnabled;
    public static String bossbarTitle;

    public static boolean timerUseVoteScoreboard;
    public static boolean timerUseScoreboard;
    public static boolean timerUseChat;
    public static List<Integer> timerBroadcast;
    public static int timerRevote;
    public static int timerStartvote;
    public static int timerMinplayers;
    public static int timerVotepercent;
    public static boolean voteEnabled;
    public static int timerVotepassed;

    public static boolean restartUseCommand;
    public static List<String> restartCommands;
    public static String defaultRestartReason;

    public static boolean tpsEnabled;
    public static int tpsMinimum;
    public static int tpsTimer;
    public static boolean tpsUseReason;
    public static String tpsMessage;
    public static boolean tpsRestartCancel;
    public static String tpsRestartCancelMsg;
    public static int tpsCheckDelay;

    public static boolean playSoundEnabled;
    public static boolean titleEnabled;
    public static int titleStayTime;
    public static String titleMessage;

    public static String language;

    public static String kickmessage;

    //public static boolean playSoundOnFirstWarningOnly;
    public static String playSoundString = "block.note.pling";
    public static double playSoundFirstTime;

    public void configCheck() throws IOException {

        if (!plugin.configDir.toFile().exists()) {
            plugin.configDir.toFile().createNewFile();
        }

        restartType = check(config.node("autorestart", "enabled"), "Fixed", "Values: Fixed, Realtime or None. The value choses here represents what timer will be used.").getString();
        restartInterval = check(config.node("autorestart", "fixed" ,"interval"), 6, "How long in hours should the auto restart timer be set for?").getInt();
        realTimeInterval = checkList(config.node("autorestart", "realtime" ,"intervals"), RealTimeList, "Set times for server restarts (24h time eg: 18:30)").getList(String.class);

        timerUseVoteScoreboard = check(config.node("timer", "scoreboard", "vote", "use"), true, "Whether or not the scoreboard should be shown during a vote").getBoolean();
        timerUseScoreboard = check(config.node("timer", "scoreboard", "countdown", "use"), true, "Whether or not the scoreboard should be shown during the last 5 minute countdown to a restart").getBoolean();
        timerBroadcast = checkList(config.node("timer", "broadcast"), timerBroadcastList, "warning times before reboot in seconds").getList(Integer.class);
        timerUseChat = check(config.node("timer", "chat", "use"), true, "Whether or not the warning should be broadcast in the chat.").getBoolean();
        timerRevote =  check(config.node("timer", "re-vote"), 10, "Time before another vote to restart can begin. (In minutes)  ").getInt();
        timerStartvote = check(config.node("timer", "start-vote"), 60, "How long should it be before players are allowed to start a vote after the server has restarted (In minutes) ").getInt();
        timerVotepercent = check(config.node("timer", "vote-percent"), 60, "% of online players to vote yes before a restart is triggered.").getInt();
        timerVotepassed = check(config.node("timer", "vote-passed"), 300, "Time until the restart after a vote has passed in seconds (default 300 = 5 minutes)").getInt();
        timerMinplayers = check(config.node("timer", "min-players"), 5, "The required amount of players online to start a vote ").getInt();

        playSoundEnabled = check(config.node("timer", "notifications", "playsound"), true, "Should a sound be played when a restart broadcast is sent?").getBoolean();
        //playSoundOnFirstWarningOnly = check(config.node("timer", "notifications", "sound", "play-sound-on-first-warning-only"), true, "Only play the notification sound at for the first restart warning.").getBoolean();
        playSoundString = check(config.node("timer", "notifications", "sound", "sound-to-play"), "block.note.pling", "The sound that should play for the notification. (Vanilla sounds can be found here: http://minecraft.gamepedia.com/Sounds.json)").getString();
        playSoundFirstTime = check(config.node("timer", "notifications", "sound", "when-to-start"), 600, "When should the sound notification start? (This should be the same as one of your broadcast timers)").getDouble();

        titleEnabled = check(config.node("timer", "notifications", "title", "enabled"), true, "Should a title message pop up in the middle of the screen").getBoolean();
        titleStayTime = check(config.node("timer", "notifications", "title", "staytime"), 2, "How long should the title message show up for before disappearing? (in seconds)").getInt();
        titleMessage = check(config.node("timer", "notifications", "title", "message"), "The server will be restarting in {minutes}:{seconds}", "The title message to be displayed ({hours},{minutes},{seconds} will be replaced").getString();

        bossbarEnabled = check(config.node("bossbar", "enabled"), false, "If true, A bossbar will display with a countdown until restart.").getBoolean();
        bossbarTitle = check(config.node("bossbar", "title"), "Restart", "Title displayed above the boss bar, Can use {minutes} and {seconds} to display time").getString();

        voteEnabled = check(config.node("voting", "enabled"), true, "Enable or Disable the ability for players to vote for a server restart").getBoolean();

        restartUseCommand = check(config.node("restart", "use-command"), false, "If enabled, This will run the configured command instead of restarting the server.").getBoolean();
        restartCommands = checkList(config.node("restart", "command"), restartCmdList, "The command(s) to run if 'use-command' has been enabled").getList(String.class);
        defaultRestartReason = check(config.node("restart", "reason"), "", "The default reason shown for a restart (automated and manual), Leave blank for no reason.").getString();

        tpsEnabled = check(config.node("tps", "use"), false, "If enabled, the server will initiate a restart timer if the TPS is below the minimum set.").getBoolean();
        tpsMinimum = check(config.node("tps", "minimum"), 10, "The minimum TPS to initiate a restart timer").getInt();
        tpsTimer = check(config.node("tps", "timer"), 300, "Time until the restart after a TPS check has failed, in seconds (default 300 = 5 minutes)").getInt();
        tpsUseReason = check(config.node("tps", "use-reason"), true, "If enabled, there will be a reason broadcast alongside the countdown for the restart.").getBoolean();
        tpsMessage = check(config.node("tps", "reason-message"), "Server TPS is below the minimum.", "The reason to broadcast if 'use-reason' is enabled").getString();
        tpsRestartCancel = check(config.node("tps", "restart-cancel"), false, "If set to true, When the restart timer reaches 0, The TPS will be checked again \n"
                                                                                + "If the TPS is above the minimum, the restart is canceled").getBoolean();
        tpsRestartCancelMsg = check(config.node("tps", "restart-cancel-message"), "&bThe server will not restart. The TPS is now above the minimum", "The broadcast message sent to everyone if the restart was canceled").getString();
        tpsCheckDelay = check(config.node("tps", "check-delay"), 15, "How long after the server starts until the TPS check initiates. (In minutes)").getInt();

        language = check(config.node("language"), "EN", "Localization to be used, All available translations are in the 'localization' folder").getString();

        kickmessage = check(config.node("kick-message"), "The server is restarting.", "The message that is sent to all players as the server shuts down.").getString();

        loader.save(config);
    }

    private CommentedConfigurationNode check(CommentedConfigurationNode node, Object defaultValue, String comment) throws SerializationException {
        if (node.virtual()) {
            node.set(defaultValue).comment(comment);
        }
        return node;
    }
    private CommentedConfigurationNode checkList(CommentedConfigurationNode node, Integer[] defaultValue, String comment) throws SerializationException {
        if (node.virtual()) {
            node.set(Arrays.asList(defaultValue)).comment(comment);
        }
        return node;
    }
    private CommentedConfigurationNode checkList(CommentedConfigurationNode node, String[] defaultValue, String comment) throws SerializationException {
        if (node.virtual()) {
            node.set(Arrays.asList(defaultValue)).comment(comment);
        }
        return node;
    }

}
