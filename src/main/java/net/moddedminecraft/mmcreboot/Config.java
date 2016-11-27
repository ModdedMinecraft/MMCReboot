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

    public void configCheck() throws IOException, ObjectMappingException {

        if (!plugin.defaultConfFile.exists()) {
            plugin.defaultConfFile.createNewFile();
        }

        restartEnabled = check(config.getNode("autorestart", "enabled"), true, "").getBoolean();
        restartInterval = check(config.getNode("autorestart", "interval"), 6, "").getInt();
        timerBroadcast = checkList(config.getNode("timer", "broadcast"), timerBroadcastList, "").getList(TypeToken.of(Double.class));
        timerRevote =  check(config.getNode("timer", "re-vote"), 10, "").getInt();
        timerStartvote = check(config.getNode("timer", "start-vote"), 60, "").getInt();
        timerVotepercent = check(config.getNode("timer", "vote-percent"), 10, "").getInt();
        timerMinplayers = check(config.getNode("timer", "min-players"), 5, "").getInt();
        voteEnabled = check(config.getNode("voting", "enabled"), true, "").getBoolean();

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
