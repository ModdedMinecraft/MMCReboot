package net.moddedminecraft.mmcreboot;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Config {

    private final Main plugin;

    private static ConfigurationLoader<CommentedConfigurationNode> loader;
    public static CommentedConfigurationNode config;

    public Config(Main main) throws IOException, ObjectMappingException {
        plugin = main;
        loader = HoconConfigurationLoader.builder().setPath(plugin.defaultConf).build();
        config = loader.load();
        configCheck();
    }

    private Double[] timerBroadcastList = {
            10.0,
            5.0,
            4.0,
            3.0,
            2.0,
            1.0,
            0.5
    };

    public static boolean restartEnabled;
    public static double restartInterval;
    public static List<Double> timerBroadcast;
    public static int timerRevote;
    public static int timerStartvote;
    public static int timerMinplayers;
    public static int timerVotepercent;
    public static boolean voteEnabled;
    public static int timerVotepassed;

    public void configCheck() throws IOException, ObjectMappingException {

        if (!plugin.defaultConfFile.exists()) {
            plugin.defaultConfFile.createNewFile();
        }

        restartEnabled = check(config.getNode("autorestart", "enabled"), true, "Enable / Disable automatic restarts after the designated interval time.").getBoolean();
        restartInterval = check(config.getNode("autorestart", "interval"), 6, "How long in hours should the auto restart timer be set for?").getInt();
        timerBroadcast = checkList(config.getNode("timer", "broadcast"), timerBroadcastList, "warning times before reboot in minutes (0.5 = 30 seconds)").getList(TypeToken.of(Double.class));
        timerRevote =  check(config.getNode("timer", "re-vote"), 10, "Time before another vote to restart can begin. (In minutes)  ").getInt();
        timerStartvote = check(config.getNode("timer", "start-vote"), 60, "How long should it be before players are allowed to start a vote after the server has restarted (In minutes) ").getInt();
        timerVotepercent = check(config.getNode("timer", "vote-percent"), 60, "% of online players to vote yes before a restart is triggered.").getInt();
        timerVotepassed = check(config.getNode("timer", "vote-passed"), 300, "Time until the restart after a vote has passed in seconds (default 300 = 5 minutes)").getInt();
        timerMinplayers = check(config.getNode("timer", "min-players"), 5, "The required amount of players online to start a vote ").getInt();
        voteEnabled = check(config.getNode("voting", "enabled"), true, "Enable or Disable the ability for players to vote for a server restart").getBoolean();

        loader.save(config);
    }

    private CommentedConfigurationNode check(CommentedConfigurationNode node, Object defaultValue, String comment) {
        if (node.isVirtual()) {
            node.setValue(defaultValue).setComment(comment);
        }
        return node;
    }
    private CommentedConfigurationNode checkList(CommentedConfigurationNode node, Double[] defaultValue, String comment) {
        if (node.isVirtual()) {
            node.setValue(Arrays.asList(defaultValue)).setComment(comment);
        }
        return node;
    }

}
